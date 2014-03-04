package org.ntb.imageresizer.actor

import org.ntb.imageresizer.imageformat.ImageFormat
import java.net.URI

case class Key(uri: URI, size: Int, format: ImageFormat)
