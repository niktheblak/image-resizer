package org.ntb.imageresizer.storage

import org.ntb.imageresizer.imageformat.ImageFormat

case class ImageKey(key: String, size: Int, format: ImageFormat) {
  override def toString: String = {
    s"Image(Key: $key, size: $size, format: $format)"
  }
}
