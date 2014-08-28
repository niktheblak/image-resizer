package org.ntb.imageresizer.storage

import java.io.File

trait TempDirectoryStorageFile extends TempDirectoryProvider {
  def storageId: String

  def storageFile: File = {
    new File(tempDirectory, s"${storageId}_storage.bin")
  }
}
