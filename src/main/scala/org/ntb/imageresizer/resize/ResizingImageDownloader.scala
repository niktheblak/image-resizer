package org.ntb.imageresizer.resize

import java.net.URI
import org.ntb.imageresizer.actor.DownloadActor._
import org.ntb.imageresizer.actor.ResizeActor._
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.util.FilePathUtils
import akka.actor.ActorContext
import akka.dispatch.Future
import akka.pattern.ask
import akka.util.ByteString
import akka.util.Timeout
import java.io.File

trait ResizingImageDownloader {
  val tempFilePrefix = "ResizingImageDownloader-"
  
  def downloadToFile(uri: URI, target: File)(implicit context: ActorContext, timeout: Timeout): Future[Long] = {
    val downloadActor = context.actorFor("/user/downloader")
    val downloadTask = ask(downloadActor, DownloadRequest(uri, target))
    for {
      downloadResponse <- downloadTask.mapTo[DownloadResponse]
    } yield downloadResponse.fileSize
  }
  
  def downloadAndResizeToFile(uri: URI, target: File, preferredSize: Int)(implicit context: ActorContext, timeout: Timeout): Future[Unit] = {
    val resizeActor = context.actorFor("/user/resizer")
    val format = parseImageFormatFromUri(uri) getOrElse defaultImageFormat
    val tempFile = File.createTempFile(tempFilePrefix, ".tmp")
    tempFile.deleteOnExit()
    for (
        data <- downloadToFile(uri, tempFile);
        resizeResponse <- ask(resizeActor, ResizeImageToFileRequest(tempFile, target, preferredSize, format)).mapTo[ResizeImageToFileResponse]
    ) yield {
      tempFile.delete()
      ()
    }
  }
}