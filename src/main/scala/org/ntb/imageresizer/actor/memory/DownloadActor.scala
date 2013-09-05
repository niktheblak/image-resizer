package org.ntb.imageresizer.actor.memory

import akka.actor.Actor
import akka.util.{ByteStringBuilder, ByteString}
import java.io.IOException
import java.net.URI
import org.apache.http.HttpException
import org.ntb.imageresizer.actor.ActorUtils
import org.ntb.imageresizer.io.{DefaultHttpClientProvider, Downloader}

class DownloadActor extends Actor with Downloader with DefaultHttpClientProvider with ActorUtils {
  import DownloadActor._

  def receive = {
    case DownloadRequest(uri) ⇒
      actorTry(sender) {
        val builder = new ByteStringBuilder
        download(uri, builder.asOutputStream)
        sender ! DownloadResponse(builder.result())
      } actorCatch {
        case e: HttpException ⇒
        case e: IOException ⇒
      }
  }
}

object DownloadActor {
  case class DownloadRequest(uri: URI)
  case class DownloadResponse(data: ByteString)
}