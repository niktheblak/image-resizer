package org.ntb.imageresizer.storage

import java.io.{ DataInput, RandomAccessFile, File }
import java.nio.ByteOrder
import java.util

import akka.util.ByteString
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.util.Loans

private[storage] case class ImageRecord(key: String, size: Int, format: ImageFormat, flags: Int, data: ByteString)

trait FlatFileImageStore {
  implicit val byteOrder = ByteOrder.LITTLE_ENDIAN
  val header = "IMGX".getBytes("US-ASCII")

  def writeImage(storage: File, key: String, size: Int, format: ImageFormat, data: ByteString): (Long, Long) = {
    Loans.using(new RandomAccessFile(storage, "rw")) { output ⇒
      val offset = output.length()
      output.seek(offset)
      writeImageRecord(ImageRecord(key, size, format, 0, data), output)
      val storageSize = output.getFilePointer - offset
      (offset, storageSize)
    }
  }

  def readImage(storage: File, offset: Long): ByteString = {
    require(offset < storage.length())
    Loans.using(new RandomAccessFile(storage, "r")) { input ⇒
      input.seek(offset)
      val image = readImageRecord(input)
      image.data
    }
  }

  private def writeImageRecord(image: ImageRecord, output: RandomAccessFile) {
    output.write(header)
    output.writeByte(image.flags)
    output.writeUTF(image.key)
    output.writeInt(image.size)
    output.writeByte(formatToByte(image.format))
    output.writeInt(image.data.size)
    image.data.foreach(output.writeByte(_))
    writePadding(output)
  }

  private def readImageRecord(input: DataInput): ImageRecord = {
    val headerBuffer = new Array[Byte](header.length)
    input.readFully(headerBuffer)
    require(util.Arrays.equals(headerBuffer, header))
    val flags = input.readUnsignedByte()
    val key = input.readUTF()
    val size = input.readInt()
    val format = byteToFormat(input.readByte())
    val dataLength = input.readInt()
    val data = new Array[Byte](dataLength)
    input.readFully(data)
    ImageRecord(key, size, format, flags, ByteString(data))
  }

  private def writePadding(output: RandomAccessFile) {
    val position = output.getFilePointer
    if (position % 8 != 0) {
      val paddingLength = 8 - (position % 8).toInt
      for (i ← 0 until paddingLength) output.writeByte(0)
    }
  }

  def formatToByte(format: ImageFormat): Byte = format match {
    case JPEG ⇒ 0.toByte
    case PNG ⇒ 1.toByte
    case GIF ⇒ 2.toByte
    case _ ⇒ throw new IllegalArgumentException("Invalid format: " + format)
  }

  def byteToFormat(b: Byte): ImageFormat = b match {
    case 0 ⇒ JPEG
    case 1 ⇒ PNG
    case 2 ⇒ GIF
    case n ⇒ throw new IllegalArgumentException("Invalid format byte: " + n)
  }
}
