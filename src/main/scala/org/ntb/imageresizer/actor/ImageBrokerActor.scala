package org.ntb.imageresizer.actor

import java.io._
import java.net.URI
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.{ Timeout, ByteString }
import com.google.common.hash.Hashing
import com.google.common.io.BaseEncoding
import org.ntb.imageresizer.storage.{ TempDirectoryStorageFile, TempDirectoryIndexFile, FlatFileImageStore }
import org.ntb.imageresizer.imageformat.{ JPEG, ImageFormat }
import org.ntb.imageresizer.util.Loans

import scala.collection.mutable
import scala.util.{ Success, Failure }

class ImageBrokerActor(downloadActor: ActorRef, resizeActor: ActorRef)
    extends Actor
    with ActorLogging
    with TempDirectoryIndexFile
    with TempDirectoryStorageFile {
  import ImageBrokerActor._
  import ImageDataLoaderActor._
  import DownloadActor._
  import ResizeActor._

  val index = mutable.Map.empty[ImageKey, FilePosition]
  val hashFunction = Hashing.goodFastHash(128)
  val encoding = BaseEncoding.base64Url().omitPadding()
  val imageDataLoader = context.actorOf(Props[ImageDataLoaderActor])
  implicit val executionContext = context.dispatcher
  implicit val akkaTimeout = Timeout(30, TimeUnit.SECONDS)

  override def receive = {
    case GetImageRequest(source, size, format) ⇒
      val imageKey = ImageKey(getKey(source.toString, size, format), size, format)
      index.get(imageKey) match {
        case Some(pos) ⇒
          log.info("Serving cached image {}", imageKey)
          val dataResponse = ask(imageDataLoader, LoadImageDataRequest(pos.storage, pos.offset)).mapTo[LoadImageDataResponse]
          pipe(dataResponse map { r ⇒ GetImageResponse(r.data) }) to sender()
        case None ⇒
          log.info("Loading image {} from source {}", imageKey, source)
          val senderRef = sender()
          val task = for (
            downloaded ← ask(downloadActor, DownloadRequest(source)).mapTo[DownloadResponse];
            resized ← ask(resizeActor, ResizeImageRequest(downloaded.data, size, format)).mapTo[ResizeImageResponse]
          ) yield resized.data
          task onComplete {
            case Success(data) ⇒
              val storage = storageFile
              val (offset, storageSize) = writeImage(storage, imageKey.key, imageKey.size, imageKey.format, data)
              log.info("Stored image {} to {}, position {} ({} bytes)", imageKey, storage, offset, storageSize)
              self ! PutToCache(imageKey, FilePosition(storage, offset))
              senderRef ! GetImageResponse(data)
            case Failure(t) ⇒
              senderRef ! Status.Failure(t)
          }
      }
    case PutToCache(key, position) ⇒
      index.put(key, position)
    case GetLocalImageRequest(source, id, size, format) ⇒
      sender() ! Status.Failure(new NotImplementedError("Method not implemented"))
  }

  override def preStart() {
    loadIndex()
  }

  override def postStop() {
    saveIndex()
    index.clear()
  }

  def storageId: String = getClass.getSimpleName

  private def getKey(source: String, size: Int, format: ImageFormat): String = {
    val hasher = hashFunction.newHasher()
    hasher.putUnencodedChars(source)
    hasher.putInt(size)
    hasher.putByte(formatToByte(format))
    val hash = hasher.hash()
    encoding.encode(hash.asBytes())
  }

  private def saveIndex() {
    val file = indexFile
    Loans.using(new DataOutputStream(new FileOutputStream(file))) { output ⇒
      output.writeInt(index.size)
      index foreach {
        case (k, v) ⇒
          output.writeUTF(k.key)
          output.writeInt(k.size)
          output.writeByte(formatToByte(k.format))
          output.writeUTF(v.storage.getPath)
          output.writeLong(v.offset)
      }
    }
  }

  private def loadIndex() {
    index.clear()
    val file = indexFile
    if (file.exists() && file.length() > 0) {
      Loans.using(new DataInputStream(new FileInputStream(file))) { input ⇒
        val n = input.readInt()
        for (i ← 0 until n) {
          val key = input.readUTF()
          val size = input.readInt()
          val format = byteToFormat(input.readByte())
          val path = input.readUTF()
          val offset = input.readLong()
          index.put(ImageKey(key, size, format), FilePosition(new File(path), offset))
        }
      }
    }
  }
}

object ImageBrokerActor extends FlatFileImageStore {
  case class GetImageRequest(source: URI, size: Int, format: ImageFormat = JPEG) {
    require(size > 0, "Size must be positive")
  }

  case class GetLocalImageRequest(source: File, id: String, size: Int, format: ImageFormat = JPEG) {
    require(source.exists && source.canRead, "Source file must exist and be readable")
    require(!id.isEmpty, "Image ID must not be empty")
    require(size > 0, "Size must be positive")
  }

  case class GetImageResponse(data: ByteString)

  private[actor] case class ImageKey(key: String, size: Int, format: ImageFormat) {
    override def toString: String = {
      s"Image(Key: $key, size: $size, format: $format)"
    }
  }

  private[actor] case class FilePosition(storage: File, offset: Long)

  private[actor] case class PutToCache(key: ImageKey, position: FilePosition)
}
