package org.ntb.imageresizer.actor

import java.io.{File, IOException}
import java.net.URI
import org.apache.http.HttpException
import org.ntb.imageresizer.io.Downloader
import org.ntb.imageresizer.io.DefaultHttpClientProvider
import akka.actor.Actor

class DownloadActor extends Actor with Downloader with DefaultHttpClientProvider with ActorUtils {
  import DownloadActor._

  def receive = {
    case DownloadRequest(uri, target) ⇒
      actorTry(sender) {
        val fileSize = download(uri, target)
        sender ! DownloadResponse(target, fileSize)
      } actorCatch {
        case e: HttpException ⇒
        case e: IOException ⇒
      }
  }
}

object DownloadActor {
  case class DownloadRequest(uri: URI, target: File)
  case class DownloadResponse(target: File, fileSize: Long)
}