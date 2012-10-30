package org.ntb.imageresizer.actor

import java.io.File
import java.io.FileOutputStream
import java.net.URI

import org.apache.http.HttpException
import org.ntb.imageresizer.io.FileDownloader
import org.ntb.imageresizer.util.Loans.using

import akka.actor.Actor
import akka.actor.Status
import akka.actor.actorRef2Scala
import akka.util.ByteString

class DownloadActor extends Actor with FileDownloader {
  import context.dispatcher
  import DownloadActor._

  override val httpClient = createDefaultHttpClient()
  
  def receive = {
    case DownloadRequest(uri, target) =>
      try {
        using(new FileOutputStream(target)) { output =>
          val fileSize = download(uri, output)
          sender ! DownloadResponse(fileSize)
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
  case class DownloadRequest(uri: URI, target: File)
  case class DownloadResponse(fileSize: Long)
}