package org.ntb.imageresizer.io

import java.net.URI
import akka.util.ByteString
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.HttpClient
import org.apache.http.HttpException
import org.ntb.imageresizer.util.Loans.using
import com.google.common.io.ByteStreams
import java.io.File
import java.io.OutputStream
import org.apache.http.params.HttpConnectionParams

trait Downloader {
  val httpClient: HttpClient
  
  def download(uri: URI): ByteString = {
    val get = new HttpGet(uri)
    val response = httpClient.execute(get)
    if (response.getStatusLine().getStatusCode() >= 300) {
      get.abort()
      throw new HttpException("Server responded HTTP %d for HTTP GET %s".format(response.getStatusLine().getStatusCode(), uri))
    }
    val entity = response.getEntity()
    using(entity.getContent()) { input =>
      ByteString(ByteStreams.toByteArray(input))
    }
  }
  
  def download(uri: URI, output: OutputStream): Long = {
    val get = new HttpGet(uri)
    val response = httpClient.execute(get)
    if (response.getStatusLine().getStatusCode() >= 300) {
      get.abort()
      throw new HttpException("Server responded HTTP %d for HTTP GET %s".format(response.getStatusLine().getStatusCode(), uri))
    }
    val entity = response.getEntity()
    using(entity.getContent()) { input =>
      ByteStreams.copy(input, output)
    }
  }
}