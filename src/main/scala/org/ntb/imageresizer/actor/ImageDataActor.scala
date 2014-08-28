package org.ntb.imageresizer.actor

import java.io.{ File, IOException }

import akka.actor.Actor
import akka.util.ByteString
import org.ntb.imageresizer.storage.{ FlatFileImageStore, ImageKey }

class ImageDataActor extends Actor with FlatFileImageStore with ActorUtils {
  import org.ntb.imageresizer.actor.ImageDataActor._

  override def receive = {
    case LoadImageRequest(file, offset) ⇒
      actorTry(sender()) {
        val data = readImage(file, offset)
        sender() ! LoadImageResponse(data)
      } actorCatch {
        case e: IOException ⇒
      }
    case StoreImageRequest(storage, key, data) ⇒
      actorTry(sender()) {
        val (offset, size) = writeImage(storage, key.key, key.size, key.format, data)
        sender() ! StoreImageResponse(offset, size)
      } actorCatch {
        case e: IOException ⇒
      }
  }
}

object ImageDataActor {
  case class LoadImageRequest(file: File, offset: Long)

  case class LoadImageResponse(data: ByteString)

  case class StoreImageRequest(storage: File, key: ImageKey, data: ByteString)

  case class StoreImageResponse(offset: Long, size: Long)
}
