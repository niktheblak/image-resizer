package org.ntb.imageresizer.cache

import akka.util.ByteString

trait FileCacher[A] {
  val cachePathProvider: A => String
  val cache: Cache[A, ByteString] = new FileCache(cachePathProvider)
}