package org.ntb.imageresizer.util

import java.net.URI
import java.io.{FileInputStream, File}
import concurrent.duration.Duration
import Loans.using

object FileUtils {
  def getFileExtension(uri: URI): Option[String] = {
    val path = uri.getPath
    val index = path.lastIndexOf('.')
    if (index != -1 && index < path.length() - 1) {
      val format = path.substring(index + 1).trim().toLowerCase
      Some(format)
    } else {
      None
    }
  }

  def read(file: File, amount: Int): Array[Byte] = {
    val data = new Array[Byte](amount)
    var bytesRead = 0
    using (new FileInputStream(file)) { input =>
      bytesRead = input.read(data)
    }
    data.slice(0, bytesRead)
  }
  
  def getFilePath(uri: URI): Option[String] = {
    val path = uri.getPath
    val index = path.lastIndexOf('/')
    if (index != -1 && index < path.length() - 1) {
      val filePath = path.substring(index + 1).trim()
      Some(filePath)
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