package org.ntb.imageresizer.cache

trait MemoryCacheProvider[Key, Value] {
  val maxCacheSize: Long
  val cache: MemoryCache[Key, Value]
}