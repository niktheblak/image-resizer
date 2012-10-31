package org.ntb.imageresizer.actor

import java.io.File
import java.net.URI

import org.ntb.imageresizer.actor.BaseDownloadActor.DownloadToFileRequest
import org.ntb.imageresizer.actor.BaseDownloadActor.DownloadToFileResponse
import org.ntb.imageresizer.actor.ResizeActor.ResizeImageToFileRequest
import org.ntb.imageresizer.actor.ResizeActor.ResizeImageToFileResponse
import org.ntb.imageresizer.imageformat.defaultImageFormat
import org.ntb.imageresizer.imageformat.parseImageFormatFromUri

import akka.actor.ActorContext
import akka.dispatch.Future
import akka.pattern.ask
import akka.util.Timeout

trait ActorImageDownloader {
  val tempFilePrefix = "ResizingImageDownloader-"
  protected val context: ActorContext
  implicit val timeout: Timeout
  
  def downloadToFile(uri: URI, target: File): Future[Long] = {
    val downloadActor = context.actorFor("/user/downloader")
    val downloadTask = ask(downloadActor, DownloadToFileRequest(uri, target))
    for {
      downloadResponse <- downloadTask.mapTo[DownloadToFileResponse]
    } yield downloadResponse.fileSize
  }
  
  def downloadAndResizeToFile(uri: URI, target: File, preferredSize: Int): Future[Unit] = {
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