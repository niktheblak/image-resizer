package org.ntb.imageresizer.cache

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

import org.ntb.imageresizer.io.ByteStringIO._
import org.ntb.imageresizer.io.ByteStringInputStream
import org.ntb.imageresizer.io.ByteStringOutputStream
import org.ntb.imageresizer.util.Loans.using

import com.google.common.io.ByteStreams

import akka.util.ByteString

class FileCache[A](cachePathProvider: A => String) extends Cache[A, ByteString] {
  def put(key: A, value: ByteString) {
    val fileName = cachePathProvider(key)
    val file = new File(fileName)
    write(file, value)
  }

  def get(key: A): Option[ByteString] = {
    val fileName = cachePathProvider(key)
    val file = new File(fileName)
    if (file.exists()) {
      Some(read(file))
    } else {
      None
    }
  }
  
  def get(key: A, loader: () => ByteString): ByteString = {
    val fileName = cachePathProvider(key)
    val file = new File(fileName)
    if (file.exists()) {
      read(file)
    } else {
      val content = loader()
      write(file, content)
      content
    }
  }
}
