package org.ntb.imageresizer.actor

import org.ntb.imageresizer.imageformat.ImageFormat
import org.ntb.imageresizer.storage.FormatEncoder
import org.ntb.imageresizer.util.DefaultHasher

trait KeyEncoder extends FormatEncoder with DefaultHasher {
  def encodeKey(source: String, size: Int, format: ImageFormat): String = {
    val hasher = hashFunction.newHasher()
    hasher.putUnencodedChars(source)
    hasher.putInt(size)
    hasher.putByte(formatToByte(format))
    hasher.hash().toString
  }
}
