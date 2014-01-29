package org.ntb.imageresizer.util

import java.net.URI
import java.io.File
import scala.concurrent.duration.Duration

object FileUtils {
  def getFileExtension(uri: URI): Option[String] = {
    val path = uri.getPath
    val index = path.lastIndexOf('.')
    if (index != -1 && index < path.length() - 1) {
      val extension = path.substring(index + 1).trim().toLowerCase
      Some(extension)
    } else {
      None
    }
  }

  def createTempFile(): File = {
    val file = File.createTempFile("FileUtils", ".tmp")
    file.deleteOnExit()
    file
  }

  def tempDir: File = {
    val tmpdir = System.getProperty("java.io.tmpdir")
    assert(tmpdir != null, "System property java.io.tmpdir is not set")
    new File(tmpdir)
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
}