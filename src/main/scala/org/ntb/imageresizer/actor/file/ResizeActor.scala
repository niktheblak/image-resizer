package org.ntb.imageresizer.actor.file

import akka.actor.Actor
import java.io.File
import org.ntb.imageresizer.actor.ActorUtils
import org.ntb.imageresizer.imageformat.ImageFormat
import org.ntb.imageresizer.resize.Resizer._
import org.ntb.imageresizer.resize.UnsupportedImageFormatException

class ResizeActor extends Actor with ActorUtils {
  import ResizeActor._

  def receive = {
    case ResizeImageRequest(source, target, size, format) ⇒
      actorTry(sender) {
        resizeImage(source, target, size, format)
        sender ! ResizeImageResponse(target.length())
      } actorCatch {
        case e: UnsupportedImageFormatException ⇒
      }
  }
}

object ResizeActor {
  case class ResizeImageRequest(source: File, target: File, size: Int, format: ImageFormat)
  case class ResizeImageResponse(fileSize: Long)
}