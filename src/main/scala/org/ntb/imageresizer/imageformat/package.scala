package org.ntb.imageresizer

import java.net.URI

package object imageformat {
  abstract class ImageFormat {
    val extension: String
    val mimeType: String
    override def toString() = extension
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
  
  def parseRequestedImageFormat(requestedFormat: String): Option[ImageFormat] = {
    val format = requestedFormat.toLowerCase()
    if (format == "jpg" || format == "jpeg") Some(JPEG)
    else if (format == "png") Some(PNG)
    else if (format == "gif") Some(GIF)
    else None
  }
  
  def parseImageFormatFromMimeType(mimeType: String): Option[ImageFormat] = {
    if (mimeType == "image/jpeg") Some(JPEG)
    else if (mimeType == "image/png") Some(PNG)
    else if (mimeType == "image/gif") Some(GIF)
    else None
  }
  
  def parseImageFormatFromUri(uri: URI): Option[ImageFormat] = {
    val fileName = uri.getPath().toLowerCase()
    if ((fileName endsWith "jpg") || (fileName endsWith "jpeg")) Some(JPEG)
    else if (fileName endsWith "png") Some(JPEG)
    else if (fileName endsWith "gif") Some(JPEG)
    else None
  }
}