package org.ntb.imageresizer.storage

import java.io.File

trait TempDirectoryIndexFile extends TempDirectoryProvider {
  def indexFile: File = {
    new File(tempDirectory, "index.bin")
  }
}
