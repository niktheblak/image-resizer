package org.ntb.imageresizer.actor.file

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.google.common.base.Strings.isNullOrEmpty
import concurrent.duration._
import concurrent.{Await, Future}
import java.io.{IOException, File}
import java.net.URI
import language.postfixOps
import org.ntb.imageresizer.actor.{ActorNameCachePath, ActorUtils}
import org.ntb.imageresizer.actor.file.DownloadActor._
import org.ntb.imageresizer.actor.file.ResizeActor._
import org.ntb.imageresizer.cache.TempFileCache
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.util.FileUtils.createTempFile

class FileCacheImageBrokerActor(downloadActor: ActorRef, resizeActor: ActorRef) extends Actor
    with ActorLogging
    with TempFileCache[FileCacheImageBrokerActor.Key]
    with ActorNameCachePath
    with ActorUtils {
  import FileCacheImageBrokerActor._
  import context.dispatcher

  override val cacheRoot = "imagebroker"
  implicit val timeout: Timeout = 30 seconds

  override def preStart() {
    log.info(s"Starting FileCacheImageBrokerActor with cache directory ${cacheDirectory()}")
  }

  def receive = {
    case request @ GetImageRequest(uri, preferredSize, imageFormat) ⇒
      requireArgument(sender)(preferredSize > 0, "Size must be positive")
      val source = uri.toString
      val key = (source, preferredSize, imageFormat)
      val file = cacheFileProvider(key)
      if (file.exists()) {
        log.debug(s"Serving already cached image ${file.getPath} for request $request")
        sender ! GetImageResponse(file)
      } else {
        log.debug(s"Downloading and resizing image from request $request to ${file.getPath}")
        val downloadAndResizeTask = downloadAndResizeToFile(uri, file, preferredSize, imageFormat)
        actorTry(sender) {
          val resizedSize = Await.result(downloadAndResizeTask, timeout.duration)
          log.debug(s"Download and resize complete, resized image size $resizedSize bytes")
          sender ! GetImageResponse(file)
        } actorCatch {
          case e: IOException ⇒
        }
      }
    case GetLocalImageRequest(source, id, preferredSize, imageFormat) ⇒
      requireArgument(sender)(source.exists && source.canRead, "Source file must exist and be readable")
      requireArgument(sender)(!isNullOrEmpty(id), "Image ID must not be empty")
      requireArgument(sender)(preferredSize > 0, "Size must be positive")
      val key = (id, preferredSize, imageFormat)
      val file = cacheFileProvider(key)
      if (file.exists()) {
        sender ! GetImageResponse(file)
      } else {
        val resizeTask = resize(source, file, preferredSize, imageFormat)
        actorTry(sender) {
          val resizedSize = Await.result(resizeTask, timeout.duration)
          log.debug(s"Local resize complete, resized image size $resizedSize bytes")
          sender ! GetImageResponse(file)
        } actorCatch {
          case e: IOException ⇒
        }
      }
    case ClearCache() ⇒
      log.info(s"Clearing cache directory ${cacheDirectory().getAbsolutePath}")
      clearCacheDirectory()
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
}