package org.ntb.imageresizer.util

import com.google.common.hash.{HashFunction, Hashing}

trait DefaultHasher {
  val hashFunction: HashFunction = Hashing.md5()

  def hashBytes(input: Array[Byte]): String = hashFunction.hashBytes(input).toString

  def hashString(input: String): String = hashFunction.hashUnencodedChars(input).toString
}
