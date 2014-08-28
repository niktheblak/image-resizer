package org.ntb.imageresizer.storage

import java.io._

import org.ntb.imageresizer.util.Loans

import scala.collection.mutable

trait IndexStore extends FlatFileImageStore {
  def saveIndex(index: mutable.Map[ImageKey, FilePosition], file: File) {
    Loans.using(new DataOutputStream(new FileOutputStream(file))) { output ⇒
      output.writeInt(index.size)
      index foreach {
        case (k, v) ⇒
          output.writeUTF(k.key)
          output.writeInt(k.size)
          output.writeByte(formatToByte(k.format))
          output.writeUTF(v.storage.getPath)
          output.writeLong(v.offset)
      }
    }
  }

  def loadIndex(index: mutable.Map[ImageKey, FilePosition], file: File) {
    index.clear()
    if (file.exists() && file.length() > 0) {
      Loans.using(new DataInputStream(new FileInputStream(file))) { input ⇒
        val n = input.readInt()
        for (i ← 0 until n) {
          val key = input.readUTF()
          val size = input.readInt()
          val format = byteToFormat(input.readByte())
          val path = input.readUTF()
          val offset = input.readLong()
          index.put(ImageKey(key, size, format), FilePosition(new File(path), offset))
        }
      }
    }
  }
}
