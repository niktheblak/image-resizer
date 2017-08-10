package org.ntb.imageresizer.resize

import java.awt.image.BufferedImage

trait BufferedImageLoan {
  def usingImage[R](c: BufferedImage)(action: BufferedImage => R): R = {
    try {
      action(c)
    } finally {
      if (c != null) {
        c.flush()
      }
    }
  }
}
