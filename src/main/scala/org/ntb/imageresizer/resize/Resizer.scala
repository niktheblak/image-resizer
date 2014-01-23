package org.ntb.imageresizer.resize

import java.io.{ File, FileOutputStream, InputStream, OutputStream }
import org.ntb.imageresizer.imageformat.ImageFormat
import org.ntb.imageresizer.util.Loans.using

object Resizer extends ImgScalrResizer with JavaImageIOImageReader {
  def resizeImage(input: File, target: File, size: Int, format: ImageFormat) {
    using(new FileOutputStream(target)) { output ⇒
      usingImage(read(input)) { image ⇒
        resizeBufferedImage(image, output, size, format)
      }
    }
  }

  def resizeImage(input: InputStream, output: OutputStream, size: Int, format: ImageFormat) {
    usingImage(read(input)) { image ⇒
      resizeBufferedImage(image, output, size, format)
    }
  }

  def resizeImage(input: File, target: File, width: Int, height: Int, format: ImageFormat) {
    using(new FileOutputStream(target)) { output ⇒
      usingImage(read(input)) { image ⇒
        resizeBufferedImage(image, output, width, height, format)
      }
    }
  }

  def resizeImage(input: InputStream, output: OutputStream, width: Int, height: Int, format: ImageFormat) {
    usingImage(read(input)) { image ⇒
      resizeBufferedImage(image, output, width, height, format)
    }
  }
}