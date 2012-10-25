package org.ntb.imageresizer.cache

trait MemoryCacher[Key, Value] {
  val maxCacheSize: Long
  val cache: MemoryCache[Key, Value]
}