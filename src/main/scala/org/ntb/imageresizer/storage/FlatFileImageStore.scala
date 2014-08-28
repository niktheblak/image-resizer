package org.ntb.imageresizer.storage

import java.io.{ File, RandomAccessFile }

import akka.util.ByteString
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.util.Loans

trait FlatFileImageStore extends ImageRecordIO {
  def writeImage(storage: File, key: String, size: Int, format: ImageFormat, data: ByteString): (Long, Long) = {
    Loans.using(new RandomAccessFile(storage, "rw")) { output ⇒
      val offset = output.length()
      output.seek(offset)
      writeImageRecord(ImageRecord(key, size, format, 0, data), output)
      val storageSize = output.getFilePointer - offset
      (offset, storageSize)
    }
  }

  def readImage(storage: File, offset: Long): ByteString = {
    require(offset < storage.length())
    Loans.using(new RandomAccessFile(storage, "r")) { input ⇒
      input.seek(offset)
      val image = readImageRecord(input)
      image.data
    }
  }
}
