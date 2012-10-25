package org.ntb.imageresizer.cache

import org.ntb.imageresizer.util.FilePathUtils.isNullOrEmpty
import akka.util.ByteString
import java.io.File
import org.apache.commons.codec.digest.DigestUtils.md5Hex

trait TempFileCacher[A] extends FileCacher[A] {
  val appPrefix = "image-resizer"
    
  def tempDirCachePathProviderProvider(key: A): String = {
    require(key != null, "Argument key cannot be null")
    require(!isNullOrEmpty(key.toString), "toString() method of argument key must return a nonempty string")
    val dir = cacheDirectory()
    val fileName = md5Hex(key.toString())
    new File(dir, fileName).getAbsolutePath()
  }
  
  def clearCacheDirectory() {
    val dir = cacheDirectory()
    dir.listFiles().foreach(_.delete())
  }
  
  def cacheDirectory(): File = {
    val tmpdirProperty = System.getProperty("java.io.tmpdir")
    assert(tmpdirProperty != null, "Environment variable java.io.tmpdir is not set")
    val applicationTempDir = new File(tmpdirProperty, appPrefix)
    assert(!applicationTempDir.isFile(), "File with path %s already exists".format(applicationTempDir.getAbsolutePath()))
    if (!applicationTempDir.exists()) applicationTempDir.mkdir()
    applicationTempDir
  }
  
  override val cachePathProvider: A => String = tempDirCachePathProviderProvider
}