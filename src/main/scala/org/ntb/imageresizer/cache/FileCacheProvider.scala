package org.ntb.imageresizer.cache

import akka.util.ByteString
import java.io.File

trait FileCacheProvider[A] {
  val cacheFileProvider: A => File
  val cache: Cache[A, ByteString] = new FileCache(cacheFileProvider)
}