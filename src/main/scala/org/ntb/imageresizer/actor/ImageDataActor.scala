package org.ntb.imageresizer.actor

import java.io.{ RandomAccessFile, File, IOException }

import akka.actor.Actor
import akka.util.ByteString
import org.ntb.imageresizer.storage.{ FlatFileImageStore, ImageKey }

class ImageDataActor(storage: File) extends Actor with FlatFileImageStore with ActorUtils {
  import org.ntb.imageresizer.actor.ImageDataActor._

  private var storageBackend: RandomAccessFile = _

  override def receive = {
    case LoadImageRequest(offset) ⇒
      actorTry(sender()) {
        val data = readImage(storageBackend, offset)
        sender() ! LoadImageResponse(data)
      } actorCatch {
        case e: IOException ⇒
      }
    case StoreImageRequest(key, data) ⇒
      actorTry(sender()) {
        val (offset, size) = writeImage(storageBackend, key.key, key.size, key.format, data)
        sender() ! StoreImageResponse(offset, size)
      } actorCatch {
        case e: IOException ⇒
      }
  }

  override def preStart() {
    storageBackend = new RandomAccessFile(storage, "rw")
  }

  override def postStop() {
    storageBackend.close()
  }
}

object ImageDataActor {
  case class LoadImageRequest(offset: Long)

  case class LoadImageResponse(data: ByteString)

  case class StoreImageRequest(key: ImageKey, data: ByteString)

  case class StoreImageResponse(offset: Long, size: Long)
}
