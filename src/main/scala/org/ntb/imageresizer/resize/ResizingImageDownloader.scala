package org.ntb.imageresizer.resize

import java.net.URI

import org.ntb.imageresizer.actor.DownloadFileRequest
import org.ntb.imageresizer.actor.DownloadFileResponse
import org.ntb.imageresizer.actor.ResizeImageRequest
import org.ntb.imageresizer.actor.ResizeImageResponse
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.util.FilePathUtils

import akka.actor.ActorContext
import akka.dispatch.Future
import akka.pattern.ask
import akka.util.ByteString
import akka.util.Timeout

trait ResizingImageDownloader {
  def downloadAndResize(uri: URI, preferredSize: Int)(implicit context: ActorContext, timeout: Timeout): Future[ByteString] = {
    val resizeActor = context.actorFor("/user/resizer")
    val format = getFormat(uri)
    val downloadTask = download(uri)
    for (
        data <- downloadTask;
        resizeResponse <- ask(resizeActor, ResizeImageRequest(data, preferredSize, format)).mapTo[ResizeImageResponse]
    ) yield resizeResponse.data
  }
  
  def download(uri: URI)(implicit context: ActorContext, timeout: Timeout): Future[ByteString] = {
    val downloadActor = context.actorFor("/user/downloader")
    val downloadTask = ask(downloadActor, DownloadFileRequest(uri))
    for {
      downloadResponse <- downloadTask.mapTo[DownloadFileResponse]
    } yield downloadResponse.data
  }

  def getFormat(uri: URI): ImageFormat = {
    val format = for {
      ext <- FilePathUtils.getFileExtension(uri)
      format <- parseRequestedImageFormat(ext)
    } yield format
    format getOrElse defaultImageFormat
  }
}