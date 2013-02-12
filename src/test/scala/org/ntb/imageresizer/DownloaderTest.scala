package org.ntb.imageresizer

import java.io.ByteArrayOutputStream
import java.net.URI
import org.apache.http.HttpException
import org.apache.http.HttpStatus._
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify
import org.mockito.Matchers.any
import org.ntb.imageresizer.io.Downloader
import org.scalatest.FlatSpec
import org.scalatest.OptionValues._
import org.scalatest.matchers.ShouldMatchers
import MockHttpClients.statusCodeHttpClient
import MockHttpClients.successfulHttpClient
import org.ntb.imageresizer.io.HttpClientProvider

class FileDownloaderTest extends FlatSpec with ShouldMatchers {
  import MockHttpClients._
  import FileDownloaderTest._

  val testData: Array[Byte] = Array(1.toByte, 2.toByte, 3.toByte)

  "FileDownloader" should "successfully download data from HTTP URL to ByteString" in {
    val uri = URI.create("http://localhost/logo.png")
    val httpClient = successfulHttpClient(testData)
    val downloader = fileDownloader(httpClient)
    val data = downloader.download(uri)
    val captor = ArgumentCaptor.forClass(classOf[HttpGet])
    verify(httpClient).execute(captor.capture())
    captor.getValue().getURI() should equal (uri)
    data.toArray should equal (testData)
  }

  it should "successfully download data from HTTP URL to OutputStream" in {
    val uri = URI.create("http://localhost/logo.png")
    val httpClient = successfulHttpClient(testData)
    val downloader = fileDownloader(httpClient)
    val output = new ByteArrayOutputStream()
    val fileSize = downloader.download(uri, output)
    fileSize should equal (testData.length)
    output.toByteArray() should equal (testData)
  }

  it should "download if content is modified" in {
    val uri = URI.create("http://localhost/logo.png")
    val httpClient = successfulHttpClient(testData)
    val downloader = fileDownloader(httpClient)
    val result = downloader.downloadIfModified(uri, 1)
    result.value.toArray should equal (testData)
  }

  it should "not download if content is not modified" in {
    val uri = URI.create("http://localhost/logo.png")
    val httpClient = statusCodeHttpClient(SC_NOT_MODIFIED)
    val downloader = fileDownloader(httpClient)
    val result = downloader.downloadIfModified(uri, 1)
    result should not be ('defined)
  }

  it should "throw HttpException when HTTP server responds with an error code" in {
    val httpClient = statusCodeHttpClient(404)
    val downloader = fileDownloader(httpClient)
    evaluating { downloader.download(URI.create("http://localhost/logo.png")) } should produce [HttpException]
    verify(httpClient).execute(any[HttpGet])
  }
}

object FileDownloaderTest {
  def fileDownloader(httpClient: HttpClient): Downloader =
    new TestFileDownloader(httpClient)

  class TestFileDownloader(backingHttpClient: HttpClient) extends Downloader with HttpClientProvider {
    override val httpClient = backingHttpClient
  }
}