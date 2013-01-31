package org.ntb.imageresizer

import org.ntb.imageresizer.util.FileUtils
import org.scalatest.OptionValues._
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import java.net.URI

class FileUtilsTest extends WordSpec with ShouldMatchers  {
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
      path should not be ('defined)
    }

    "return Nothing if extension is not found from an HTTP URL" in {
      val uri = URI.create("http://www.server.com/picturedata?format=jpg")
      val path = FileUtils.getFileExtension(uri)
      path should not be ('defined)
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
      path should not be ('defined)
    }
  }
}