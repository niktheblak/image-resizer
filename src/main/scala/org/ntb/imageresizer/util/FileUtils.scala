package org.ntb.imageresizer.util

import java.io.File

import akka.util.ByteString
import com.github.nscala_time.time.Imports._
import com.google.common.io.Files

object FileUtils {
  def createTempFile(prefix: String = "FileUtils", suffix: String = ".tmp"): File = {
    val file = File.createTempFile(prefix, suffix)
    file.deleteOnExit()
    file
  }

  def hasExpired(file: File, maxAge: Duration): Boolean = {
    file.lastModified match {
      case 0L ⇒ false
      case lastModified ⇒ (new DateTime(lastModified) + maxAge).isBeforeNow
    }
  }

  def deleteIfExpired(file: File, maxAge: Duration) {
    if (hasExpired(file, maxAge)) {
      file.delete()
    }
  }

  def listFiles(dir: File): Seq[File] = {
    val files = dir.listFiles()
    if (files != null) files.toSeq
    else Seq.empty
  }

  def toByteString(file: File): ByteString = {
    val builder = ByteString.newBuilder
    val output = builder.asOutputStream
    Files.copy(file, output)
    builder.result()
  }
}