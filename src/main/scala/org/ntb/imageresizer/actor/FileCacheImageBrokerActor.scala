package org.ntb.imageresizer.actor

import org.apache.http.HttpException
import org.ntb.imageresizer.actor.DownloadActor._
import org.ntb.imageresizer.actor.ResizeActor._
import org.ntb.imageresizer.cache.TempFileCacheProvider
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.resize.UnsupportedImageFormatException
import org.ntb.imageresizer.util.FileUtils.createTempFile
import .isNullOrEmpty
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.util.Timeout
import concurrent.{Await, Future}
import scala.concurrent.duration._
import java.io.File
import java.net.URI
import java.util.concurrent.TimeoutException
import language.postfixOps

class FileCacheImageBrokerActor(downloadActor: ActorRef, resizeActor: ActorRef) extends Actor
    with ActorLogging
    with TempFileCacheProvider[FileCacheImageBrokerActor.Key]
    with ActorUtils {
  import FileCacheImageBrokerActor._
  import context.dispatcher

  val timeout: FiniteDuration = 30 seconds
  implicit val akkaTimeout = Timeout(timeout)
  
  override def cachePath = "imagebroker"

  override def preStart() {
    log.info("Starting FileCacheImageBrokerActor with cache directory %s".format(cacheDirectory()))
  }

  def receive = {
    case request @ GetImageRequest(uri, preferredSize, imageFormat) ⇒
      requireArgument(sender)(preferredSize > 0, "Size must be positive")
      val source = uri.toString
      val file = cacheFileProvider((source, preferredSize, imageFormat))
      if (file.exists()) {
        log.debug("Serving already cached image %s for request %s".format(file.getPath, request))
        sender ! GetImageResponse(file)
      } else {
        log.debug("Downloading and resizing image from request %s to %s".format(request, file.getPath))
        val downloadAndResizeTask = downloadAndResizeToFile(uri, file, preferredSize, imageFormat)
        actorTry(sender) {
          Await.result(downloadAndResizeTask, timeout)
          sender ! GetImageResponse(file)
        } actorCatch  {
          case e: TimeoutException ⇒
          case e: UnsupportedImageFormatException ⇒
          case e: HttpException ⇒
        }
      }
    case GetLocalImageRequest(source, id, preferredSize, imageFormat) ⇒
      requireArgument(sender)(source.exists && source.canRead, "Source file must exist and be readable")
      requireArgument(sender)(!isNullOrEmpty(id), "Image ID must not be empty")
      requireArgument(sender)(preferredSize > 0, "Size must be positive")
      val file = cacheFileProvider(id, preferredSize, imageFormat)
      if (file.exists()) {
        sender ! GetImageResponse(file)
      } else {
        val resizeTask = resize(source, file, preferredSize, imageFormat)
        actorTry(sender) {
          Await.result(resizeTask, timeout)
          sender ! GetImageResponse(file)
        } actorCatch  {
          case e: TimeoutException ⇒
          case e: UnsupportedImageFormatException ⇒
          case e: HttpException ⇒
        }
        actorTry(sender) {
          Await.result(resizeTask, timeout)
          sender ! GetImageResponse(file)
        } actorCatch  {
          case e: TimeoutException ⇒
          case e: UnsupportedImageFormatException ⇒
          case e: HttpException ⇒
        }
      }
    case ClearCache() ⇒
      log.info("Clearing cache directory " + cacheDirectory().getAbsolutePath)
      clearCacheDirectory()
  }

  def downloadAndResizeToFile(uri: URI, target: File, preferredSize: Int, format: ImageFormat): Future[Long] = {
    val tempFile = createTempFile()
    val resizeTask = for (
      data ← download(uri, tempFile);
      resizedSize ← resize(tempFile, target, preferredSize, format)
    ) yield resizedSize
    resizeTask onComplete {
      case _ ⇒ tempFile.delete()
    }
    resizeTask
  }

  def download(uri: URI, target: File): Future[Long] = {
    val downloadTask = ask(downloadActor, DownloadRequest(uri, target))
    for {
      downloadResponse ← downloadTask.mapTo[DownloadResponse]
    } yield downloadResponse.fileSize
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