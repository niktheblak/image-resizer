package org.ntb.imageresizer.storage

import akka.util.ByteString
import org.ntb.imageresizer.imageformat._

trait FlatFileImageStore extends ImageRecordIO { self: StorageFileProvider ⇒
  def writeImage(key: String, size: Int, format: ImageFormat, data: ByteString): (Long, Long) = {
    val offset = storage.length()
    storage.seek(offset)
    writeImageRecord(ImageRecord(key, size, format, 0, data), storage)
    val storageSize = storage.getFilePointer - offset
    (offset, storageSize)
  }

  def readImage(offset: Long): ByteString = {
    require(offset < storage.length())
    storage.seek(offset)
    val image = readImageRecord(storage)
    image.data
  }
}
