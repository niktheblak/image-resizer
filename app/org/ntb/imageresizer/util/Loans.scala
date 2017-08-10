package org.ntb.imageresizer.util

import java.io.Closeable

object Loans {
  def using[T <: Closeable, R](c: T)(action: T => R): R = {
    try {
      action(c)
    } finally {
      if (c != null) {
        c.close()
      }
    }
  }
}