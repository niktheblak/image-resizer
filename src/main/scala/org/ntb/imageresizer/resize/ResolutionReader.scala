package org.ntb.imageresizer.resize

import java.awt.image.BufferedImage
import java.io.{ InputStream, File }
import java.net.URL

import akka.util.ByteString

trait ResolutionReader extends JavaImageIOImageReader with BufferedImageLoan {
  def readResolution(input: InputStream): Resolution =
    usingImage(read(input))(getResolution)

  def readResolution(input: File): Resolution =
    usingImage(read(input))(getResolution)

  def readResolution(input: URL): Resolution =
    usingImage(read(input))(getResolution)

  def readResolution(input: ByteString): Resolution =
    usingImage(read(input))(getResolution)

  def getResolution(image: BufferedImage): Resolution =
    Resolution(image.getWidth, image.getHeight)
}
