package org.ntb.imageresizer.actor

import java.io.{ File, RandomAccessFile }

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import akka.util.ByteString
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.storage.{ FlatFileImageStore, ImageKey, StorageFileProvider }
import org.ntb.imageresizer.util.FileUtils
import org.scalatest._

import scala.concurrent.duration._

class ImageDataActorTest
    extends TestKit(ActorSystem("TestActorSystem"))
    with ImplicitSender
    with FlatSpecLike
    with Matchers
    with BeforeAndAfter
    with BeforeAndAfterAll
    with FlatFileImageStore
    with StorageFileProvider {
  import ImageDataActor._

  val testTimeout = 2.seconds
  val testData = ByteString(1, 2, 3)

  private var storageFile: File = _
  private var storageBackend: RandomAccessFile = _
  private var imageDataActor: TestActorRef[ImageDataActor] = _

  def storage = storageBackend

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  before {
    storageFile = FileUtils.createTempFile(prefix = "ImageDataActorTest")
    storageBackend = new RandomAccessFile(storageFile, "rw")
    imageDataActor = TestActorRef(new ImageDataActor(storageFile))
  }

  after {
    imageDataActor.stop()
    storageBackend.close()
    storageFile.delete()
  }

  "ImageDataActor" should "read image data" in {
    writeImage("testKey", 100, JPEG, testData)
    imageDataActor ! LoadImageRequest(0, storageBackend.length())
    expectMsgPF(testTimeout) {
      case LoadImageResponse(data) =>
        data shouldEqual testData
    }
  }

  it should "write image data" in {
    val imageKey = ImageKey("testKey", 100, JPEG)
    imageDataActor ! StoreImageRequest(imageKey, testData)
    expectMsgPF(testTimeout) {
      case StoreImageResponse(storedOffset, storedSize) =>
        storedOffset shouldEqual 0L
        storedSize shouldEqual storageBackend.length()
    }
    val storedImageData = readImage(0, storageBackend.length())
    storedImageData shouldEqual testData
  }

  it should "return and seek to correct offset" in {
    val imageKey1 = ImageKey("testKey1", 100, JPEG)
    val imageKey2 = ImageKey("testKey2", 100, JPEG)
    imageDataActor ! StoreImageRequest(imageKey1, ByteString(3, 4, 5))
    expectMsgPF(testTimeout) {
      case StoreImageResponse(storedOffset, storedSize) =>
        storedOffset shouldEqual 0L
        storedSize shouldEqual storageBackend.length()
    }
    imageDataActor ! StoreImageRequest(imageKey2, testData)
    expectMsgPF(testTimeout) {
      case StoreImageResponse(storedOffset, storedSize) =>
        storedOffset shouldEqual 32L
        storedSize shouldEqual 32L
        val storedImageData = readImage(storedOffset, storedSize)
        storedImageData shouldEqual testData
    }
  }
}
