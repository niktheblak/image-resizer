package org.ntb.imageresizer.storage

import java.io.RandomAccessFile
import java.nio.ByteBuffer

import akka.util.ByteString
import org.ntb.imageresizer.io.ByteStringIO

trait ImageRecordSerializer extends FormatEncoder {
  val header: ByteString = ByteString('I', 'M', 'G', 'X')

  def paddingLength(position: Int): Int = {
    if (position % 8 != 0) 8 - (position % 8)
    else 0
  }

  def writePadding(output: ByteBuffer) {
    val length = paddingLength(output.position)
    for (_ ← 0 until length) output.put(0.toByte)
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
    assert(!buffer.hasRemaining, "Buffer was not completely filled by image write")
    buffer.rewind()
    output.getChannel.write(buffer)
  }

  private def writeToBuffer(image: ImageRecord, output: ByteBuffer) {
    output.put(header.asByteBuffer)
    output.put(image.flags.toByte)
    val keyData = image.key.getBytes("UTF-8")
    require(keyData.length <= Short.MaxValue, s"Key length ${keyData.length} too large")
    output.putShort(keyData.length.toShort)
    output.put(keyData)
    output.putInt(image.size)
    output.put(encode(image.format))
    output.putInt(image.data.size)
    image.data.copyToBuffer(output)
    writePadding(output)
    assert(output.position() % 8 == 0, s"Output size ${output.position()} not aligned to word boundary")
  }

  def readImageRecord(input: RandomAccessFile, size: Long): ImageRecord = {
    val buffer = ByteBuffer.allocate(size.toInt)
    input.getChannel.read(buffer)
    buffer.rewind()
    readFromBuffer(buffer)
  }

  private def readFromBuffer(input: ByteBuffer): ImageRecord = {
    val headerData = ByteStringIO.read(input, header.length)
    require(headerData.equals(header), s"Invalid header: ${headerData.toString()}")
    val flags = input.get
    val keySize = input.getShort
    val keyData = new Array[Byte](keySize)
    input.get(keyData)
    val key = new String(keyData, "UTF-8")
    val size = input.getInt
    val format = decode(input.get)
    val dataSize = input.getInt
    require(dataSize <= input.remaining(), s"Data size $dataSize larger than remaining in buffer ${input.remaining}")
    val data = new Array[Byte](dataSize)
    input.get(data)
    skipPadding(input)
    ImageRecord(key, size, format, flags, ByteString(data))
  }

  private def skipPadding(buffer: ByteBuffer) {
    val padding = paddingLength(buffer.position)
    buffer.position(buffer.position() + padding)
  }
}
