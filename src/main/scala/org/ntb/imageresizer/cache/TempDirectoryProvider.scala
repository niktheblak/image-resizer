package org.ntb.imageresizer.cache

import java.io.File

trait TempDirectoryProvider {
  def tempDirectory: File = {
    val systemTempDir = System.getProperty("java.io.tmpdir")
    assert(systemTempDir != null, "System property java.io.tmpdir is not set")
    new File(systemTempDir)
  }
}
