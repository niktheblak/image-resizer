package org.ntb.imageresizer.storage

import java.io.File

trait CurrentDirectoryStorageFile {
  def storageId: String

  def storageFile: File = {
    new File(s"${storageId}_storage.bin")
  }
}
