package org.ntb.imageresizer.cache

import com.google.common.base.Strings._
import com.google.common.hash.Hashing
import java.io.File
import org.ntb.imageresizer.util.FileUtils
import scala.concurrent.duration._

trait TempFileCache[A] extends FileCache[A] {
  def cachePath: String
  override val cacheFileProvider: A ⇒ File = tempDirCacheFileProvider
  override val maxAge = 24.hours
  val hashFunction = Hashing.md5()

  def tempDirCacheFileProvider(key: A): File = {
    require(key != null, "Argument key cannot be null")
    val keyString = key.toString
    require(!isNullOrEmpty(keyString), "toString() method of argument key must return a nonempty string")
    val dir = cacheDirectory()
    val fileName = hashFunction.hashUnencodedChars(keyString).toString
    val file = new File(dir, fileName)
    FileUtils.deleteIfExpired(file, maxAge)
    file
  }

  def clearCacheDirectory() {
    val dir = cacheDirectory()
    dir.listFiles().foreach(_.delete())
  }

  def cacheDirectory(): File = {
    val cacheDir = new File(FileUtils.tempDir, cachePath)
    assert(!cacheDir.isFile, "File with path %s already exists".format(cacheDir.getAbsolutePath))
    if (!cacheDir.exists()) cacheDir.mkdir()
    cacheDir
  }
}
