package org.ntb.imageresizer.actor

import org.ntb.imageresizer.actor.DownloadActor._
import org.ntb.imageresizer.actor.ResizeActor._
import org.ntb.imageresizer.cache.TempFileCacheProvider
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.util.FileUtils.createTempFile
import com.google.common.base.Strings.isNullOrEmpty
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import collection.mutable
import concurrent.{Await, Future}
import concurrent.duration._
import util.{Try, Success, Failure}
import java.io.File
import java.net.URI
import language.postfixOps

class FileCacheImageBrokerActor(downloadActor: ActorRef, resizeActor: ActorRef) extends Actor
    with ActorLogging
    with TempFileCacheProvider[FileCacheImageBrokerActor.Key]
    with ActorUtils {
  import FileCacheImageBrokerActor._
  import context.dispatcher

  protected case class RemoveFromWorkQueue(key: Key)

  val timeout: FiniteDuration = 30 seconds
  implicit val akkaTimeout = Timeout(timeout)
  val workQueue = mutable.Map[FileCacheImageBrokerActor.Key, Future[Long]]()
  
  override def cachePath = "imagebroker"

  override def preStart() {
    log.info("Starting FileCacheImageBrokerActor with cache directory %s".format(cacheDirectory()))
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
      if (workQueue.contains(key)) {
        workQueue(key) onComplete(replyWithResizedImage(self, sender, key, file))
      } else {
        if (file.exists()) {
          log.debug("Serving already cached image %s for request %s".format(file.getPath, request))
          sender ! GetImageResponse(file)
        } else {
          log.debug("Downloading and resizing image from request %s to %s".format(request, file.getPath))
          val downloadAndResizeTask = downloadAndResizeToFile(uri, file, preferredSize, imageFormat)
          workQueue.put(key, downloadAndResizeTask)
          downloadAndResizeTask onComplete(replyWithResizedImage(self, sender, key, file))
        }
      }
    case GetLocalImageRequest(source, id, preferredSize, imageFormat) ⇒
      requireArgument(sender)(source.exists && source.canRead, "Source file must exist and be readable")
      requireArgument(sender)(!isNullOrEmpty(id), "Image ID must not be empty")
      requireArgument(sender)(preferredSize > 0, "Size must be positive")
      val key = (id, preferredSize, imageFormat)
      val file = cacheFileProvider(key)
      if (workQueue.contains(key)) {
        workQueue(key) onComplete(replyWithResizedImage(self, sender, key, file))
      } else {
        if (file.exists()) {
          sender ! GetImageResponse(file)
        } else {
          val resizeTask = resize(source, file, preferredSize, imageFormat)
          workQueue.put(key, resizeTask)
          resizeTask onComplete(replyWithResizedImage(self, sender, key, file))
        }
      }
    case ClearCache() ⇒
      log.info("Clearing cache directory " + cacheDirectory().getAbsolutePath)
      flushWorkQueue()
      clearCacheDirectory()
    case RemoveFromWorkQueue(key) ⇒
      workQueue.remove(key)
  }

  def flushWorkQueue() {
    if (!workQueue.isEmpty) {
      val tasks = Future.sequence(workQueue.values)
      Await.ready(tasks, timeout)
      workQueue.clear()
    }
  }

  def replyWithResizedImage[A](self: ActorRef, sender: ActorRef, key: Key, file: File): Try[Long] => Unit = {
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
      resizedSize ← resize(downloadResponse.target, target , preferredSize, format)
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
}