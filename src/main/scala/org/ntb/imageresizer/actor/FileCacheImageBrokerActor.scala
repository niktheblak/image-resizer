package org.ntb.imageresizer.actor

import org.ntb.imageresizer.actor.ActorUtils.requireArgument
import org.ntb.imageresizer.actor.DownloadActor._
import org.ntb.imageresizer.actor.ResizeActor._
import org.ntb.imageresizer.cache.TempFileCacheProvider
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.resize.UnsupportedImageFormatException
import org.ntb.imageresizer.util.FileUtils.createTempFile
import org.ntb.imageresizer.util.StringUtils.isNullOrEmpty
import org.apache.http.HttpException
import org.ntb.imageresizer.actor.ActorUtils.requireArgument
import org.ntb.imageresizer.actor.DownloadActor._
import org.ntb.imageresizer.actor.ResizeActor._
import org.ntb.imageresizer.cache.TempFileCacheProvider
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.resize.UnsupportedImageFormatException
import org.ntb.imageresizer.util.FileUtils.createTempFile
import org.ntb.imageresizer.util.StringUtils.isNullOrEmpty
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Status
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.util.Timeout
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Success, Failure}
import java.io.File
import java.net.URI
import java.util.concurrent.TimeoutException

import language.postfixOps

class FileCacheImageBrokerActor(downloadActor: ActorRef, resizeActor: ActorRef) extends Actor
  with ActorLogging
  with TempFileCacheProvider[FileCacheImageBrokerActor.Key] {
  import FileCacheImageBrokerActor._
  import context.dispatcher
  
  protected case class RemoveFromBuffer(key: Key)
  implicit val timeout = Timeout(30 seconds)
  val encodingTasks: mutable.Map[Key, Future[Long]] = mutable.Map.empty
  
  override def cachePath = "imagebroker"

  override def preStart() {
    log.info("Starting FileCacheImageBrokerActor with cache directory %s".format(cacheDirectory()))
  }

  def receive = {
    case request @ GetImageRequest(uri, preferredSize, imageFormat) =>
      requireArgument(sender)(preferredSize > 0, "Size must be positive")
      val source = uri.toString
      val file = cacheFileProvider((source, preferredSize, imageFormat))
      if (file.exists()) {
        log.debug("Serving already cached image %s for request %s".format(file.getPath, request))
        sender ! GetImageResponse(file)
      } else {
        log.debug("Downloading and resizing image from request %s to %s".format(request, file.getPath))
        handleResizeTask(sender)((source, preferredSize, imageFormat), file, downloadAndResizeToFile(uri, file, preferredSize, imageFormat))
      }
    case GetLocalImageRequest(source, id, preferredSize, imageFormat) =>
      requireArgument(sender)(source.exists && source.canRead, "Source file must exist and be readable")
      requireArgument(sender)(!isNullOrEmpty(id), "Image ID must not be empty")
      requireArgument(sender)(preferredSize > 0, "Size must be positive")
      val file = cacheFileProvider(id, preferredSize, imageFormat)
      if (file.exists()) {
        sender ! GetImageResponse(file)
      } else {
        handleResizeTask(sender)((id, preferredSize, imageFormat), file, resize(source, file, preferredSize, imageFormat))
      }
    case RemoveFromBuffer(key) =>
      encodingTasks.remove(key)
    case ClearCache() =>
      log.info("Clearing cache directory " + cacheDirectory().getAbsolutePath)
      clearCacheDirectory()
  }

  def handleResizeTask(sender: ActorRef)(key: Key, file: File, resizeOp: => Future[Long]) {
    val resizeTask = encodingTasks.getOrElseUpdate(key, resizeOp)
    resizeTask onComplete {
      case Success(size) =>
        self ! RemoveFromBuffer(key)
        sender ! GetImageResponse(file)
      case Failure(t) =>
        self ! RemoveFromBuffer(key)
        file.delete()
        handleException(sender)(t)
    }
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
    val downloadTask = ask(downloadActor, DownloadToFileRequest(uri, target))
    for {
      downloadResponse <- downloadTask.mapTo[DownloadToFileResponse]
    } yield downloadResponse.fileSize
  }

  def resize(source: File, target: File, preferredSize: Int, format: ImageFormat): Future[Long] = {
    val resizeTask = ask(resizeActor, ResizeImageToFileRequest(source, target, preferredSize, format)).mapTo[ResizeImageToFileResponse]
    for (response <- resizeTask) yield response.fileSize
  }

  def handleException(sender: ActorRef)(t: Throwable) {
    t match {
      case e: TimeoutException =>
        sender ! Status.Failure(e)
      case e: UnsupportedImageFormatException =>
        sender ! Status.Failure(e)
      case e: HttpException =>
        sender ! Status.Failure(e)
      case e: Throwable =>
        sender ! Status.Failure(e)
        throw e
    }
  }
}

object FileCacheImageBrokerActor {
  type Key = (String, Int, ImageFormat)
  case class GetImageRequest(uri: URI, preferredSize: Int, format: ImageFormat = JPEG)
  case class GetLocalImageRequest(source: File, id: String, preferredSize: Int, format: ImageFormat = JPEG)
  case class GetImageResponse(data: File)
  case class ClearCache()
}