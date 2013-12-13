package org.ntb.imageresizer

import org.ntb.imageresizer.util.FileUtils
import org.scalatest.OptionValues._
import org.scalatest.WordSpec
import org.scalatest.Matchers
import java.net.URI
import java.io.File
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import scala.concurrent.duration._

class FileUtilsTest extends WordSpec with Matchers with MockitoSugar {
  import scala.language.postfixOps

  "getFileExtension" should {
    "return extension of a filename in HTTP URL" in {
      val uri = URI.create("http://www.server.com/logo.png")
      val path = FileUtils.getFileExtension(uri)
      path.value should equal ("png")
    }

    "return extension of a plain filename" in {
      val uri = URI.create("logo.png")
      val path = FileUtils.getFileExtension(uri)
      path.value should equal ("png")
    }

    "return file extension of a relative path" in {
      val uri = URI.create("/path/to/logo.png")
      val path = FileUtils.getFileExtension(uri)
      path.value should equal ("png")
    }

    "return extension of a filename with dots" in {
      val uri = URI.create("my.prefix.logo.png")
      val path = FileUtils.getFileExtension(uri)
      path.value should equal ("png")
    }

    "return Nothing if extension is not found from a file path" in {
      val uri = URI.create("/path/logo")
      val path = FileUtils.getFileExtension(uri)
      path should not be 'defined
    }

    "return Nothing if extension is not found from an HTTP URL" in {
      val uri = URI.create("http://www.server.com/picturedata?format=jpg")
      val path = FileUtils.getFileExtension(uri)
      path should not be 'defined
    }
  }

  "getFilePath" should {
    "return filename from an HTTP URL" in {
      val uri = URI.create("http://www.server.com/logo.png")
      val path = FileUtils.getFilePath(uri)
      path.value should equal ("logo.png")
    }

    "return filename from a relative path" in {
      val uri = URI.create("/path/to/logo.png")
      val path = FileUtils.getFilePath(uri)
      path.value should equal ("logo.png")
    }

    "return filename from HTTP URL with multiple path segments" in {
      val uri = URI.create("http://www.server.com/site/a/logo.png")
      val path = FileUtils.getFilePath(uri)
      path.value should equal ("logo.png")
    }

    "return Nothing if no path was found" in {
      val uri = URI.create("http://www.server.com/")
      val path = FileUtils.getFilePath(uri)
      path should not be 'defined
    }
  }

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