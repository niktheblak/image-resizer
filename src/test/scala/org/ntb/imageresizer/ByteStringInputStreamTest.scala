package org.ntb.imageresizer

import akka.util.ByteString
import org.junit.runner.RunWith
import org.ntb.imageresizer.io.ByteStringInputStream
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ByteStringInputStreamTest extends Specification {
  "ByteStringInputStream" should {
    "expose complete ByteString contents via read() method" in {
      val data = ByteString(1, 2)
      val input = new ByteStringInputStream(data)
      val b0 = input.read()
      val b1 = input.read()
      b0 must_== 1
      b1 must_== 2
    }

    "return -1 after stream has been read" in {
      val data = ByteString(1, 2)
      val input = new ByteStringInputStream(data)
      input.read()
      input.read()
      val end = input.read()
      end must_== -1
    }

    "expose complete ByteString contents via read(Array[Byte]) method" in {
      val data = ByteString(1, 2, 3, 4)
      val input = new ByteStringInputStream(data)
      val buf = new Array[Byte](4)
      input.read(buf)
      buf must_== (Array[Byte](1, 2, 3, 4))
    }

    "expose complete ByteString contents via read(Array[Byte], Int, Int) method" in {
      val data = ByteString(1, 2, 3, 4)
      val input = new ByteStringInputStream(data)
      val buf = new Array[Byte](4)
      input.read(buf, 0, 4)
      buf must_== (Array[Byte](1, 2, 3, 4))
    }

    "return number of bytes read" in {
      val data = ByteString(1, 2)
      val input = new ByteStringInputStream(data)
      val buf = new Array[Byte](4)
      val bytesRead = input.read(buf)
      bytesRead must_== 2
    }

    "throw IndexOutOfBoundsException if asked to read from index that does not exist" in {
      val data = ByteString(1, 2)
      val input = new ByteStringInputStream(data)
      val buf = new Array[Byte](4)
      input.read(buf, 3, 2) must throwA [IndexOutOfBoundsException]
    }
  }
}