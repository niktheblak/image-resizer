package org.ntb.imageresizer.actor

import java.io.{ IOException, File }

import akka.actor.{ Status, Actor }
import akka.util.ByteString
import org.ntb.imageresizer.storage.FlatFileImageStore

class ImageDataLoaderActor extends Actor with FlatFileImageStore {
  import ImageDataLoaderActor._

  override def receive = {
    case LoadImageDataRequest(file, offset) ⇒
      try {
        val data = readImage(file, offset)
        sender() ! LoadImageDataResponse(data)
      } catch {
        case e: IOException ⇒
          sender () ! Status.Failure(new IOException(s"Error while reading source file $file: ${e.getMessage}", e))
      }
  }
}

object ImageDataLoaderActor {
  case class LoadImageDataRequest(file: File, offset: Long)

  case class LoadImageDataResponse(data: ByteString)
}
