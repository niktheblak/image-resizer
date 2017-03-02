package org.ntb.imageresizer.actor

import java.io.{ File, IOException, RandomAccessFile }

import akka.actor.Actor
import akka.util.ByteString
import org.ntb.imageresizer.storage.{ FlatFileImageStore, ImageKey, StorageFileProvider }

class ImageDataActor(storageFile: File)
    extends Actor
    with FlatFileImageStore
    with StorageFileProvider
    with ActorUtils {
  import org.ntb.imageresizer.actor.ImageDataActor._

  private var storageBackend: RandomAccessFile = _

  def storage: RandomAccessFile = storageBackend

  override def receive = {
    case LoadImageRequest(offset, size) ⇒
      actorTry(sender()) {
        val data = readImage(offset, size)
        sender() ! LoadImageResponse(data)
      } actorCatch {
        case e: IOException ⇒
      }
    case StoreImageRequest(key, data) ⇒
      actorTry(sender()) {
        val (offset, size) = writeImage(key.key, key.size, key.format, data)
        sender() ! StoreImageResponse(offset, size)
      } actorCatch {
        case e: IOException ⇒
      }
  }

  override def preStart() {
    storageBackend = new RandomAccessFile(storageFile, "rw")
  }

  override def postStop() {
    storageBackend.close()
  }
}

object ImageDataActor {
  case class LoadImageRequest(offset: Long, size: Long)

  case class LoadImageResponse(data: ByteString)

  case class StoreImageRequest(key: ImageKey, data: ByteString)

  case class StoreImageResponse(offset: Long, size: Long)
}
