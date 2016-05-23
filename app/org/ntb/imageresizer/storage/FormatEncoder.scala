package org.ntb.imageresizer.storage

import org.ntb.imageresizer.imageformat.{ GIF, PNG, JPEG, ImageFormat }

trait FormatEncoder {
  def encode(format: ImageFormat): Byte = format match {
    case JPEG ⇒ 0.toByte
    case PNG ⇒ 1.toByte
    case GIF ⇒ 2.toByte
    case _ ⇒ throw new IllegalArgumentException("Invalid format: " + format)
  }

  def decode(b: Byte): ImageFormat = b match {
    case 0 ⇒ JPEG
    case 1 ⇒ PNG
    case 2 ⇒ GIF
    case n ⇒ throw new IllegalArgumentException("Invalid format byte: " + n)
  }
}
