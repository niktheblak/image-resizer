package org.ntb.imageresizer.actor

import java.io._
import java.net.URI
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.{ ByteString, Timeout }
import com.google.common.hash.Hashing
import com.google.common.io.BaseEncoding
import org.ntb.imageresizer.imageformat.{ ImageFormat, JPEG }
import org.ntb.imageresizer.storage.{ FlatFileImageStore, TempDirectoryIndexFile, TempDirectoryStorageFile }
import org.ntb.imageresizer.util.Loans

import scala.collection.mutable
import scala.concurrent.{ Await, Future }
import scala.util.{ Failure, Success }

class ImageBrokerActor(downloadActor: ActorRef, resizeActor: ActorRef)
    extends Actor
    with ActorLogging
    with TempDirectoryIndexFile
    with TempDirectoryStorageFile {
  import org.ntb.imageresizer.actor.DownloadActor._
  import org.ntb.imageresizer.actor.ImageBrokerActor._
  import org.ntb.imageresizer.actor.ImageDataActor._
  import org.ntb.imageresizer.actor.ResizeActor._

  val index = mutable.Map.empty[ImageKey, FilePosition]
  val tasks = mutable.Map.empty[ImageKey, Future[(ByteString, Long, Long)]]
  val hashFunction = Hashing.goodFastHash(128)
  val encoding = BaseEncoding.base64Url().omitPadding()
  val imageDataLoader = context.actorOf(Props[ImageDataActor])
  implicit val executionContext = context.dispatcher
  implicit val akkaTimeout = Timeout(30, TimeUnit.SECONDS)

  override def receive = {
    case GetImageRequest(source, size, format) ⇒
      val imageKey = ImageKey(getKey(source.toString, size, format), size, format)
      index.get(imageKey) match {
        case Some(pos) ⇒
          log.info("Serving cached image {}", imageKey)
          val dataResponse = ask(imageDataLoader, LoadImageRequest(pos.storage, pos.offset)).mapTo[LoadImageResponse]
          pipe(dataResponse map { r ⇒ GetImageResponse(r.data) }) to sender()
        case None ⇒
          val senderRef = sender()
          tasks.get(imageKey) match {
            case Some(task) ⇒
              log.info("Task for image {} already exists, using existing task", imageKey)
              task onComplete {
                case Success((data, offset, storageSize)) ⇒
                  senderRef ! GetImageResponse(data)
                case Failure(t) ⇒
                  senderRef ! Status.Failure(t)
              }
            case None ⇒
              log.info("Loading image {} from source {}", imageKey, source)
              val storage = storageFile
              val task = for (
                downloaded ← ask(downloadActor, DownloadRequest(source)).mapTo[DownloadResponse];
                resized ← ask(resizeActor, ResizeImageRequest(downloaded.data, size, format)).mapTo[ResizeImageResponse];
                stored ← ask(imageDataLoader, StoreImageRequest(storage, imageKey, resized.data)).mapTo[StoreImageResponse]
              ) yield (resized.data, stored.offset, stored.size)
              tasks.put(imageKey, task)
              task onComplete {
                case Success((data, offset, storageSize)) ⇒
                  log.info("Stored image {} to {}, position {} ({} bytes)", imageKey, storage, offset, storageSize)
                  self ! TaskComplete(imageKey, FilePosition(storage, offset))
                  senderRef ! GetImageResponse(data)
                case Failure(t) ⇒
                  senderRef ! Status.Failure(t)
              }
          }
      }
    case TaskComplete(key, position) ⇒
      tasks.remove(key)
      index.put(key, position)
    case GetLocalImageRequest(source, id, size, format) ⇒
      sender() ! Status.Failure(new NotImplementedError("Method not implemented"))
  }

  override def preStart() {
    loadIndex()
  }

  override def postStop() {
    if (tasks.nonEmpty) {
      flushTasks()
    }
    saveIndex()
    index.clear()
  }

  def storageId: String = getClass.getSimpleName

  private def flushTasks() {
    val remainingTasks = Future.sequence(tasks.values)
    try {
      Await.result(remainingTasks, akkaTimeout.duration)
    } catch {
      case e: Exception ⇒
        log.error("Error while shutting down", e)
    }
  }

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

  private[actor] case class TaskComplete(key: ImageKey, position: FilePosition)
}
