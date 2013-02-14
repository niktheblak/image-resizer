package org.ntb.imageresizer.cache

import java.util.concurrent.Callable
import com.google.common.cache.{ Cache ⇒ GCache }
import org.apache.http.HttpException
import org.ntb.imageresizer.resize.UnsupportedImageFormatException
import java.io.IOException

class GuavaMemoryCache[A, B](guavaCache: GCache[A, B]) extends MemoryCache[A, B] {
  override def put(key: A, value: B) {
    guavaCache.put(key, value)
  }
  
  override def get(key: A): Option[B] = {
    val value = guavaCache.getIfPresent(key)
    if (value != null) Some(value) else None
  }
  
  override def get(key: A, loader: () ⇒ B): B = {
    guavaCache.get(key, new Callable[B] {
      @throws(classOf[HttpException])
      @throws(classOf[UnsupportedImageFormatException])
      @throws(classOf[IOException])
      override def call(): B = loader()
    })
  }
  
  override def cleanUp() {
    guavaCache.cleanUp()
  }
}