package org.ntb.imageresizer.imageformat

import java.net.URI
import org.scalatest.Matchers
import org.scalatest.OptionValues._
import org.scalatest.WordSpec

class ImageFormatTest extends WordSpec with Matchers {
  import scala.language.postfixOps

  "getFileExtension" should {
    "return extension of a filename in HTTP URL" in {
      val uri = URI.create("http://www.server.com/logo.png")
      val path = getFileExtension(uri)
      path.value should equal ("png")
    }

    "return extension of a filename in HTTP URL with query parameters" in {
      val uri = URI.create("http://www.server.com/logo.png?sessionId=0abcDE&track=1")
      val path = getFileExtension(uri)
      path.value should equal ("png")
    }

    "return extension of a plain filename" in {
      val uri = URI.create("logo.png")
      val path = getFileExtension(uri)
      path.value should equal ("png")
    }

    "return file extension of a relative path" in {
      val uri = URI.create("/path/to/logo.png")
      val path = getFileExtension(uri)
      path.value should equal ("png")
    }

    "return extension of a filename with dots" in {
      val uri = URI.create("my.prefix.logo.png")
      val path = getFileExtension(uri)
      path.value should equal ("png")
    }

    "return Nothing if path is not specified in HTTP URL" in {
      val uri = URI.create("http://www.server.com")
      val path = getFileExtension(uri)
      path should not be 'defined
    }

    "return Nothing if extension is not found from a file path" in {
      val uri = URI.create("/path/logo")
      val path = getFileExtension(uri)
      path should not be 'defined
    }

    "return Nothing if extension is not found from an HTTP URL" in {
      val uri = URI.create("http://www.server.com/picturedata?format=jpg")
      val path = getFileExtension(uri)
      path should not be 'defined
    }
  }
}
