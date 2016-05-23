package org.ntb.imageresizer.io

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

import org.ntb.imageresizer.util.Loans.using

import com.google.common.io.{ Files, ByteStreams }

import akka.util.{ ByteStringBuilder, ByteString }

object ByteStringIO {
  def read(file: File): ByteString = {
    require(file.exists())
    val builder = new ByteStringBuilder
    using(new FileInputStream(file)) { input ⇒
      ByteStreams.copy(input, builder.asOutputStream)
      builder.result()
    }
  }

  def read(file: File, offset: Long, length: Long): ByteString = {
    require(file.exists())
    val builder = new ByteStringBuilder
    val byteSource = Files.asByteSource(file).slice(offset, length)
    using(byteSource.openStream()) { input ⇒
      ByteStreams.copy(input, builder.asOutputStream)
      builder.result()
    }
  }

  def write(file: File, content: ByteString) {
    val input = content.iterator.asInputStream
    using(new FileOutputStream(file)) { output ⇒
      ByteStreams.copy(input, output)
    }
  }

  def append(file: File, content: ByteString) {
    val input = content.iterator.asInputStream
    using(new FileOutputStream(file, true)) { output ⇒
      ByteStreams.copy(input, output)
    }
  }
}