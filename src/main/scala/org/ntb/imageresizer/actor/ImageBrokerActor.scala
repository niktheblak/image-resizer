package org.ntb.imageresizer.actor

import java.io._
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.{ ByteString, Timeout }
import org.ntb.imageresizer.imageformat.{ ImageFormat, JPEG }
import org.ntb.imageresizer.storage._
import org.ntb.imageresizer.util.FileUtils
import spray.http.{ IllegalUriException, Uri }

import scala.collection.mutable
import scala.concurrent.{ Await, Future }
import scala.util.{ Failure, Success }

class ImageBrokerActor(downloadActor: ActorRef, resizeActor: ActorRef)
    extends Actor
    with ActorLogging
    with ActorUtils
    with IndexStore
    with KeyEncoder
    with CurrentDirectoryIndexFile
    with CurrentDirectoryStorageFile {
  import DownloadActor._
  import ImageBrokerActor._
  import ImageDataActor._
  import ResizeActor._

  val index = mutable.Map.empty[ImageKey, FilePosition]
  val tasks = mutable.Map.empty[ImageKey, Future[LoadImageTask]]
  val imageDataActors = mutable.Map.empty[File, ActorRef]

  implicit val executionContext = context.dispatcher
  implicit val akkaTimeout = Timeout(30, TimeUnit.SECONDS)

  override def receive = {
    case request @ GetImageRequest(source, size, format) ⇒
      actorTry(sender()) {
        validateGetImageRequest(request)
      } actorCatch {
        case e: IllegalArgumentException ⇒
      }
      val imageKey = ImageKey(encodeKey(source, size, format), size, format)
      val storage = storageFor(imageKey)
      val imageDataActor = imageDataActorFor(storage)
      val recipient = sender()
      handleGetImageRequest(imageDataActor, size, format, imageKey) {
        // No cache task was found for this image, start a new task
        val sourceUrl = validateUri(source)
        loadAndCacheRemoteImage(imageDataActor, imageKey, sourceUrl, size, format) { task ⇒
          registerNotifications(task, recipient, storage, imageKey)
        }
      }
    case TaskComplete(key, position) ⇒
      tasks.remove(key)
      index.put(key, position)
    case TaskFailed(key, t) ⇒
      tasks.remove(key)
      log.error(t, "Resize task failed for image {}", key)
    case request @ GetLocalImageRequest(source, id, size, format) ⇒
      actorTry(sender()) {
        validateGetLocalImageRequest(request)
      } actorCatch {
        case e: IllegalArgumentException ⇒
      }
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
    log.debug("Looking up image {} from index", imageKey)
    index.get(imageKey) match {
      case Some(pos) ⇒
        log.debug("Serving cached image {}", imageKey)
        val dataResponse = ask(imageDataActor, LoadImageRequest(pos.offset, pos.size)).mapTo[LoadImageResponse]
        pipe(dataResponse map { r ⇒ GetImageResponse(r.data) }) to sender()
      case None ⇒
        // Image was not found from index, check ongoing cache tasks
        val senderRef = sender()
        tasks.get(imageKey) match {
          case Some(task) ⇒
            log.debug("Task for image {} already exists, using existing task", imageKey)
            task onComplete {
              case Success(LoadImageTask(data, offset, storageSize)) ⇒
                senderRef ! GetImageResponse(data)
              case Failure(t) ⇒
                senderRef ! Status.Failure(t)
            }
          case None ⇒
            log.debug("Image {} not found, caching image from remote source", imageKey)
            loadImage
        }
    }
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
    imageDataActors.values.foreach(context.stop)
    imageDataActors.clear()
    if (index.nonEmpty) {
      log.info("Saving index ({} items) to {}", index.size, indexFile)
      saveIndex(index, indexFile)
      index.clear()
    }
  }

  def loadAndCacheRemoteImage(imageDataActor: ActorRef, imageKey: ImageKey, sourceUri: Uri, size: Int, format: ImageFormat)(listen: Future[LoadImageTask] ⇒ Unit) {
    log.debug("Loading image {} from URL {}", imageKey, sourceUri)
    val loadImageTask = ask(downloadActor, DownloadRequest(sourceUri)).mapTo[DownloadResponse] map { response ⇒
      response.data
    }
    cacheImage(loadImageTask, listen, imageDataActor, imageKey, size, format)
  }

  def cacheLocalImage(imageDataActor: ActorRef, imageKey: ImageKey, sourceFile: File, size: Int, format: ImageFormat)(listen: Future[LoadImageTask] ⇒ Unit) {
    log.debug("Loading image {} from source file {}", imageKey, sourceFile)
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
        log.debug("Stored image {} to {}, position {} ({} bytes)", imageKey, storage, offset, storageSize)
        self ! TaskComplete(imageKey, FilePosition(storage, offset, storageSize))
        recipient ! GetImageResponse(data)
      case Failure(t) ⇒
        self ! TaskFailed(imageKey, t)
        recipient ! Status.Failure(t)
    }
  }

  def imageDataActorFor(file: File): ActorRef = {
    imageDataActors.getOrElseUpdate(file, {
      log.info("Creating image data actor for file {}", file)
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
        log.error("Error while shutting down", e)
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

  def validateGetImageRequest(request: GetImageRequest) {
    validateUri(request.source)
    require(request.size > 0, "Size must be positive")
  }

  def validateGetLocalImageRequest(request: GetLocalImageRequest) {
    require(request.source.exists && request.source.canRead, "Source file must exist and be readable")
    require(!request.id.isEmpty, "Image ID must not be empty")
    require(request.size > 0, "Size must be positive")
  }

  def validateUri(source: String): Uri = {
    try {
      val uri = Uri(source, Uri.ParsingMode.Strict)
      require(!uri.isEmpty && uri.isAbsolute, "Source URI must be absolute")
      uri
    } catch {
      case e: IllegalUriException ⇒
        throw new IllegalArgumentException(e.getMessage)
      case e: IllegalArgumentException ⇒
        throw e
    }
  }
}
