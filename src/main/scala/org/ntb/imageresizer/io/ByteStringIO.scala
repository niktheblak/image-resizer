package org.ntb.imageresizer.io

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

import org.ntb.imageresizer.util.Loans.using

import com.google.common.io.ByteStreams

import akka.util.ByteString

object ByteStringIO {
  def read(file: File): ByteString = {
    require(file.exists())
    using(new FileInputStream(file)) { input =>
      using(new ByteStringOutputStream()) { output =>
        ByteStreams.copy(input, output)
        output.toByteString
      }
    }
  }

  def write(file: File, content: ByteString) {
    using(new ByteStringInputStream(content)) { input =>
      using(new FileOutputStream(file)) { output =>
        ByteStreams.copy(input, output)
      }
    }
  }
}