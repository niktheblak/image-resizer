package org.ntb.imageresizer.actor

import java.io._
import java.net.URI
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.{ ByteString, Timeout }
import com.google.common.hash.Hashing
import com.google.common.io.BaseEncoding
import org.ntb.imageresizer.imageformat.{ ImageFormat, JPEG }
import org.ntb.imageresizer.storage._

import scala.collection.mutable
import scala.concurrent.{ Await, Future }
import scala.util.{ Failure, Success }

class ImageBrokerActor(downloadActor: ActorRef, resizeActor: ActorRef)
    extends Actor
    with ActorLogging
    with IndexStore
    with TempDirectoryIndexFile
    with TempDirectoryStorageFile {
  import DownloadActor._
  import ImageBrokerActor._
  import ImageDataActor._
  import ResizeActor._

  val index = mutable.Map.empty[ImageKey, FilePosition]
  val tasks = mutable.Map.empty[ImageKey, Future[(ByteString, Long, Long)]]
  val hashFunction = Hashing.goodFastHash(128)
  val encoding = BaseEncoding.base64Url().omitPadding()
  val imageDataLoader = context.actorOf(Props[ImageDataActor])
  implicit val executionContext = context.dispatcher
  implicit val akkaTimeout = Timeout(30, TimeUnit.SECONDS)

  override def receive = {
    case GetImageRequest(source, size, format) ⇒
      val imageKey = ImageKey(getKey(source.toString, size, format), size, format)
      index.get(imageKey) match {
        case Some(pos) ⇒
          log.info("Serving cached image {}", imageKey)
          val dataResponse = ask(imageDataLoader, LoadImageRequest(pos.storage, pos.offset)).mapTo[LoadImageResponse]
          pipe(dataResponse map { r ⇒ GetImageResponse(r.data) }) to sender()
        case None ⇒
          val senderRef = sender()
          tasks.get(imageKey) match {
            case Some(task) ⇒
              log.info("Task for image {} already exists, using existing task", imageKey)
              task onComplete {
                case Success((data, offset, storageSize)) ⇒
                  senderRef ! GetImageResponse(data)
                case Failure(t) ⇒
                  senderRef ! Status.Failure(t)
              }
            case None ⇒
              log.info("Loading image {} from source {}", imageKey, source)
              val storage = storageFile
              val task = for (
                downloaded ← ask(downloadActor, DownloadRequest(source)).mapTo[DownloadResponse];
                resized ← ask(resizeActor, ResizeImageRequest(downloaded.data, size, format)).mapTo[ResizeImageResponse];
                stored ← ask(imageDataLoader, StoreImageRequest(storage, imageKey, resized.data)).mapTo[StoreImageResponse]
              ) yield (resized.data, stored.offset, stored.size)
              tasks.put(imageKey, task)
              task onComplete {
                case Success((data, offset, storageSize)) ⇒
                  log.info("Stored image {} to {}, position {} ({} bytes)", imageKey, storage, offset, storageSize)
                  self ! TaskComplete(imageKey, FilePosition(storage, offset))
                  senderRef ! GetImageResponse(data)
                case Failure(t) ⇒
                  senderRef ! Status.Failure(t)
              }
          }
      }
    case TaskComplete(key, position) ⇒
      tasks.remove(key)
      index.put(key, position)
    case GetLocalImageRequest(source, id, size, format) ⇒
      sender() ! Status.Failure(new NotImplementedError("Method not implemented"))
  }

  override def preStart() {
    loadIndex(index, indexFile)
  }

  override def postStop() {
    if (tasks.nonEmpty) {
      flushTasks()
    }
    saveIndex(index, indexFile)
    index.clear()
  }

  def storageId: String = getClass.getSimpleName

  private def flushTasks() {
    val remainingTasks = Future.sequence(tasks.values)
    try {
      Await.result(remainingTasks, akkaTimeout.duration)
    } catch {
      case e: Exception ⇒
        log.error("Error while shutting down", e)
    }
  }

  private def getKey(source: String, size: Int, format: ImageFormat): String = {
    val hasher = hashFunction.newHasher()
    hasher.putUnencodedChars(source)
    hasher.putInt(size)
    hasher.putByte(formatToByte(format))
    val hash = hasher.hash()
    encoding.encode(hash.asBytes())
  }
}

object ImageBrokerActor extends FlatFileImageStore {
  case class GetImageRequest(source: URI, size: Int, format: ImageFormat = JPEG) {
    require(size > 0, "Size must be positive")
  }

  case class GetLocalImageRequest(source: File, id: String, size: Int, format: ImageFormat = JPEG) {
    require(source.exists && source.canRead, "Source file must exist and be readable")
    require(!id.isEmpty, "Image ID must not be empty")
    require(size > 0, "Size must be positive")
  }

  case class GetImageResponse(data: ByteString)

  private[actor] case class TaskComplete(key: ImageKey, position: FilePosition)
}
