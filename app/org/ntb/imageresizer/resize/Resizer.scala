package org.ntb.imageresizer.resize

import java.io.{ File, FileOutputStream, InputStream, OutputStream }
import org.ntb.imageresizer.imageformat.ImageFormat
import org.ntb.imageresizer.util.Loans.using

trait Resizer extends ImgScalrResizer with JavaImageIOImageReader {
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

  def resizeImage(input: File, target: File, resolution: Resolution, format: ImageFormat) {
    using(new FileOutputStream(target)) { output ⇒
      usingImage(read(input)) { image ⇒
        resizeBufferedImage(image, output, resolution, format)
      }
    }
  }

  def resizeImage(input: InputStream, output: OutputStream, resolution: Resolution, format: ImageFormat) {
    usingImage(read(input)) { image ⇒
      resizeBufferedImage(image, output, resolution, format)
    }
  }
}