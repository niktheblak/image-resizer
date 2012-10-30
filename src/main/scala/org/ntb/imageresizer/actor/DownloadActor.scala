package org.ntb.imageresizer.actor

import java.io.File
import java.io.FileOutputStream
import java.net.URI

import org.apache.http.HttpException
import org.ntb.imageresizer.io.DefaultHttpClient
import org.ntb.imageresizer.io.FileDownloader
import org.ntb.imageresizer.util.Loans.using

import akka.actor.Actor
import akka.actor.Status
import akka.actor.actorRef2Scala
import akka.util.ByteString

class DownloadActor extends Actor with FileDownloader with DefaultHttpClient {
  import context.dispatcher
  import DownloadActor._

  def receive = {
    case DownloadRequest(uri) =>
      try {
        val data = downloadToByteString(uri)
        sender ! DownloadResponse(data)
      } catch {
        case e: HttpException => sender ! Status.Failure(e)
        case e: Exception =>
          sender ! Status.Failure(e)
          throw e
      }
    case DownloadFileRequest(uri, target) =>
      try {
        using(new FileOutputStream(target)) { output =>
          val fileSize = downloadToFile(uri, output)
          sender ! DownloadFileResponse(fileSize)
        }
      } catch {
        case e: HttpException => sender ! Status.Failure(e)
        case e: Exception =>
          sender ! Status.Failure(e)
          throw e
      }
  }
}

object DownloadActor {
  case class DownloadRequest(uri: URI)
  case class DownloadResponse(data: ByteString)
  case class DownloadFileRequest(uri: URI, target: File)
  case class DownloadFileResponse(fileSize: Long)
}