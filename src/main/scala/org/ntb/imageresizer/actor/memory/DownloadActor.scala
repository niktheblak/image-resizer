package org.ntb.imageresizer.actor.memory

import akka.actor.Actor
import akka.util.ByteString
import java.io.{ByteArrayOutputStream, IOException}
import java.net.URI
import org.apache.http.HttpException
import org.ntb.imageresizer.actor.ActorUtils
import org.ntb.imageresizer.io.{DefaultHttpClientProvider, Downloader}
import org.ntb.imageresizer.util.Loans.using

class DownloadActor extends Actor with Downloader with DefaultHttpClientProvider with ActorUtils {
  import DownloadActor._

  def receive = {
    case DownloadRequest(uri) ⇒
      actorTry(sender) {
        using(new ByteArrayOutputStream()) { output =>
          download(uri, output)
          val data = ByteString(output.toByteArray)
          sender ! DownloadResponse(data)
        }
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
