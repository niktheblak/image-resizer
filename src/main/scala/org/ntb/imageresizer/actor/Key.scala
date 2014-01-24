package org.ntb.imageresizer.actor

import org.ntb.imageresizer.imageformat.ImageFormat

case class Key(uri: String, size: Int, format: ImageFormat)
