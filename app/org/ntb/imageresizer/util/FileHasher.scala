package org.ntb.imageresizer.util

import java.io.File

import com.google.common.hash.{ HashFunction, Hashing }
import com.google.common.io.Files

trait FileHasher {
  val hashFunction: HashFunction = Hashing.murmur3_128()

  def hash(file: File): String =
    Files.hash(file, hashFunction).toString
}
