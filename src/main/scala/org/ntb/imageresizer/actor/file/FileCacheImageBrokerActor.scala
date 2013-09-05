package org.ntb.imageresizer.actor.file

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.google.common.base.Strings.isNullOrEmpty
import concurrent.duration._
import concurrent.{Await, Future}
import java.io.File
import java.net.URI
import org.ntb.imageresizer.actor.ActorUtils
import org.ntb.imageresizer.actor.file.DownloadActor._
import org.ntb.imageresizer.actor.file.ResizeActor._
import org.ntb.imageresizer.cache.TempFileCache
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.util.FileUtils.createTempFile
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class FileCacheImageBrokerActor(downloadActor: ActorRef, resizeActor: ActorRef) extends Actor
    with ActorLogging
    with TempFileCache[FileCacheImageBrokerActor.Key]
    with ActorUtils {
  import FileCacheImageBrokerActor._
  import context.dispatcher

  override val cachePath = "imagebroker"
  implicit val timeout: Timeout = 30.seconds
  val workQueue = mutable.Map.empty[Key, Future[Long]]

  override def preStart() {
    log.info(s"Starting FileCacheImageBrokerActor with cache directory ${cacheDirectory()}")
  }

  override def postStop() {
    flushWorkQueue()
  }

  def receive = {
    case request @ GetImageRequest(uri, preferredSize, imageFormat) ⇒
      requireArgument(sender)(preferredSize > 0, "Size must be positive")
      val source = uri.toString
      val key = (source, preferredSize, imageFormat)
      val file = cacheFileProvider(key)
      workQueue.get(key) match {
        case Some(task) ⇒ task onComplete replyWithResizedImage(self, sender, key, file)
        case None ⇒
          if (file.exists()) {
            log.debug(s"Serving already cached image ${file.getPath} for request $request")
            sender ! GetImageResponse(file)
          } else {
            log.debug(s"Downloading and resizing image from request $request to ${file.getPath}")
            val task = downloadAndResizeToFile(uri, file, preferredSize, imageFormat)
            workQueue.put(key, task)
            task onComplete replyWithResizedImage(self, sender, key, file)
          }
      }
    case GetLocalImageRequest(source, id, preferredSize, imageFormat) ⇒
      requireArgument(sender)(source.exists && source.canRead, "Source file must exist and be readable")
      requireArgument(sender)(!isNullOrEmpty(id), "Image ID must not be empty")
      requireArgument(sender)(preferredSize > 0, "Size must be positive")
      val key = (id, preferredSize, imageFormat)
      val file = cacheFileProvider(key)
      workQueue.get(key) match {
        case Some(task) ⇒ task onComplete replyWithResizedImage(self, sender, key, file)
        case None ⇒
          if (file.exists()) {
            sender ! GetImageResponse(file)
          } else {
            val task = resize(source, file, preferredSize, imageFormat)
            workQueue.put(key, task)
            task onComplete replyWithResizedImage(self, sender, key, file)
          }
      }
    case ClearCache() ⇒
      log.info(s"Clearing cache directory ${cacheDirectory().getAbsolutePath}")
      flushWorkQueue()
      clearCacheDirectory()
    case RemoveFromWorkQueue(key) ⇒
      workQueue.remove(key)
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
    for (response ← resizeTask) yield response.fileSize
  }
}

object FileCacheImageBrokerActor {
  type Key = (String, Int, ImageFormat)
  case class GetImageRequest(uri: URI, preferredSize: Int, format: ImageFormat = JPEG)
  case class GetLocalImageRequest(source: File, id: String, preferredSize: Int, format: ImageFormat = JPEG)
  case class GetImageResponse(data: File)
  case class ClearCache()
  private case class RemoveFromWorkQueue(key: Key)
}