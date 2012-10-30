package org.ntb.imageresizer

import java.io.InputStream
import java.io.OutputStream
import org.imgscalr.Scalr
import org.ntb.imageresizer.imageformat.ImageFormat
import org.ntb.imageresizer.io.ByteStringInputStream
import org.ntb.imageresizer.io.ByteStringOutputStream
import org.ntb.imageresizer.util.Loans.using
import akka.util.ByteString
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import java.io.FileOutputStream

package object resize {
  def resizeImage(imageData: ByteString, size: Int, format: ImageFormat): ByteString =
    using(new ByteStringInputStream(imageData)) { input =>
      using(new ByteStringOutputStream()) { output =>
        resizeImage(input, output, size, format)
        output.toByteString()
      }
    }

  def resizeImage(input: InputStream, output: OutputStream, size: Int, format: ImageFormat) {
    resizeImageFrom(() => ImageIO.read(input), output, size, format)
  }

  def resizeImage(input: File, output: OutputStream, size: Int, format: ImageFormat) {
    resizeImageFrom(() => ImageIO.read(input), output, size, format)
  }

  def resizeImage(input: File, target: File, size: Int, format: ImageFormat) {
    using(new FileOutputStream(target)) { output =>
      resizeImageFrom(() => ImageIO.read(input), output, size, format)
    }
  }

  def resizeImage(input: URL, output: OutputStream, size: Int, format: ImageFormat) {
    resizeImageFrom(() => ImageIO.read(input), output, size, format)
  }

  def resizeImageFrom(inputProvider: () => BufferedImage, output: OutputStream, size: Int, format: ImageFormat) {
    val image = inputProvider()
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

  class UnsupportedImageFormatException(message: String, cause: Throwable) extends Exception(message, cause) {
    def this(message: String) { this(message, null) }
    def this(cause: Throwable) { this(null, cause) }
    def this() { this(null, null) }
  }
}