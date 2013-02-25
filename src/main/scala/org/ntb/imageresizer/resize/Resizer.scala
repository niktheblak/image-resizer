package org.ntb.imageresizer.resize

import org.ntb.imageresizer.imageformat.ImageFormat
import org.ntb.imageresizer.util.Loans.using

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URL

object Resizer extends ImgScalrResizer with JavaImageIOImageReader {
  def resizeImage(input: File, target: File, size: Int, format: ImageFormat) {
    using(new FileOutputStream(target)) { output ⇒
      usingImage(read(input)) { image ⇒
        resizeImageFrom(image, output, size, format)
      }
    }
  }

  def resizeImage(input: URL, output: OutputStream, size: Int, format: ImageFormat) {
    usingImage(read(input)) { image ⇒
      resizeImageFrom(image, output, size, format)
    }
  }
}