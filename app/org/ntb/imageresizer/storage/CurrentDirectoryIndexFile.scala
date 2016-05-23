package org.ntb.imageresizer.storage

import java.io.File

trait CurrentDirectoryIndexFile {
  def indexFile: File = {
    new File("index.bin")
  }
}
