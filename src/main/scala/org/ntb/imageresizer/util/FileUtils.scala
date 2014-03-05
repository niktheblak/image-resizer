package org.ntb.imageresizer.util

import java.io.File
import scala.concurrent.duration.Duration

object FileUtils {
  def createTempFile(): File = {
    val file = File.createTempFile("FileUtils", ".tmp")
    file.deleteOnExit()
    file
  }

  def hasExpired(file: File, maxAge: Duration): Boolean = {
    val lastModified = file.lastModified
    if (maxAge.isFinite() && lastModified > 0L)
      lastModified + maxAge.toMillis < System.currentTimeMillis()
    else false
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
}