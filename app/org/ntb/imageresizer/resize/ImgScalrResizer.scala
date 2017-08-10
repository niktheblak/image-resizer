package org.ntb.imageresizer.resize

import java.awt.image.BufferedImage
import java.io.OutputStream
import javax.imageio.ImageIO
import org.imgscalr.Scalr
import org.imgscalr.Scalr.Mode
import org.ntb.imageresizer.imageformat.ImageFormat

trait ImgScalrResizer extends BufferedImageLoan {
  def resizeBufferedImage(image: BufferedImage, output: OutputStream, size: Int, format: ImageFormat) {
    usingImage(Scalr.resize(image, size)) { scaled =>
      ImageIO.write(scaled, format.extension, output)
    }
  }

  def resizeBufferedImage(image: BufferedImage, output: OutputStream, resolution: Resolution, format: ImageFormat) {
    usingImage(resizeBasedOnWidthAndHeight(image, resolution)) { scaled =>
      ImageIO.write(scaled, format.extension, output)
    }
  }

  def resizeBasedOnWidthAndHeight(image: BufferedImage, resolution: Resolution): BufferedImage = {
    val Resolution(width, height) = resolution
    if (width > 0 && height > 0) fitExact(image, resolution)
    else if (width <= 0) fitToHeight(image, height)
    else if (height <= 0) fitToWidth(image, width)
    else throw new IllegalArgumentException("Width or height must be above zero")
  }

  def fitToWidth(image: BufferedImage, width: Int): BufferedImage =
    Scalr.resize(image, Mode.FIT_TO_WIDTH, width, 0)

  def fitToHeight(image: BufferedImage, height: Int): BufferedImage =
    Scalr.resize(image, Mode.FIT_TO_HEIGHT, 0, height)

  def fitExact(image: BufferedImage, resolution: Resolution): BufferedImage =
    Scalr.resize(image, Mode.FIT_EXACT, resolution.width, resolution.height)
}