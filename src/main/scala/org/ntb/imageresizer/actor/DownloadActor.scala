package org.ntb.imageresizer.actor

import akka.actor.Actor
import akka.pattern.pipe
import akka.util.ByteString
import spray.client.pipelining._
import spray.http._

import scala.concurrent.Future

class DownloadActor extends Actor with ActorUtils {
  import context.dispatcher
  import org.ntb.imageresizer.actor.DownloadActor._

  val pipeline: HttpRequest ⇒ Future[Array[Byte]] = (
    sendReceive
    ~> unmarshal[Array[Byte]]
  )

  def receive = {
    case DownloadRequest(uri) ⇒
      val response = pipeline(Get(uri))
      response map { r ⇒ DownloadResponse(ByteString(r)) } pipeTo sender
  }
}

object DownloadActor {
  case class DownloadRequest(uri: Uri)
  case class DownloadResponse(data: ByteString)
}
