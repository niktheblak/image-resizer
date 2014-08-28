package org.ntb.imageresizer.cache

import java.io.File

trait TempDirectoryIndexFile extends TempDirectoryProvider {
  def indexFile: File = {
    new File(tempDirectory, "index.bin")
  }
}
