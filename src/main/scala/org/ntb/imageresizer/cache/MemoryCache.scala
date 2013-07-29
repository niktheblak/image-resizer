package org.ntb.imageresizer.cache

trait MemoryCache[A, B] extends Cache[A, B] {
  def cleanUp()
}