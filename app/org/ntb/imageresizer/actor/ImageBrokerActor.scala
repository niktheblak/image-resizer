package org.ntb.imageresizer.actor

import java.io._
import javax.inject.{ Inject, Named }

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.{ ByteString, Timeout }
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.storage._
import org.ntb.imageresizer.util.FileUtils
import play.api.Logger
import play.api.http.HeaderNames
import play.api.libs.ws._

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.util.{ Failure, Success }

class DownloadException(msg: String) extends RuntimeException(msg)

class ImageBrokerActor @Inject() (ws: WSClient, @Named("resizer") resizeActor: ActorRef)
    extends Actor
    with ActorUtils
    with IndexStore
    with KeyEncoder
    with CurrentDirectoryIndexFile
    with CurrentDirectoryStorageFile {
  import ImageBrokerActor._
  import ImageDataActor._
  import ResizeActor._

  private val index = mutable.Map.empty[ImageKey, FilePosition]
  private val tasks = mutable.Map.empty[ImageKey, Future[LoadImageTask]]
  private val imageDataActors = mutable.Map.empty[File, ActorRef]

  private implicit val executionContext = context.dispatcher
  private implicit val akkaTimeout = Timeout(10.seconds)
  private val downloadTimeout = 10.seconds

  override def receive = {
    case GetImageRequest(source, size, format) ⇒
      assert(size > 0, s"Invalid size: $size")
      val imageKey = ImageKey(encodeKey(source, size, format), size, format)
      val storage = storageFor(imageKey)
      val imageDataActor = imageDataActorFor(storage)
      val recipient = sender()
      handleGetImageRequest(imageDataActor, size, format, imageKey) {
        // No cache task was found for this image, start a new task
        loadAndCacheRemoteImage(imageDataActor, imageKey, source, size, format) { task ⇒
          registerNotifications(task, recipient, storage, imageKey)
        }
      }
    case TaskComplete(key, position) ⇒
      tasks.remove(key)
      index.put(key, position)
    case TaskFailed(key, t) ⇒
      tasks.remove(key)
      Logger.error(s"Resize task failed for image $key", t)
    case GetLocalImageRequest(source, id, size, format) ⇒
      assert(size > 0, s"Invalid size: $size")
      val imageKey = ImageKey(encodeKey(id, size, format), size, format)
      val storage = storageFor(imageKey)
      val imageDataActor = imageDataActorFor(storage)
      val recipient = sender()
      handleGetImageRequest(imageDataActor, size, format, imageKey) {
        // No cache task was found for this image, start a new task
        cacheLocalImage(imageDataActor, imageKey, source, size, format) { task ⇒
          registerNotifications(task, recipient, storage, imageKey)
        }
      }
  }

  def handleGetImageRequest(imageDataActor: ActorRef, size: Int, format: ImageFormat, imageKey: ImageKey)(loadImage: ⇒ Unit) {
    Logger.debug(s"Looking up image $imageKey from index")
    index.get(imageKey) match {
      case Some(pos) ⇒
        Logger.debug(s"Serving cached image $imageKey")
        val dataResponse = ask(imageDataActor, LoadImageRequest(pos.offset, pos.size)).mapTo[LoadImageResponse]
        pipe(dataResponse map { r ⇒ GetImageResponse(r.data) }) to sender()
      case None ⇒
        // Image was not found from index, check ongoing cache tasks
        val senderRef = sender()
        tasks.get(imageKey) match {
          case Some(task) ⇒
            Logger.debug(s"Task for image $imageKey already exists, using existing task")
            task onComplete {
              case Success(LoadImageTask(data, offset, storageSize)) ⇒
                senderRef ! GetImageResponse(data)
              case Failure(t) ⇒
                senderRef ! Status.Failure(t)
            }
          case None ⇒
            Logger.debug(s"Image $imageKey not found, caching image from remote source")
            loadImage
        }
    }
  }

  override def preStart() {
    val file = indexFile
    if (file.exists() && file.length() > 0) {
      loadIndex(index, file)
      Logger.info(s"Loaded ${index.size} items to index from $file")
      if (Logger.isDebugEnabled) {
        Logger.debug(s"Images in index:\n${index.keys.mkString("\n")}")
      }
    }
    Logger.info(s"Started $storageId using storage file $storageFile and index file $indexFile")
  }

  override def postStop() {
    Logger.info(s"${self.path.name} shutting down")
    if (tasks.nonEmpty) {
      Logger.info(s"Awaiting for ${tasks.size} remaining tasks...")
      flushTasks()
      tasks.clear()
    }
    imageDataActors.values.foreach(context.stop)
    imageDataActors.clear()
    if (index.nonEmpty) {
      Logger.info(s"Saving index (${index.size} items) to $indexFile")
      saveIndex(index, indexFile)
      index.clear()
    }
  }

  def loadAndCacheRemoteImage(imageDataActor: ActorRef, imageKey: ImageKey, source: String, size: Int, format: ImageFormat)(listen: Future[LoadImageTask] ⇒ Unit) {
    Logger.debug(s"Loading image $imageKey from URL $source")
    val loadImageTask = ws.url(source)
      .withRequestTimeout(downloadTimeout)
      .withFollowRedirects(true)
      .get() map { response ⇒
        validateResponse(source, response)
        validateFormat(source, response)
        response.bodyAsBytes
      }
    cacheImage(loadImageTask, listen, imageDataActor, imageKey, size, format)
  }

  def validateResponse(source: String, response: WSResponse) {
    if (response.status >= 400) {
      throw new DownloadException(s"Server for $source responded with HTTP ${response.status} ${response.statusText}: ${response.body}")
    }
  }

  def validateFormat(source: String, response: WSResponse) {
    val contentTypeHeader = response.header(HeaderNames.CONTENT_TYPE)
    val format = for {
      ct ← contentTypeHeader
      f ← parseImageFormatFromMimeType(ct)
    } yield f
    if (format.isEmpty) {
      Logger.warn(s"Server response for $source has unknown image format ${contentTypeHeader.getOrElse("")}")
    }
  }

  def cacheLocalImage(imageDataActor: ActorRef, imageKey: ImageKey, sourceFile: File, size: Int, format: ImageFormat)(listen: Future[LoadImageTask] ⇒ Unit) {
    Logger.debug(s"Loading image $imageKey from source file $sourceFile")
    val loadImageTask = Future {
      FileUtils.toByteString(sourceFile)
    }
    cacheImage(loadImageTask, listen, imageDataActor, imageKey, size, format)
  }

  def cacheImage(loadTask: Future[ByteString], listen: Future[LoadImageTask] ⇒ Unit, imageDataActor: ActorRef, imageKey: ImageKey, size: Int, format: ImageFormat) {
    val cacheTask = for {
      imageData ← loadTask
      resized ← ask(resizeActor, ResizeImageRequest(imageData, size, format)).mapTo[ResizeImageResponse]
      stored ← ask(imageDataActor, StoreImageRequest(imageKey, resized.data)).mapTo[StoreImageResponse]
    } yield LoadImageTask(resized.data, stored.offset, stored.size)
    tasks.put(imageKey, cacheTask)
    listen(cacheTask)
  }

  def registerNotifications(task: Future[LoadImageTask], recipient: ActorRef, storage: File, imageKey: ImageKey) {
    task onComplete {
      case Success(LoadImageTask(data, offset, storageSize)) ⇒
        Logger.debug(s"Stored image $imageKey to $storage, position $offset ($storageSize bytes)")
        self ! TaskComplete(imageKey, FilePosition(storage, offset, storageSize))
        recipient ! GetImageResponse(data)
      case Failure(t) ⇒
        self ! TaskFailed(imageKey, t)
        recipient ! Status.Failure(t)
    }
  }

  def imageDataActorFor(file: File): ActorRef = {
    imageDataActors.getOrElseUpdate(file, {
      Logger.info(s"Creating image data actor for file $file")
      context.actorOf(Props(classOf[ImageDataActor], file))
    })
  }

  def storageFor(imageKey: ImageKey): File = {
    // TODO: Switch to a new cache file if the old gets too large
    storageFile
  }

  def storageId: String =
    self.path.name

  def flushTasks() {
    val remainingTasks = Future.sequence(tasks.values)
    try {
      Await.result(remainingTasks, akkaTimeout.duration)
    } catch {
      case e: Exception ⇒
        Logger.error("Error while shutting down", e)
    }
  }
}

object ImageBrokerActor {
  case class GetImageRequest(source: String, size: Int, format: ImageFormat = JPEG)
  case class GetLocalImageRequest(source: File, id: String, size: Int, format: ImageFormat = JPEG)
  case class GetImageResponse(data: ByteString)

  private[actor] case class TaskComplete(key: ImageKey, position: FilePosition)
  private[actor] case class TaskFailed(key: ImageKey, t: Throwable)
  private[actor] case class LoadImageTask(data: ByteString, offset: Long, size: Long)
}
