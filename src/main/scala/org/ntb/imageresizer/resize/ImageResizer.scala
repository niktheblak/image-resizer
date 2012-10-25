package org.ntb.imageresizer.resize

import java.io.InputStream
import java.io.OutputStream

import org.imgscalr.Scalr
import org.ntb.imageresizer.imageformat.ImageFormat
import org.ntb.imageresizer.io.ByteStringInputStream
import org.ntb.imageresizer.io.ByteStringOutputStream
import org.ntb.imageresizer.util.Loans.using

import akka.util.ByteString
import javax.imageio.ImageIO

object ImageResizer {
  def resizeImage(imageData: ByteString, size: Int, format: ImageFormat): ByteString =
    using(new ByteStringInputStream(imageData)) { input =>
      using(new ByteStringOutputStream()) { output =>
        resize(input, output, size, format)
        output.toByteString()
      }
    }

  def resize(input: InputStream, output: OutputStream, size: Int, format: ImageFormat) {
    val image = ImageIO.read(input)
    if (image == null) {
      throw new UnsupportedImageFormatException("Failed to decode image, format probably not supported by " + classOf[ImageIO].getCanonicalName())
    }
    try {
      val scaled = Scalr.resize(image, size)
      try {
        ImageIO.write(scaled, format.extension, output)
      } finally {
        scaled.flush()
      }
    } finally {
      image.flush()
    }
  }
}