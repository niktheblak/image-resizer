package org.ntb.imageresizer.util

import java.io.File
import org.mockito.Mockito.{ never, verify, when }
import org.scalatest.Matchers
import org.scalatest.WordSpec
import org.scalatest.mock.MockitoSugar
import com.github.nscala_time.time.Imports._

class FileUtilsTest extends WordSpec with Matchers with MockitoSugar {
  "hasExpired" should {
    "return true if file has been expired" in {
      val expireTime = DateTime.now - 2.minutes
      val maxAge = 1.minutes
      val file = mock[File]
      when(file.lastModified()).thenReturn(expireTime.getMillis)
      val expired = FileUtils.hasExpired(file, maxAge)
      expired should equal (true)
    }

    "return false when file expiration cannot be determined" in {
      val maxAge = 1.minutes
      val file = mock[File]
      when(file.lastModified()).thenReturn(0L)
      val expired = FileUtils.hasExpired(file, maxAge)
      expired should equal (false)
    }
  }

  "deleteIfExpired" should {
    "delete the file if it has been expired" in {
      val expireTime = DateTime.now - 2.minutes
      val maxAge = 1.minutes
      val file = mock[File]
      when(file.lastModified()).thenReturn(expireTime.getMillis)
      FileUtils.deleteIfExpired(file, maxAge)
      verify(file).delete()
    }

    "not delete the file if it has not been expired" in {
      val expireTime = DateTime.now - 1.minutes
      val maxAge = 2.minutes
      val file = mock[File]
      when(file.lastModified()).thenReturn(expireTime.getMillis)
      FileUtils.deleteIfExpired(file, maxAge)
      verify(file, never()).delete()
    }
  }
}