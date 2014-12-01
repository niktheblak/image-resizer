package org.ntb.imageresizer.storage

import java.io.RandomAccessFile

trait StorageFileProvider {
  def storage: RandomAccessFile
}
