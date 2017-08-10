package org.ntb.imageresizer.storage

import java.io.{ File, RandomAccessFile }

import akka.util.ByteString
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.util.Loans
import org.scalatest.{ FlatSpec, Matchers }

class ImageRecordSerializerTest extends FlatSpec with Matchers {
  import ImageRecordSerializerTest._

  val testData: ByteString = ByteString(1.toByte, 2.toByte, 3.toByte)

  val serializer = new TestImageRecordSerializer

  def writeImage(image: ImageRecord, file: File): Unit =
    Loans.using(new RandomAccessFile(file, "rw")) { raFile =>
      serializer.writeImageRecord(image, raFile)
    }

  def readImage(file: File): ImageRecord =
    Loans.using(new RandomAccessFile(file, "r")) { raFile =>
      serializer.readImageRecord(raFile, file.length())
    }

  "ImageRecordSerializer" should "correctly write images" in {
    val file = createTempFile()
    val image = ImageRecord("testKey", 10, JPEG, 0, testData)
    writeImage(image, file)
    file.length should equal(32)
    file.delete()
  }

  it should "correctly read serialized images" in {
    val file = createTempFile()
    val image = ImageRecord("testKey", 10, JPEG, 0, testData)
    writeImage(image, file)
    val storedImage = readImage(file)
    storedImage should equal(image)
    file.delete()
  }

  it should "correctly report serialized image size" in {
    val file = createTempFile()
    val image = ImageRecord("testKey", 10, JPEG, 0, testData)
    writeImage(image, file)
    file.length should equal(serializer.serializedSize(image))
    file.delete()
  }
}

object ImageRecordSerializerTest {
  class TestImageRecordSerializer extends ImageRecordSerializer

  def createTempFile(): File = {
    val file = File.createTempFile("ImageRecordSerializerTest-", ".tmp")
    file.deleteOnExit()
    file
  }
}
