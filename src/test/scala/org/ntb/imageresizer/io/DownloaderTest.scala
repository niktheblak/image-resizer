package org.ntb.imageresizer.io

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.URI
import org.apache.http.HttpException
import org.apache.http.HttpStatus._
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.mockito.Matchers.any
import org.mockito.Mockito.verify
import org.ntb.imageresizer.MockHttpClients
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class DownloaderTest extends FlatSpec with Matchers with MockHttpClients {
  import DownloaderTest._

  val testData: Array[Byte] = Array(1.toByte, 2.toByte, 3.toByte)

  "Downloader" should "successfully download data from HTTP URL to OutputStream" in {
    val uri = URI.create("http://localhost/logo.png")
    val httpClient = successfulHttpClient(testData)
    val downloader = new TestDownloader(httpClient)
    val output = new ByteArrayOutputStream()
    val fileSize = downloader.download(uri, output)
    fileSize should equal (testData.length)
    output.toByteArray should equal (testData)
  }

  it should "download if content is modified" in {
    val uri = URI.create("http://localhost/logo.png")
    val httpClient = successfulHttpClient(testData)
    val downloader = new TestIfModifiedDownloader(httpClient)
    val output = new ByteArrayOutputStream()
    val fileSizeOpt = downloader.downloadIfModified(uri, 1, output)
    fileSizeOpt should be ('defined)
    output.toByteArray should equal (testData)
  }

  it should "not download if content is not modified" in {
    val uri = URI.create("http://localhost/logo.png")
    val httpClient = statusCodeHttpClient(SC_NOT_MODIFIED)
    val downloader = new TestIfModifiedDownloader(httpClient)
    val output = mock[OutputStream]
    val result = downloader.downloadIfModified(uri, 1, output)
    result should not be 'defined
  }

  it should "throw HttpNotFoundException when the resource does not exist" in {
    val httpClient = statusCodeHttpClient(SC_NOT_FOUND)
    val downloader = new TestDownloader(httpClient)
    val output = new ByteArrayOutputStream()
    an[HttpNotFoundException] should be thrownBy {
      downloader.download(URI.create("http://localhost/logo.png"), output)
    }
    verify(httpClient).execute(any[HttpGet])
  }

  it should "throw HttpException when HTTP server responds with an error code" in {
    val httpClient = statusCodeHttpClient(SC_INTERNAL_SERVER_ERROR)
    val downloader = new TestDownloader(httpClient)
    val output = new ByteArrayOutputStream()
    an[HttpException] should be thrownBy {
      downloader.download(URI.create("http://localhost/logo.png"), output)
    }
    verify(httpClient).execute(any[HttpGet])
  }

  it should "parse HTTP date header" in {
    val httpClient = statusCodeHttpClient(SC_OK)
    val downloader = new TestIfModifiedDownloader(httpClient)
    val date = downloader.fromLastModifiedHeader("Thu, 14 Feb 2013 12:00:00 GMT")
    date should equal (1360843200000L)
  }

  it should "produce correct HTTP date header" in {
    val httpClient = statusCodeHttpClient(SC_OK)
    val downloader = new TestIfModifiedDownloader(httpClient)
    val header = downloader.toLastModifiedHeader(1360843200000L)
    header should equal ("Thu, 14 Feb 2013 12:00:00 GMT")
  }
}

object DownloaderTest {
  class TestDownloader(override val httpClient: CloseableHttpClient)
    extends HttpClientProvider
    with Downloader
    with IfModifiedDownloader

  class TestIfModifiedDownloader(override val httpClient: CloseableHttpClient)
    extends HttpClientProvider
    with IfModifiedDownloader
}