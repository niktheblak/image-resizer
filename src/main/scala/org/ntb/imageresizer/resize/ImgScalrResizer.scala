package org.ntb.imageresizer.resize

import org.ntb.imageresizer.imageformat.ImageFormat

import org.imgscalr.Scalr

import java.awt.image.BufferedImage
import java.io.OutputStream

import javax.imageio.ImageIO

trait ImgScalrResizer {
  def resizeImageFrom(image: BufferedImage, output: OutputStream, size: Int, format: ImageFormat) {
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