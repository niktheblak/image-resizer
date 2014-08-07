package org.ntb.imageresizer.actor

import org.ntb.imageresizer.imageformat.ImageFormat

private[actor] case class Key(id: String, size: Int, format: ImageFormat)
