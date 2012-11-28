package org.ntb.imageresizer.resize

import org.ntb.imageresizer.imageformat.ImageFormat
import org.ntb.imageresizer.io.ByteStringInputStream
import org.ntb.imageresizer.io.ByteStringOutputStream
import org.ntb.imageresizer.util.Loans.using

import org.ntb.imageresizer.imageformat.ImageFormat
import org.ntb.imageresizer.io.ByteStringInputStream
import org.ntb.imageresizer.io.ByteStringOutputStream
import org.ntb.imageresizer.util.Loans.using

import akka.util.ByteString

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL

object Resizer extends ImgScalrResizer with JavaImageIOImageReader {
  def resizeImage(imageData: ByteString, size: Int, format: ImageFormat): ByteString =
    using(new ByteStringInputStream(imageData)) { input =>
      using(new ByteStringOutputStream()) { output =>
        resizeImage(input, output, size, format)
        output.toByteString()
      }
    }

  def resizeImage(input: InputStream, output: OutputStream, size: Int, format: ImageFormat) {
    resizeImageFrom(read(input), output, size, format)
  }

  def resizeImage(input: File, output: OutputStream, size: Int, format: ImageFormat) {
    resizeImageFrom(read(input), output, size, format)
  }

  def resizeImage(input: File, target: File, size: Int, format: ImageFormat) {
    using(new FileOutputStream(target)) { output =>
      resizeImageFrom(read(input), output, size, format)
    }
  }

  def resizeImage(input: URL, output: OutputStream, size: Int, format: ImageFormat) {
    resizeImageFrom(read(input), output, size, format)
  }
}