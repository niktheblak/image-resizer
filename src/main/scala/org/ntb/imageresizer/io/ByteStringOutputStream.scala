package org.ntb.imageresizer.io

import java.io.OutputStream

import akka.util.ByteStringBuilder

class ByteStringOutputStream extends OutputStream {
  val builder = new ByteStringBuilder()
  
  override def write(b: Int) {
    builder += b.asInstanceOf[Byte]
  }
  
  override def write(bytes: Array[Byte]) {
    builder ++= bytes
  }
  
  override def write(bytes: Array[Byte], offset: Int, length: Int) {
    builder ++= bytes.slice(offset, offset + length)
  }
  
  def toByteString() = {
    builder.result()
  }
}