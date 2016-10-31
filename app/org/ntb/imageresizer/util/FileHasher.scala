package org.ntb.imageresizer.util

import java.io.File

import com.google.common.hash.Hashing
import com.google.common.io.Files

trait FileHasher {
  val hashFunction = Hashing.murmur3_128()

  def hash(file: File): String =
    Files.hash(file, hashFunction).toString
}
