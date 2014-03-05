package org.ntb.imageresizer.util

import java.io.File
import org.mockito.Mockito._
import org.scalatest.Matchers
import org.scalatest.WordSpec
import org.scalatest.mock.MockitoSugar
import scala.concurrent.duration._

class FileUtilsTest extends WordSpec with Matchers with MockitoSugar {
  import scala.language.postfixOps

  "hasExpired" should {
    "return true if file has been expired" in {
      val expireTime = currentTimeMinus(2 minutes)
      val maxAge = 1 minutes
      val file = mock[File]
      when(file.lastModified()).thenReturn(expireTime)
      val expired = FileUtils.hasExpired(file, maxAge)
      expired should equal (true)
    }

    "return false when file expiration cannot be determined" in {
      val maxAge = 1 minutes
      val file = mock[File]
      when(file.lastModified()).thenReturn(-1)
      val expired = FileUtils.hasExpired(file, maxAge)
      expired should equal (false)
    }
  }

  "deleteIfExpired" should {
    "delete the file if it has been expired" in {
      val expireTime = currentTimeMinus(2 minutes)
      val maxAge = 1 minutes
      val file = mock[File]
      when(file.lastModified()).thenReturn(expireTime)
      FileUtils.deleteIfExpired(file, maxAge)
      verify(file).delete()
    }

    "not delete the file if it has not been expired" in {
      val expireTime = currentTimeMinus(1 minutes)
      val maxAge = 2 minutes
      val file = mock[File]
      when(file.lastModified()).thenReturn(expireTime)
      FileUtils.deleteIfExpired(file, maxAge)
      verify(file, never()).delete()
    }
  }

  def currentTimeMinus(d: Duration): Long = {
    require(d.isFinite())
    val t = System.currentTimeMillis() - d.toMillis
    assume(t > 0)
    t
  }
}