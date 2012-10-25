package org.ntb.imageresizer.actor

import java.net.URI

import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.Status
import akka.util.ByteString

import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.HttpConnectionParams
import org.apache.http.HttpException
import org.ntb.imageresizer.io.FileDownloader

case class DownloadFileRequest(uri: URI)
case class DownloadFileResponse(data: ByteString)

class DownloadActor extends Actor with FileDownloader {
  import context.dispatcher
  
  override val httpClient = createHttpClient()
  val defaultHttpTimeout = 10000

  def receive = {
    case DownloadFileRequest(uri) =>
      try {
        val data = downloadFile(uri)
        sender ! DownloadFileResponse(data)
      } catch {
        case e: HttpException =>
          sender ! Status.Failure(e)
        case e: Exception =>
          sender ! Status.Failure(e)
          throw e
      }
  }
  
  def createHttpClient(timeout: Int = defaultHttpTimeout): HttpClient = {
    val httpClient = new DefaultHttpClient()
    val params = httpClient.getParams()
    HttpConnectionParams.setConnectionTimeout(params, timeout)
    HttpConnectionParams.setSoTimeout(params, timeout)
    httpClient
  }
}