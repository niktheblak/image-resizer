package org.ntb.imageresizer.cache

import java.util.concurrent.Callable
import akka.util.ByteString

trait MemoryCache[A, B] extends Cache[A, B] {
  def cleanUp()
}