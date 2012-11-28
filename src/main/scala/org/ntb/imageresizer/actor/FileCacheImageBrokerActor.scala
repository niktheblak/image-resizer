package org.ntb.imageresizer.actor

import java.io.File
import java.net.URI
import java.util.concurrent.TimeoutException
import org.ntb.imageresizer.actor.ActorUtils.requireArgument
import org.ntb.imageresizer.actor.DownloadActor._
import org.ntb.imageresizer.cache.TempFileCacheProvider
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.resize.UnsupportedImageFormatException
import org.ntb.imageresizer.actor.ResizeActor._
import org.ntb.imageresizer.util.FilePathUtils.createTempFile
import org.ntb.imageresizer.util.StringUtils.isNullOrEmpty
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Status
import akka.actor.actorRef2Scala
import akka.dispatch.Await
import akka.dispatch.Future
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration.intToDurationInt
import org.apache.http.HttpException

class FileCacheImageBrokerActor extends Actor
  with ActorLogging
  with TempFileCacheProvider[(String, Int)]
  with ActorNameCachePath {
  import context.dispatcher
  import FileCacheImageBrokerActor._

  implicit val timeout = Timeout(30 seconds)

  override def preStart() {
    log.info("Starting FileCacheImageBrokerActor with cache directory %s".format(cachePath))
  }

  def receive = {
    case request @ GetImageRequest(uri, preferredSize, imageFormat) =>
      requireArgument(sender)(preferredSize > 0, "Size must be positive")
      val file = cacheFileProvider((uri.toString(), preferredSize))
      if (file.exists()) {
        log.debug("Serving already cached image %s for request %s".format(file.getPath(), request))
        sender ! GetImageResponse(file)
      } else {
        log.debug("Downloading and resizing image from request %s to %s".format(request, file.getPath()))
        val downloadTask = downloadAndResizeToFile(uri, file, preferredSize, imageFormat)
        try {
          Await.result(downloadTask, timeout.duration)
          log.debug("Image from request %s was successfully downloaded to %s".format(request, file.getPath()))
          sender ! GetImageResponse(file)
        } catch {
          case e: Exception =>
            file.delete()
            handleException(e)
        }
      }
    case GetLocalImageRequest(source, id, preferredSize, imageFormat) =>
      requireArgument(sender)(source.exists() && source.canRead(), "Source file must exist and be readable")
      requireArgument(sender)(!isNullOrEmpty(id), "Image ID must not be empty")
      requireArgument(sender)(preferredSize > 0, "Size must be positive")
      val file = cacheFileProvider(id, preferredSize)
      if (file.exists()) {
        sender ! GetImageResponse(file)
      } else {
        val resizeTask = resize(source, file, preferredSize, imageFormat)
        try {
          Await.result(resizeTask, timeout.duration)
          sender ! GetImageResponse(file)
        } catch {
          case e: Exception =>
            file.delete()
            handleException(e)
        }
      }
    case ClearCache() =>
      log.info("Clearing cache directory " + cacheDirectory().getAbsolutePath())
      clearCacheDirectory()
  }

  def downloadAndResizeToFile(uri: URI, target: File, preferredSize: Int, format: ImageFormat): Future[Long] = {
    val tempFile = createTempFile()
    val resizeTask = for (
      data <- download(uri, tempFile);
      resizedSize <- resize(tempFile, target, preferredSize, format)
    ) yield resizedSize
    resizeTask onComplete {
      case _ => tempFile.delete()
    }
    resizeTask
  }

  def download(uri: URI, target: File): Future[Long] = {
    val downloadActor = context.actorFor("/user/downloader")
    val downloadTask = ask(downloadActor, DownloadToFileRequest(uri, target))
    for {
      downloadResponse <- downloadTask.mapTo[DownloadToFileResponse]
    } yield downloadResponse.fileSize
  }

  def resize(source: File, target: File, preferredSize: Int, format: ImageFormat): Future[Long] = {
    val resizeActor = context.actorFor("/user/resizer")
    val resizeTask = ask(resizeActor, ResizeImageToFileRequest(source, target, preferredSize, format)).mapTo[ResizeImageToFileResponse]
    for (response <- resizeTask) yield response.fileSize
  }

  def handleException(ex: Exception) {
    ex match {
      case e: TimeoutException =>
        sender ! Status.Failure(e)
      case e: UnsupportedImageFormatException =>
        sender ! Status.Failure(e)
      case e: HttpException =>
        sender ! Status.Failure(e)
      case e: Exception =>
        sender ! Status.Failure(e)
        throw e
    }
  }
}

object FileCacheImageBrokerActor {
  case class GetImageRequest(uri: URI, preferredSize: Int, format: ImageFormat = JPEG)
  case class GetLocalImageRequest(source: File, id: String, preferredSize: Int, format: ImageFormat = JPEG)
  case class GetImageResponse(data: File)
  case class ClearCache()
}