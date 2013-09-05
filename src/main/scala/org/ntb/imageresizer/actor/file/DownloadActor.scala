package org.ntb.imageresizer.actor.file

import akka.actor.Actor
import java.io.{File, IOException}
import java.net.URI
import org.apache.http.HttpException
import org.ntb.imageresizer.actor.ActorUtils
import org.ntb.imageresizer.io.DefaultHttpClientProvider
import org.ntb.imageresizer.io.Downloader

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