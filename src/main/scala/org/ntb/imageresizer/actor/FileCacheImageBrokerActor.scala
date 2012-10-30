package org.ntb.imageresizer.actor

import java.io.File
import java.net.URI
import java.util.concurrent.TimeoutException

import org.ntb.imageresizer.cache.TempFileCacheProvider
import org.ntb.imageresizer.imageformat.ImageFormat
import org.ntb.imageresizer.resize.UnsupportedImageFormatException

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Status
import akka.actor.actorRef2Scala
import akka.dispatch.Await
import akka.util.Timeout
import akka.util.duration.intToDurationInt

class FileCacheImageBrokerActor extends Actor
  with ActorLogging
  with ActorImageDownloader
  with TempFileCacheProvider[(URI, Int)]
  with ActorNameCachePath {
  import context.dispatcher
  import FileCacheImageBrokerActor._
  val timeout = Timeout(10 seconds)

  override def preStart() {
    log.info("Starting %s with cache directory %s".format(classOf[FileCacheImageBrokerActor].getCanonicalName(), cachePath))
  }

  def receive = {
    case request @ GetImageRequest(uri, preferredSize) =>
      val file = cacheFileProvider((uri, preferredSize))
      if (file.exists()) {
        log.debug("Serving already cached image %s for request %s".format(file.getPath(), request))
        sender ! GetImageResponse(file)
      } else {
        log.debug("Downloading and resizing image from request %s to %s".format(request, file.getPath()))
        val downloadTask = downloadAndResizeToFile(uri, file, preferredSize)
        try {
          Await.result(downloadTask, timeout.duration)
          log.debug("Image from request %s was successfully downloaded to %s".format(request, file.getPath()))
          sender ! GetImageResponse(file)
        } catch {
          case e: TimeoutException =>
            file.delete()
            sender ! Status.Failure(e)
          case e: UnsupportedImageFormatException =>
            file.delete()
            sender ! Status.Failure(e)
          case e: Exception =>
            file.delete()
            sender ! Status.Failure(e)
            throw e
        }
      }
    case ClearCache() =>
      log.info("Cleaning cache directory " + cacheDirectory().getAbsolutePath())
      clearCacheDirectory()
  }
}

object FileCacheImageBrokerActor {
  case class GetImageRequest(uri: URI, preferredSize: Int)
  case class GetImageResponse(data: File)
  case class ResizeImageRequest(source: File, size: Int, format: ImageFormat)
  case class ResizeImageResponse(target: File)
  case class ClearCache()
}