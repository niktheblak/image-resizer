package org.ntb.imageresizer.actor.file

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import concurrent.duration._
import concurrent.{ Await, Future }
import java.io.File
import java.net.URI
import org.ntb.imageresizer.actor.Key
import org.ntb.imageresizer.actor.file.DownloadActor._
import org.ntb.imageresizer.actor.file.ResizeActor._
import org.ntb.imageresizer.cache.TempFileCache
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.util.FileUtils.createTempFile
import scala.collection.mutable
import scala.util.{ Failure, Success, Try }

class FileCacheImageBrokerActor(downloadActor: ActorRef, resizeActor: ActorRef) extends Actor
    with ActorLogging
    with TempFileCache[Key] {
  import FileCacheImageBrokerActor._
  import context.dispatcher

  override val cachePath = "imagebroker"
  private implicit val timeout: Timeout = 30.seconds
  private val workQueue = mutable.Map.empty[Key, Future[Long]]

  override def preStart() {
    log.info(s"Starting ${getClass.getSimpleName} with cache directory ${cacheDirectory()}")
  }

  override def postStop() {
    log.info(s"Stopping ${getClass.getSimpleName}")
    flushWorkQueue()
  }

  def receive = {
    case request @ GetImageRequest(uri, preferredSize, imageFormat) ⇒
      handleGetImageRequest(self, sender, request)
    case request @ GetLocalImageRequest(source, id, preferredSize, imageFormat) ⇒
      handleGetLocalImageRequest(self, sender, request)
    case ClearCache() ⇒
      log.info(s"Clearing cache directory ${cacheDirectory().getAbsolutePath}")
      flushWorkQueue()
      clearCacheDirectory()
    case RemoveFromWorkQueue(key) ⇒
      workQueue.remove(key)
  }

  def handleGetImageRequest(self: ActorRef, sender: ActorRef, request: GetImageRequest) {
    val key = Key(request.uri, request.size, request.format)
    val file = cacheFileProvider(key)
    workQueue.get(key) match {
      case Some(task) ⇒ task onComplete replyWithResizedImage(self, sender, key, file)
      case None ⇒
        if (file.exists()) {
          log.debug(s"Serving already cached image ${file.getPath} for request $request")
          sender ! GetImageResponse(file)
        } else {
          log.debug(s"Downloading and resizing image from request $request to ${file.getPath}")
          val task = downloadAndResizeToFile(request.uri, file, request.size, request.format)
          workQueue.put(key, task)
          task onComplete replyWithResizedImage(self, sender, key, file)
        }
    }
  }

  def handleGetLocalImageRequest(self: ActorRef, sender: ActorRef, request: GetLocalImageRequest) {
    val key = Key(toUri(request.id), request.size, request.format)
    val file = cacheFileProvider(key)
    workQueue.get(key) match {
      case Some(task) ⇒ task onComplete replyWithResizedImage(self, sender, key, file)
      case None ⇒
        if (file.exists()) {
          sender ! GetImageResponse(file)
        } else {
          val task = resize(request.source, file, request.size, request.format)
          workQueue.put(key, task)
          task onComplete replyWithResizedImage(self, sender, key, file)
        }
    }
  }

  def flushWorkQueue() {
    if (!workQueue.isEmpty) {
      log.info(s"Flushing work queue with ${workQueue.size} tasks")
      val tasks = Future.sequence(workQueue.values)
      try {
        Await.ready(tasks, timeout.duration)
        log.info("Work queue flushed")
      } catch {
        case e: Exception ⇒ log.error("Error while flushing work queue", e)
      }
      workQueue.clear()
    }
  }

  def replyWithResizedImage[A](self: ActorRef, sender: ActorRef, key: Key, file: File): Try[Long] ⇒ Unit = {
    case Success(t) ⇒
      self ! RemoveFromWorkQueue(key)
      sender ! GetImageResponse(file)
    case Failure(t) ⇒
      file.delete()
      self ! RemoveFromWorkQueue(key)
      sender ! Status.Failure(t)
  }

  def downloadAndResizeToFile(uri: URI, target: File, preferredSize: Int, format: ImageFormat): Future[Long] = {
    val tempFile = createTempFile()
    val resizeTask = for (
      downloadResponse ← ask(downloadActor, DownloadRequest(uri, tempFile)).mapTo[DownloadResponse];
      resizedSize ← resize(downloadResponse.target, target, preferredSize, format)
    ) yield resizedSize
    resizeTask onComplete {
      case _ ⇒ tempFile.delete()
    }
    resizeTask
  }

  def resize(source: File, target: File, preferredSize: Int, format: ImageFormat): Future[Long] = {
    val resizeTask = ask(resizeActor, ResizeImageRequest(source, target, preferredSize, format)).mapTo[ResizeImageResponse]
    resizeTask map (_.fileSize)
  }

  def toUri(id: String): URI = new URI(id)
}

object FileCacheImageBrokerActor {
  case class GetImageRequest(uri: URI, size: Int, format: ImageFormat = JPEG) {
    require(size > 0, "Size must be positive")
  }

  case class GetLocalImageRequest(source: File, id: String, size: Int, format: ImageFormat = JPEG) {
    require(source.exists && source.canRead, "Source file must exist and be readable")
    require(!id.isEmpty, "Image ID must not be empty")
    require(size > 0, "Size must be positive")
  }

  case class GetImageResponse(data: File)

  case class ClearCache()

  private case class RemoveFromWorkQueue(key: Key)
}