package org.ntb.imageresizer

import org.junit.runner.RunWith
import org.ntb.imageresizer.io.ByteStringOutputStream
import org.specs2.mutable.Specification
import akka.util.ByteString
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ByteStringOutputStreamTest extends Specification {
  "ByteStringOutputStream" should {
    "return a ByteString of data that was written into it via write(Int)" in {
      val output = new ByteStringOutputStream()
      output.write(1)
      output.write(2)
      output.write(3)
      output.write(4)
      val byteString = output.toByteString()
      byteString must_== (ByteString(1, 2, 3, 4))
    }

    "return a ByteString of data that was written into it via write(Array[Byte])" in {
      val output = new ByteStringOutputStream()
      output.write(Array[Byte](1, 2, 3, 4))
      val byteString = output.toByteString()
      byteString must_== (ByteString(1, 2, 3, 4))
    }

    "return a ByteString of data that was written into it via write(Array[Byte], Int, Int)" in {
      val output = new ByteStringOutputStream()
      output.write(Array[Byte](1, 2, 3, 4), 1, 2)
      val byteString = output.toByteString()
      byteString must_== (ByteString(2, 3))
    }
  }
}