package org.ntb.imageresizer.cache

import scala.language.postfixOps
import scala.concurrent.duration._
import java.io.File
import com.google.common.base.Strings._
import org.apache.commons.codec.digest.DigestUtils._
import org.ntb.imageresizer.util.FileUtils._
import org.ntb.imageresizer.util.FileUtils

trait TempFileCache[A] extends FileCache[A] {
  val cachePath: String
  override val cacheFileProvider: A ⇒ File = tempDirCacheFileProvider
  lazy override val maxAge = 24 hours

  def tempDirCacheFileProvider(key: A): File = {
    require(key != null, "Argument key cannot be null")
    require(!isNullOrEmpty(key.toString), "toString() method of argument key must return a nonempty string")
    val dir = cacheDirectory()
    val fileName = md5Hex(key.toString)
    val file = new File(dir, fileName)
    deleteIfExpired(file, maxAge)
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
