package org.ntb.imageresizer.resize

case class Resolution(width: Int, height: Int) {
  require(width >= 0 && height >= 0, "Width and height must be zero or greater")

  override def toString: String = s"${width}x$height"
}
