package org.ntb.imageresizer.resize

import org.ntb.imageresizer.imageformat.ImageFormat

import org.imgscalr.Scalr

import java.awt.image.BufferedImage
import java.io.{Closeable, OutputStream}

import javax.imageio.ImageIO
import java.awt.Color

trait ImgScalrResizer {
  def resizeImageFrom(image: BufferedImage, output: OutputStream, size: Int, format: ImageFormat) {
    usingImage(Scalr.resize(image, size)) { scaled ⇒
      ImageIO.write(scaled, format.extension, output)
    }
  }

  def usingImage[R](c: BufferedImage)(action: BufferedImage ⇒ R): R = {
    try {
      action(c)
    } finally {
      if (c != null) {
        c.flush()
      }
    }
  }
}