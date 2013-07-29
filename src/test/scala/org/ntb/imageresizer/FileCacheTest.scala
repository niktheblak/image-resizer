package org.ntb.imageresizer

import java.io.File
import java.nio.charset.Charset
import org.ntb.imageresizer.cache.FileCache
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.OptionValues._
import com.google.common.io.Files
import akka.util.ByteString
import java.util.UUID

class FileCacheTest extends WordSpec with ShouldMatchers {
  import FileCacheTest._
  
  "put" should {
    "create a file with name given by cachePathProvider" in {
      val tempFile = createTempFile("testFile")
      val fileCache = new TestFileCache(filePathProvider(tempFile))
      fileCache.put("testFile", ByteString("abcd"))
      tempFile should be ('exists)
      Files.toString(tempFile, Charset.forName("UTF-8")) should equal ("abcd")
    }
    
    "overwrite existing file with new content" in {
      val tempFile = createTempFile("testFile")
      val fileCache = new TestFileCache(filePathProvider(tempFile))
      fileCache.put("testFile", ByteString("abcd"))
      tempFile should be ('exists)
      fileCache.put("testFile", ByteString("efgh"))
      Files.toString(tempFile, Charset.forName("UTF-8")) should equal ("efgh")
    }
  }
  
  "get(key)" should {
    "return the content of existing file" in {
      val tempFile = createTempFile("testFile")
      val fileCache = new TestFileCache(filePathProvider(tempFile))
      fileCache.put("testFile", ByteString("abcd"))
      val content = fileCache.get("testFile")
      content.value.utf8String should equal ("abcd")
    }
    
    "return None if file with specified key does not exist" in {
      val fileCache = new TestFileCache((key: String) => nonExistingFile)
      new File("testFile") should not be 'exists
      val content = fileCache.get("testFile")
      content should not be 'defined
    }
  }
  
  "get(key, loader)" should {
    "create a file with content given by loader" in {
      val tmpdir = System.getProperty("java.io.tmpdir")
      val tempFile = nonExistingFile
      tempFile.deleteOnExit()
      tempFile should not be 'exists
      val fileCache = new TestFileCache(filePathProvider(tempFile))
      val content = fileCache.get("testFile", () => ByteString("abcd"))
      tempFile should be ('exists)
      content.utf8String should equal ("abcd")
      Files.toString(tempFile, Charset.forName("UTF-8")) should equal ("abcd")
    }
  }
}

class TestFileCache(val cacheFileProvider: String ⇒ File) extends FileCache[String]

object FileCacheTest {
  def createTempFile(nameFragment: String): File = {
    val tempFile = File.createTempFile("FileCacheTest-" + nameFragment, ".tmp")
    tempFile.deleteOnExit()    
    tempFile
  }
  
  def filePathProvider(file: File)(key: String): File = file
  
  def nonExistingFile: File = new File(UUID.randomUUID().toString)
}