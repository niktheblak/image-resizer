package controllers

import java.io.File

import com.google.common.hash.Hashing
import com.google.common.io.Files

trait FileHasher {
  val hashFunction = Hashing.goodFastHash(128)

  def hash(file: File): String = {
    val hashCode = Files.hash(file, hashFunction)
    hashCode.toString
  }
}
