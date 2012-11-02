package org.ntb.imageresizer.actor

import java.io.File
import java.net.URI
import java.util.concurrent.TimeoutException
import org.ntb.imageresizer.cache.TempFileCacheProvider
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.resize.UnsupportedImageFormatException
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Status
import akka.actor.actorRef2Scala
import akka.dispatch.Await
import akka.util.Timeout
import akka.util.duration.intToDurationInt
import org.apache.http.HttpException

class FileCacheImageBrokerActor extends Actor
  with ActorLogging
  with ActorImageDownloader
  with TempFileCacheProvider[(URI, Int)]
  with ActorNameCachePath {
  import context.dispatcher
  import FileCacheImageBrokerActor._
  val timeout = Timeout(30 seconds)

  override def preStart() {
    log.info("Starting FileCacheImageBrokerActor with cache directory %s".format(cachePath))
  }

  def receive = {
    case request @ GetImageRequest(uri, preferredSize, imageFormat) =>
      val file = cacheFileProvider((uri, preferredSize))
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
    case ClearCache() =>
      log.info("Clearing cache directory " + cacheDirectory().getAbsolutePath())
      clearCacheDirectory()
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
  case class GetImageResponse(data: File)
  case class ClearCache()
}