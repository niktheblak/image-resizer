package org.ntb.imageresizer.storage

import java.io.{ DataInput, RandomAccessFile }
import java.util

import akka.util.ByteString

trait ImageRecordIO extends FormatEncoder {
  val header = "IMGX".getBytes("US-ASCII")

  def writeImageRecord(image: ImageRecord, output: RandomAccessFile) {
    output.write(header)
    output.writeByte(image.flags)
    output.writeUTF(image.key)
    output.writeInt(image.size)
    output.writeByte(encode(image.format))
    output.writeInt(image.data.size)
    image.data.foreach(output.writeByte(_))
    writePadding(output)
  }

  def readImageRecord(input: DataInput): ImageRecord = {
    val headerBuffer = new Array[Byte](header.length)
    input.readFully(headerBuffer)
    require(util.Arrays.equals(headerBuffer, header))
    val flags = input.readUnsignedByte()
    val key = input.readUTF()
    val size = input.readInt()
    val format = decode(input.readByte())
    val dataLength = input.readInt()
    val data = new Array[Byte](dataLength)
    input.readFully(data)
    ImageRecord(key, size, format, flags, ByteString(data))
  }

  def writePadding(output: RandomAccessFile) {
    val position = output.getFilePointer
    if (position % 8 != 0) {
      val paddingLength = 8 - (position % 8).toInt
      for (i ← 0 until paddingLength) output.writeByte(0)
    }
  }
}
