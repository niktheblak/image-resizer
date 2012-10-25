package org.ntb.imageresizer.io

import java.io.InputStream

import scala.math.min

import akka.util.ByteString

class ByteStringInputStream(byteString: ByteString) extends InputStream {
  private val input = byteString.asByteBuffer

  override def read(): Int = {
    if (!input.hasRemaining()) -1 else input.get() & 0xFF
  }

  override def read(bytes: Array[Byte], offset: Int, length: Int): Int = {
    if (!input.hasRemaining()) {
      -1
    } else {
      val len = min(length, input.remaining())
      input.get(bytes, offset, len)
      len
    }
  }
}