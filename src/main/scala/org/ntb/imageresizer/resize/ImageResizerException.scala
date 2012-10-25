package org.ntb.imageresizer.resize

class ImageResizerException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this(message: String) { this(message, null) }
  def this() { this(null) }
}