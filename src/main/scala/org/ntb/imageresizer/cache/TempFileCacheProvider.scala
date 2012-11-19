package org.ntb.imageresizer.cache

import org.ntb.imageresizer.util.FilePathUtils.isNullOrEmpty
import akka.util.ByteString
import java.io.File
import org.apache.commons.codec.digest.DigestUtils.md5Hex

trait TempFileCacheProvider[A] extends FileCacheProvider[A] {
  def cachePath: String
    
  def tempDirCacheFileProvider(key: A): File = {
    require(key != null, "Argument key cannot be null")
    require(!isNullOrEmpty(key.toString), "toString() method of argument key must return a nonempty string")
    val dir = cacheDirectory()
    val fileName = md5Hex(key.toString())
    new File(dir, fileName)
  }
  
  def clearCacheDirectory() {
    val dir = cacheDirectory()
    dir.listFiles().foreach(_.delete())
  }
  
  def cacheDirectory(): File = {
    val tmpdirProperty = System.getProperty("java.io.tmpdir")
    assert(tmpdirProperty != null, "Environment variable java.io.tmpdir is not set")
    val cacheDir = new File(tmpdirProperty, cachePath)
    assert(!cacheDir.isFile(), "File with path %s already exists".format(cacheDir.getAbsolutePath()))
    if (!cacheDir.exists()) cacheDir.mkdir()
    cacheDir
  }
  
  override val cacheFileProvider: A => File = tempDirCacheFileProvider
}