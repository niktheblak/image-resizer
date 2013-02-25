package org.ntb.imageresizer.resize

import org.ntb.imageresizer.imageformat.ImageFormat

import org.imgscalr.Scalr

import java.awt.image.BufferedImage
import java.io.OutputStream

import javax.imageio.ImageIO
import org.imgscalr.Scalr.Mode

trait ImgScalrResizer {
  def resizeBufferedImage(image: BufferedImage, output: OutputStream, size: Int, format: ImageFormat) {
    usingImage(Scalr.resize(image, size)) { scaled ⇒
      ImageIO.write(scaled, format.extension, output)
    }
  }

  def resizeBufferedImage(image: BufferedImage, output: OutputStream, width: Int, height: Int, format: ImageFormat) {
    usingImage(widthAndHeightResize(image, width, height)) { scaled ⇒
      ImageIO.write(scaled, format.extension, output)
    }
  }

  def widthAndHeightResize(image: BufferedImage, width: Int, height: Int): BufferedImage =
    if (width > 0 && height > 0)
      Scalr.resize(image, Mode.FIT_EXACT, width, height)
    else if (width <= 0)
      Scalr.resize(image, Mode.FIT_TO_HEIGHT, 0, height)
    else if (height <= 0)
      Scalr.resize(image, Mode.FIT_TO_WIDTH, width, 0)
    else
      throw new IllegalArgumentException("Width or height must be above zero")

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