package org.ntb.imageresizer.resize

import org.ntb.imageresizer.io.ByteStringInputStream
import org.ntb.imageresizer.util.Loans.using

import akka.util.ByteString

import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.net.URL

import javax.imageio.ImageIO

trait JavaImageIOImageReader {
  def read(input: InputStream): BufferedImage = checkImage(ImageIO.read(input))
  
  def read(input: File): BufferedImage = checkImage(ImageIO.read(input))
  
  def read(input: URL): BufferedImage = checkImage(ImageIO.read(input))
  
  def read(imageData: ByteString): BufferedImage = {
    using(new ByteStringInputStream(imageData)) { input ⇒
      checkImage(ImageIO.read(input))
    }
  }
  
  def checkImage(image: BufferedImage): BufferedImage = {
    if (image == null) {
      throw new UnsupportedImageFormatException("Failed to decode image, format probably not supported by " + classOf[ImageIO].getCanonicalName())
    }
    image
  }
}