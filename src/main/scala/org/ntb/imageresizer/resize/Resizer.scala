package org.ntb.imageresizer.resize

import org.ntb.imageresizer.imageformat.ImageFormat
import org.ntb.imageresizer.util.Loans.using

import java.io.File
import java.io.FileOutputStream

object Resizer extends ImgScalrResizer with JavaImageIOImageReader {
  def resizeImage(input: File, target: File, size: Int, format: ImageFormat) {
    using(new FileOutputStream(target)) { output ⇒
      usingImage(read(input)) { image ⇒
        resizeBufferedImage(image, output, size, format)
      }
    }
  }

  def resizeImage(input: File, target: File, width: Int, height: Int, format: ImageFormat) {
    using(new FileOutputStream(target)) { output ⇒
      usingImage(read(input)) { image ⇒
        resizeBufferedImage(image, output, width, height, format)
      }
    }
  }
}