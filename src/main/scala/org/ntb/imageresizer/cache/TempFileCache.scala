package org.ntb.imageresizer.cache

import com.google.common.base.Strings._
import java.io.File
import org.ntb.imageresizer.util.DefaultHasher
import org.ntb.imageresizer.util.FileUtils._
import scala.concurrent.duration._

trait TempFileCache[A] extends FileCache[A] with DefaultHasher {
  def cachePath: String
  override val cacheFileProvider: A ⇒ File = tempDirCacheFileProvider
  override val maxAge = 24.hours

  def tempDirCacheFileProvider(key: A): File = {
    require(key != null, "Argument key cannot be null")
    val keyString = key.toString
    require(!isNullOrEmpty(keyString), "toString() method of argument key must return a nonempty string")
    val dir = cacheDirectory()
    val fileName = hashString(keyString).toString
    val file = new File(dir, fileName)
    deleteIfExpired(file, maxAge)
    file
  }

  def clearCacheDirectory() {
    val dir = cacheDirectory()
    listFiles(dir).foreach(_.delete())
  }

  def cacheDirectory(): File = {
    val cacheDir = new File(tempDir, cachePath)
    assert(!cacheDir.isFile, s"File with path ${cacheDir.getAbsolutePath} already exists")
    if (!cacheDir.exists()) cacheDir.mkdir()
    cacheDir
  }

  def tempDir: File = {
    val tmpdir = System.getProperty("java.io.tmpdir")
    assert(tmpdir != null, "System property java.io.tmpdir is not set")
    new File(tmpdir)
  }
}
