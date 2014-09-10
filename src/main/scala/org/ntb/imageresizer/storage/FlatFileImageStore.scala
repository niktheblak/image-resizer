package org.ntb.imageresizer.storage

import java.io.RandomAccessFile

import akka.util.ByteString
import org.ntb.imageresizer.imageformat._

trait FlatFileImageStore extends ImageRecordIO {
  def writeImage(storage: RandomAccessFile, key: String, size: Int, format: ImageFormat, data: ByteString): (Long, Long) = {
    val offset = storage.length()
    storage.seek(offset)
    writeImageRecord(ImageRecord(key, size, format, 0, data), storage)
    val storageSize = storage.getFilePointer - offset
    (offset, storageSize)
  }

  def readImage(storage: RandomAccessFile, offset: Long): ByteString = {
    require(offset < storage.length())
    storage.seek(offset)
    val image = readImageRecord(storage)
    image.data
  }
}
