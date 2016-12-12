package org.ntb.imageresizer.util

import java.io.File

import akka.util.ByteString
import com.google.common.io.Files

object FileUtils {
  def createTempFile(prefix: String = "FileUtils", suffix: String = ".tmp"): File = {
    val file = File.createTempFile(prefix, suffix)
    file.deleteOnExit()
    file
  }

  def toByteString(file: File): ByteString = {
    val builder = ByteString.newBuilder
    val output = builder.asOutputStream
    Files.copy(file, output)
    builder.result()
  }
}