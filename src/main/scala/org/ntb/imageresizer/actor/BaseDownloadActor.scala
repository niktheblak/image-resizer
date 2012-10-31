package org.ntb.imageresizer.actor

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI

import org.apache.http.HttpException
import org.ntb.imageresizer.io.Downloader
import org.ntb.imageresizer.util.Loans.using

import akka.actor.Actor
import akka.actor.Status
import akka.actor.actorRef2Scala
import akka.util.ByteString

abstract class BaseDownloadActor extends Actor with Downloader {
  import context.dispatcher
  import BaseDownloadActor._
  
  def receive = {
    case DownloadRequest(uri) =>
      try {
        val data = download(uri)
        sender ! DownloadResponse(data)
      } catch {
        case e: HttpException => sender ! Status.Failure(e)
        case e: Exception =>
          sender ! Status.Failure(e)
          throw e        
      }
    case DownloadToFileRequest(uri, target) =>
      try {
        using(new FileOutputStream(target)) { output =>
          val fileSize = download(uri, output)
          sender ! DownloadToFileResponse(fileSize)
        }
      } catch {
        case e: HttpException => sender ! Status.Failure(e)
        case e: IOException => sender ! Status.Failure(e)
        case e: Exception =>
          sender ! Status.Failure(e)
          throw e
      }
  }
}

object BaseDownloadActor {
  case class DownloadRequest(uri: URI)
  case class DownloadResponse(data: ByteString)
  case class DownloadToFileRequest(uri: URI, target: File)
  case class DownloadToFileResponse(fileSize: Long)
}