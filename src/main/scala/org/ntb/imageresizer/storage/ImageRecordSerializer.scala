package org.ntb.imageresizer.storage

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util

import akka.util.ByteString

trait ImageRecordSerializer extends FormatEncoder {
  val header = "IMGX".getBytes("US-ASCII")

  def paddingLength(position: Int): Int = {
    if (position % 8 != 0) 8 - (position % 8)
    else 0
  }

  def serializedSize(image: ImageRecord): Int = {
    val length =
      header.length + // Header
        1 + // Flags
        2 + // Key size
        image.key.getBytes("UTF-8").length + // Key data
        4 + // Image size
        1 + // Image format
        4 + // Image data size
        image.data.length // Image data
    length + paddingLength(length) // Total length + padding
  }

  def writeImageRecord(image: ImageRecord, output: RandomAccessFile) {
    val buffer = ByteBuffer.allocate(serializedSize(image))
    writeToBuffer(image, buffer)
    assert(!buffer.hasRemaining)
    buffer.rewind()
    output.getChannel.write(buffer)
  }

  private def writeToBuffer(image: ImageRecord, output: ByteBuffer) {
    output.put(header)
    output.put(image.flags.toByte)
    val keyData = image.key.getBytes("UTF-8")
    require(keyData.length <= Short.MaxValue)
    output.putShort(keyData.length.toShort)
    output.put(keyData)
    output.putInt(image.size)
    output.put(encode(image.format))
    output.putInt(image.data.size)
    image.data.copyToBuffer(output)
    writePadding(output)
    assert(output.position % 8 == 0)
  }

  def readImageRecord(input: RandomAccessFile, size: Long): ImageRecord = {
    val buffer = ByteBuffer.allocate(size.toInt)
    input.getChannel.read(buffer)
    buffer.rewind()
    readFromBuffer(buffer)
  }

  private def readFromBuffer(input: ByteBuffer): ImageRecord = {
    val headerData = new Array[Byte](header.length)
    input.get(headerData)
    require(util.Arrays.equals(headerData, header), s"Invalid header: ${util.Arrays.toString(headerData)}")
    val flags = input.get
    val keySize = input.getShort
    val keyData = new Array[Byte](keySize)
    input.get(keyData)
    val key = new String(keyData, "UTF-8")
    val size = input.getInt
    val format = decode(input.get)
    val dataSize = input.getInt
    require(dataSize <= input.remaining(), s"Invalid data size: $dataSize, remaining: ${input.remaining}")
    val data = new Array[Byte](dataSize)
    input.get(data)
    val padding = paddingLength(input.position)
    input.position(input.position + padding)
    ImageRecord(key, size, format, flags, ByteString(data))
  }

  def writePadding(output: ByteBuffer) {
    val length = paddingLength(output.position)
    for (i ← 0 until length) output.put(0.toByte)
  }
}
