package org.ntb.imageresizer

import java.net.URI
import org.junit.runner.RunWith
import org.ntb.imageresizer.util.FilePathUtils
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FilePathUtilsTest extends Specification  {
  "getFileExtension" should {
    "return extension of a filename in HTTP URL" in {
      val uri = URI.create("http://www.server.com/logo.png")
      val path = FilePathUtils.getFileExtension(uri)
      path.isDefined must beTrue
      path.get must_== ("png")
    }

    "return extension of a plain filename" in {
      val uri = URI.create("logo.png")
      val path = FilePathUtils.getFileExtension(uri)
      path.isDefined must beTrue
      path.get must_== ("png")
    }

    "return file extension of a relative path" in {
      val uri = URI.create("/path/to/logo.png")
      val path = FilePathUtils.getFileExtension(uri)
      path.isDefined must beTrue
      path.get must_== ("png")
    }

    "return extension of a filename with dots" in {
      val uri = URI.create("my.prefix.logo.png")
      val path = FilePathUtils.getFileExtension(uri)
      path.isDefined must beTrue
      path.get must_== ("png")
    }

    "return Nothing if extension is not found from a file path" in {
      val uri = URI.create("/path/logo")
      val path = FilePathUtils.getFileExtension(uri)
      path.isDefined must beFalse
    }

    "return Nothing if extension is not found from an HTTP URL" in {
      val uri = URI.create("http://www.server.com/picturedata?format=jpg")
      val path = FilePathUtils.getFileExtension(uri)
      path.isDefined must beFalse
    }
  }

  "getFilePath" should {
    "return filename from an HTTP URL" in {
      val uri = URI.create("http://www.server.com/logo.png")
      val path = FilePathUtils.getFilePath(uri)
      path.isDefined must beTrue
      path.get must_== ("logo.png")
    }

    "return filename from a relative path" in {
      val uri = URI.create("/path/to/logo.png")
      val path = FilePathUtils.getFilePath(uri)
      path.isDefined must beTrue
      path.get must_== ("logo.png")
    }

    "return filename from HTTP URL with multiple path segments" in {
      val uri = URI.create("http://www.server.com/site/a/logo.png")
      val path = FilePathUtils.getFilePath(uri)
      path.isDefined must beTrue
      path.get must_== ("logo.png")
    }

    "return Nothing if no path was found" in {
      val uri = URI.create("http://www.server.com/")
      val path = FilePathUtils.getFilePath(uri)
      path.isDefined must beFalse
    }
  }
}