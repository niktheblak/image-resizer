package org.ntb.imageresizer.actor

import com.google.common.hash.Hashing
import com.google.common.io.BaseEncoding
import org.ntb.imageresizer.imageformat.ImageFormat
import org.ntb.imageresizer.storage.FormatEncoder

trait KeyEncoder extends FormatEncoder {
  private val hashFunction = Hashing.goodFastHash(128)
  private val encoding = BaseEncoding.base64Url().omitPadding()

  def encodeKey(source: String, size: Int, format: ImageFormat): String = {
    val hasher = hashFunction.newHasher()
    hasher.putUnencodedChars(source)
    hasher.putInt(size)
    hasher.putByte(formatToByte(format))
    val hash = hasher.hash()
    encoding.encode(hash.asBytes())
  }
}
