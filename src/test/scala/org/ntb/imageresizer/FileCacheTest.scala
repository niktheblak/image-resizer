package org.ntb.imageresizer

import java.io.File
import java.nio.charset.Charset

import org.junit.runner.RunWith
import org.ntb.imageresizer.cache.FileCache
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import com.google.common.io.Files

import akka.util.ByteString

@RunWith(classOf[JUnitRunner])
class FileCacheTest extends Specification {
  def createTempFile(nameFragment: String): File = {
    val tempFile = File.createTempFile("FileCacheTest-" + nameFragment, ".tmp")
    tempFile.deleteOnExit()    
    tempFile
  }
  
  def filePathProvider(file: File)(key: String): String = file.getAbsolutePath()
  
  "put" should {
    "create a file with name given by cachePathProvider" in {
      val tempFile = createTempFile("testFile")
      val fileCache = new FileCache(filePathProvider(tempFile)_)
      fileCache.put("testFile", ByteString("abcd"))
      tempFile.exists() must beTrue
      Files.toString(tempFile, Charset.forName("UTF-8")) mustEqual "abcd"
    }
    
    "overwrite existing file with new content" in {
      val tempFile = createTempFile("testFile")
      val fileCache = new FileCache(filePathProvider(tempFile)_)
      fileCache.put("testFile", ByteString("abcd"))
      tempFile.exists() must beTrue
      fileCache.put("testFile", ByteString("efgh"))
      Files.toString(tempFile, Charset.forName("UTF-8")) mustEqual "efgh"
    }
  }
  
  "get(key)" should {
    "return the content of existing file" in {
      val tempFile = createTempFile("testFile")
      val fileCache = new FileCache(filePathProvider(tempFile)_)
      fileCache.put("testFile", ByteString("abcd"))
      val content = fileCache.get("testFile")
      content.isDefined must beTrue
      content.get.utf8String mustEqual "abcd"
    }
    
    "return None if file with specified key does not exist" in {
      val fileCache = new FileCache((key: String) => key)
      new File("testFile").exists() must beFalse
      val content = fileCache.get("testFile")
      content.isDefined must beFalse
    }
  }
  
  "get(key, loader)" should {
    "create a file with content given by loader" in {
      val tmpdir = System.getProperty("java.io.tmpdir")
      val tempFile = new File(tmpdir, "testFile")
      tempFile.deleteOnExit()
      tempFile.exists() must beFalse
      val fileCache = new FileCache(filePathProvider(tempFile)_)
      val content = fileCache.get("testFile", () => ByteString("abcd"))
      tempFile.exists() must beTrue
      content.utf8String mustEqual "abcd"
      Files.toString(tempFile, Charset.forName("UTF-8")) mustEqual "abcd"
    }
  }
}