package org.ntb.imageresizer.actor

import java.io._
import java.net.URI
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.{ ByteString, Timeout }
import org.ntb.imageresizer.imageformat.{ ImageFormat, JPEG }
import org.ntb.imageresizer.storage._

import scala.collection.mutable
import scala.concurrent.{ Await, Future }
import scala.util.{ Failure, Success }

class ImageBrokerActor(downloadActor: ActorRef, resizeActor: ActorRef)
    extends Actor
    with ActorLogging
    with IndexStore
    with KeyEncoder
    with TempDirectoryIndexFile
    with TempDirectoryStorageFile {
  import DownloadActor._
  import ImageBrokerActor._
  import ImageDataActor._
  import ResizeActor._

  val index = mutable.Map.empty[ImageKey, FilePosition]
  val tasks = mutable.Map.empty[ImageKey, Future[(ByteString, Long, Long)]]
  val imageDataActor = context.actorOf(Props(classOf[ImageDataActor], storageFile))
  implicit val executionContext = context.dispatcher
  implicit val akkaTimeout = Timeout(30, TimeUnit.SECONDS)

  override def receive = {
    case GetImageRequest(source, size, format) ⇒
      val imageKey = ImageKey(encodeKey(source, size, format), size, format)
      log.debug("Looking up image {} from index", imageKey)
      index.get(imageKey) match {
        case Some(pos) ⇒
          log.debug("Serving cached image {}", imageKey)
          val dataResponse = ask(imageDataActor, LoadImageRequest(pos.offset)).mapTo[LoadImageResponse]
          pipe(dataResponse map { r ⇒ GetImageResponse(r.data) }) to sender()
        case None ⇒
          val senderRef = sender()
          tasks.get(imageKey) match {
            case Some(task) ⇒
              log.debug("Task for image {} already exists, using existing task", imageKey)
              task onComplete {
                case Success((data, offset, storageSize)) ⇒
                  senderRef ! GetImageResponse(data)
                case Failure(t) ⇒
                  senderRef ! Status.Failure(t)
              }
            case None ⇒
              log.debug("Loading image {} from source {}", imageKey, source)
              val storage = storageFile
              val task = for (
                downloaded ← ask(downloadActor, DownloadRequest(new URI(source))).mapTo[DownloadResponse];
                resized ← ask(resizeActor, ResizeImageRequest(downloaded.data, size, format)).mapTo[ResizeImageResponse];
                stored ← ask(imageDataActor, StoreImageRequest(imageKey, resized.data)).mapTo[StoreImageResponse]
              ) yield (resized.data, stored.offset, stored.size)
              tasks.put(imageKey, task)
              task onComplete {
                case Success((data, offset, storageSize)) ⇒
                  log.debug("Stored image {} to {}, position {} ({} bytes)", imageKey, storage, offset, storageSize)
                  self ! TaskComplete(imageKey, FilePosition(storage, offset))
                  senderRef ! GetImageResponse(data)
                case Failure(t) ⇒
                  self ! TaskFailed(imageKey, t)
                  senderRef ! Status.Failure(t)
              }
          }
      }
    case TaskComplete(key, position) ⇒
      tasks.remove(key)
      index.put(key, position)
    case TaskFailed(key, t) ⇒
      tasks.remove(key)
      log.error(t, "Resize task failed for image {}", key)
    case GetLocalImageRequest(source, id, size, format) ⇒
      sender() ! Status.Failure(new NotImplementedError("Method not implemented"))
  }

  override def preStart() {
    val file = indexFile
    if (file.exists() && file.length() > 0) {
      loadIndex(index, file)
      log.info("Loaded {} items to index from {}", index.size, file)
      if (log.isDebugEnabled) {
        log.debug("Images in index:\n{}", index.keys.mkString("\n"))
      }
    }
    log.info("Started {} using storage file {} and index file {}", storageId, storageFile, indexFile)
  }

  override def postStop() {
    log.info("{} shutting down", self.path.name)
    if (tasks.nonEmpty) {
      log.info("Awaiting for {} remaining tasks...", tasks.size)
      flushTasks()
      tasks.clear()
    }
    if (index.nonEmpty) {
      log.info("Saving index ({} items) to {}", index.size, indexFile)
      saveIndex(index, indexFile)
      index.clear()
    }
  }

  def storageId: String =
    self.path.name

  def flushTasks() {
    val remainingTasks = Future.sequence(tasks.values)
    try {
      Await.result(remainingTasks, akkaTimeout.duration)
    } catch {
      case e: Exception ⇒
        log.error("Error while shutting down", e)
    }
  }
}

object ImageBrokerActor {
  case class GetImageRequest(source: String, size: Int, format: ImageFormat = JPEG) {
    require(size > 0, "Size must be positive")
  }

  case class GetLocalImageRequest(source: File, id: String, size: Int, format: ImageFormat = JPEG) {
    require(source.exists && source.canRead, "Source file must exist and be readable")
    require(!id.isEmpty, "Image ID must not be empty")
    require(size > 0, "Size must be positive")
  }

  case class GetImageResponse(data: ByteString)

  private[actor] case class TaskComplete(key: ImageKey, position: FilePosition)

  private[actor] case class TaskFailed(key: ImageKey, t: Throwable)
}
