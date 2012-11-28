package org.ntb.imageresizer.util

import java.net.URI
import java.io.File

object FilePathUtils {
  def getFileExtension(uri: URI): Option[String] = {
    val path = uri.getPath()
    val index = path.lastIndexOf('.')
    if (index != -1 && index < path.length() - 1) {
      val format = path.substring(index + 1).trim().toLowerCase()
      Some(format)
    } else {
      None
    }
  }
  
  def getFilePath(uri: URI): Option[String] = {
    val path = uri.getPath()
    val index = path.lastIndexOf('/')
    if (index != -1 && index < path.length() - 1) {
      val filePath = path.substring(index + 1).trim()
      Some(filePath)
    } else {
      None
    }
  }
  
  def createTempFile(): File = {
    val file = File.createTempFile("FilePathUtils", ".tmp")
    file.deleteOnExit()
    file
  }
}