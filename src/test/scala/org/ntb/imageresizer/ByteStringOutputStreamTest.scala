package org.ntb.imageresizer

import org.ntb.imageresizer.io.ByteStringOutputStream
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import akka.util.ByteString

class ByteStringOutputStreamTest extends FlatSpec with ShouldMatchers {
  "ByteStringOutputStream" should "return a ByteString of data that was written into it via write(Int)" in {
    val output = new ByteStringOutputStream()
    output.write(1)
    output.write(2)
    output.write(3)
    output.write(4)
    val byteString = output.toByteString()
    byteString should equal(ByteString(1, 2, 3, 4))
  }

  it should "return a ByteString of data that was written into it via write(Array[Byte])" in {
    val output = new ByteStringOutputStream()
    output.write(Array[Byte](1, 2, 3, 4))
    val byteString = output.toByteString()
    byteString should equal(ByteString(1, 2, 3, 4))
  }

  it should "return a ByteString of data that was written into it via write(Array[Byte], Int, Int)" in {
    val output = new ByteStringOutputStream()
    output.write(Array[Byte](1, 2, 3, 4), 1, 2)
    val byteString = output.toByteString()
    byteString should equal(ByteString(2, 3))
  }
}