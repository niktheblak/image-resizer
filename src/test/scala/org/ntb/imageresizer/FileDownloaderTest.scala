package org.ntb.imageresizer

import java.io.ByteArrayInputStream
import java.net.URI

import org.apache.http.client.methods.HttpGet
import org.apache.http.client.HttpClient
import org.apache.http.HttpEntity
import org.apache.http.HttpException
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.ntb.imageresizer.io.FileDownloader
import org.specs2.runner.JUnitRunner
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

@RunWith(classOf[JUnitRunner])
class FileDownloaderTest extends Specification with Mockito {
  import MockHttpClients._
  "FileDownloader" should {
    "successfully download data from HTTP URL" in {
      val uri = URI.create("http://www.server.com/logo.png")
      val testData: Array[Byte] = Array(1, 2, 3)
      val httpClient = successfulHttpClient(testData)
      val downloader = fileDownloader(httpClient)
      val data = downloader.downloadToByteString(uri)
      val captor = ArgumentCaptor.forClass(classOf[HttpGet])
      there was one(httpClient).execute(captor.capture())
      captor.getValue().getURI() must_== uri
      data.seq must_== testData.toSeq
    }
    
    "throw HttpException when HTTP server respons with an error code" in {
      val httpClient = statusCodeHttpClient(404)
      val downloader = fileDownloader(httpClient)
      downloader.downloadToByteString(URI.create("http://www.server.com/logo.png")) must throwA[HttpException]
      there was one(httpClient).execute(any[HttpGet])
    }
  }
  
  def fileDownloader(httpClient: HttpClient): FileDownloader =
    new TestFileDownloader(httpClient)

  class TestFileDownloader(backingHttpClient: HttpClient) extends FileDownloader {
    override val httpClient = backingHttpClient
  }
}