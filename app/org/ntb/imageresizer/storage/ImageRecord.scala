package org.ntb.imageresizer.storage

import akka.util.ByteString
import org.ntb.imageresizer.imageformat.ImageFormat

case class ImageRecord(key: String, size: Int, format: ImageFormat, flags: Int, data: ByteString)
