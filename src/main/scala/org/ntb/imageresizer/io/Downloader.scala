package org.ntb.imageresizer.io

import java.io.InputStream
import java.io.OutputStream
import java.net.URI

import org.apache.http.HttpException
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.ntb.imageresizer.util.Loans.using

import com.google.common.io.ByteStreams

import akka.util.ByteString

trait Downloader { self: HttpClientProvider =>
  def download(uri: URI): ByteString = {
    downloadWith(uri, input => ByteString(ByteStreams.toByteArray(input)))
  }

  def download(uri: URI, output: OutputStream): Long = {
    downloadWith(uri, input => ByteStreams.copy(input, output))
  }

  def downloadWith[A](uri: URI, contentProcessor: InputStream => A): A = {
    try {
      val get = new HttpGet(uri)
      val response = httpClient.execute(get)
      if (response.getStatusLine().getStatusCode() >= 300) {
        get.abort()
        throw new HttpException("Server responded HTTP %d for HTTP GET %s".format(response.getStatusLine().getStatusCode(), uri))
      }
      val entity = response.getEntity()
      using(entity.getContent()) { input =>
        contentProcessor(input)
      }
    } catch {
      case e: ClientProtocolException => throw new HttpException(e.getMessage(), e)
    }
  }
}