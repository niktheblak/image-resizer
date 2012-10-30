package org.ntb.imageresizer.cache

import com.google.common.cache.CacheBuilder
import com.google.common.cache.{ Cache => GCache }

trait GuavaMemoryCacheProvider[Key <: Object, Value <: Object] extends MemoryCacheProvider[Key, Value] {
  override val maxCacheSize = 10L * 1024L * 1024L
  override val cache: MemoryCache[Key, Value] = new GuavaMemoryCache(CacheBuilder.newBuilder().maximumSize(maxCacheSize).build())
}