package org.ntb.imageresizer.resize

class UnsupportedImageFormatException(message: String, cause: Throwable) extends ImageResizerException(message, cause) {
  def this(message: String) { this(message, null) }
  def this() { this(null) }
}