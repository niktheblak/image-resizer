package org.ntb.imageresizer.cache

import org.ntb.imageresizer.util.FileUtils.deleteIfExpired
import org.ntb.imageresizer.io.ByteStringIO._
import akka.util.ByteString
import scala.concurrent.duration._
import java.io.File

trait FileCache[A] extends Cache[A, ByteString] {
  val cacheFileProvider: A ⇒ File
  val maxAge: Duration

  def put(key: A, value: ByteString) {
    val file = cacheFileProvider(key)
    write(file, value)
  }

  def get(key: A): Option[ByteString] = {
    val file = cacheFileProvider(key)
    deleteIfExpired(file, maxAge)
    if (file.exists()) {
      Some(read(file))
    } else {
      None
    }
  }

  def get(key: A, loader: () ⇒ ByteString): ByteString = {
    val file = cacheFileProvider(key)
    deleteIfExpired(file, maxAge)
    if (file.exists()) {
      read(file)
    } else {
      val content = loader()
      write(file, content)
      content
    }
  }
}
