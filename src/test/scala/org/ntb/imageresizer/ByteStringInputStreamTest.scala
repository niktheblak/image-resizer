package org.ntb.imageresizer

import akka.util.ByteString
import org.ntb.imageresizer.io.ByteStringInputStream
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

class ByteStringInputStreamTest extends FlatSpec with ShouldMatchers {
  "ByteStringInputStream" should "expose complete ByteString contents via read() method" in {
    val data = ByteString(1, 2)
    val input = new ByteStringInputStream(data)
    val b0 = input.read()
    val b1 = input.read()
    b0 should equal(1)
    b1 should equal(2)
  }

  it should "return -1 after stream has been read" in {
    val data = ByteString(1, 2)
    val input = new ByteStringInputStream(data)
    input.read()
    input.read()
    val end = input.read()
    end should equal(-1)
  }

  it should "expose complete ByteString contents via read(Array[Byte]) method" in {
    val data = ByteString(1, 2, 3, 4)
    val input = new ByteStringInputStream(data)
    val buf = new Array[Byte](4)
    input.read(buf)
    buf should equal(Array[Byte](1, 2, 3, 4))
  }

  it should "expose complete ByteString contents via read(Array[Byte], Int, Int) method" in {
    val data = ByteString(1, 2, 3, 4)
    val input = new ByteStringInputStream(data)
    val buf = new Array[Byte](4)
    input.read(buf, 0, 4)
    buf should equal(Array[Byte](1, 2, 3, 4))
  }

  it should "return number of bytes read" in {
    val data = ByteString(1, 2)
    val input = new ByteStringInputStream(data)
    val buf = new Array[Byte](4)
    val bytesRead = input.read(buf)
    bytesRead should equal(2)
  }

  it should "throw IndexOutOfBoundsException if asked to read from index that does not exist" in {
    val data = ByteString(1, 2)
    val input = new ByteStringInputStream(data)
    val buf = new Array[Byte](4)
    evaluating { input.read(buf, 3, 2) } should produce[IndexOutOfBoundsException]
  }
}