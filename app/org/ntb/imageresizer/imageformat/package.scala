package org.ntb.imageresizer

import java.net.URI

import com.google.common.base.Strings

package object imageformat {
  abstract class ImageFormat {
    val extension: String
    val mimeType: String
    override def toString: String = extension
  }

  case object JPEG extends ImageFormat {
    val extension = "jpg"
    val mimeType = "image/jpeg"
  }

  case object PNG extends ImageFormat {
    val extension = "png"
    val mimeType = "image/png"
  }

  case object GIF extends ImageFormat {
    val extension = "gif"
    val mimeType = "image/gif"
  }

  val supportedImageFormats = Seq(JPEG, PNG, GIF)

  val defaultImageFormat = JPEG

  def parseRequestedImageFormat(requestedFormat: String): Option[ImageFormat] =
    requestedFormat.toLowerCase match {
      case "jpg" => Some(JPEG)
      case "jpeg" => Some(JPEG)
      case "png" => Some(PNG)
      case "gif" => Some(GIF)
      case _ => None
    }

  def parseImageFormatFromMimeType(mimeType: String): Option[ImageFormat] =
    mimeType match {
      case "image/jpeg" => Some(JPEG)
      case "image/png" => Some(PNG)
      case "image/gif" => Some(GIF)
      case _ => None
    }

  def parseImageFormatFromUri(uri: URI): Option[ImageFormat] =
    for {
      ext ← getFileExtension(uri)
      format ← parseRequestedImageFormat(ext)
    } yield format

  def getFileExtension(uri: URI): Option[String] = {
    val path = Strings.nullToEmpty(uri.getPath)
    val index = path.lastIndexOf('.')
    if (index != -1 && index < path.length() - 1) {
      val extension = path.substring(index + 1).trim().toLowerCase
      Some(extension)
    } else {
      None
    }
  }
}