package org.ntb.imageresizer.io

import org.ntb.imageresizer.util.Loans.using
import org.apache.http.HttpException
import org.apache.http.HttpHeaders._
import org.apache.http.HttpStatus._
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpHead
import com.google.common.io.ByteStreams
import akka.util.ByteString
import java.io.{FileOutputStream, File, InputStream, OutputStream}
import java.net.URI
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Date

trait Downloader { self: HttpClientProvider =>
  def download(uri: URI, output: OutputStream): Long = {
    download(uri, input => ByteStreams.copy(input, output))
  }

  def download(uri: URI, target: File): Long = {
    download(uri, input => using (new FileOutputStream(target)) { output =>
      ByteStreams.copy(input, output)
    })
  }

  def download[A](uri: URI, f: InputStream => A): A = {
    try {
      val get = new HttpGet(uri)
      val response = httpClient.execute(get)
      if (response.getStatusLine.getStatusCode != SC_OK) {
        get.abort()
        throw new HttpException("Server responded HTTP %d for HTTP GET %s".format(response.getStatusLine.getStatusCode, uri))
      }
      val entity = response.getEntity
      using(entity.getContent) { input =>
        f(input)
      }
    } catch {
      case e: ClientProtocolException => throw new HttpException(e.getMessage, e)
    }
  }
  
  def downloadIfModified(uri: URI, lastModified: Long): Option[ByteString] = {
    downloadIfModified(uri, lastModified, input => ByteString(ByteStreams.toByteArray(input)))
  }

  def downloadIfModified[A](uri: URI, lastModified: Long, f: InputStream => A): Option[A] = {
    try {
      val get = new HttpGet(uri)
      get.setHeader(IF_MODIFIED_SINCE, toLastModifiedHeader(lastModified))
      val response = httpClient.execute(get)
      val statusCode = response.getStatusLine.getStatusCode
      if (statusCode == SC_NOT_MODIFIED) {
        get.abort()
        None
      } else if (statusCode == SC_OK) {
        val entity = response.getEntity
        using(entity.getContent) { input =>
          Some(f(input))
        }
      } else {
        get.abort()
        throw new HttpException("Server responded HTTP %d for HTTP GET %s".format(response.getStatusLine.getStatusCode, uri))
      }
    } catch {
      case e: ClientProtocolException => throw new HttpException(e.getMessage, e)
    }
  }

  def lastModified(uri: URI): Long = {
    try {
      val head = new HttpHead(uri)
      val response = httpClient.execute(head)
      val lastModified = response.getFirstHeader(LAST_MODIFIED)
      fromLastModifiedHeader(lastModified.getValue)
    } catch {
      case e: ParseException => throw new HttpException(e.getMessage, e)
      case e: ClientProtocolException => throw new HttpException(e.getMessage, e)
    }
  }
  
  def toLastModifiedHeader(lastModified: Long): String = {
    val dateFormat = httpDateFormat
    dateFormat.format(new Date(lastModified))
  }
  
  def fromLastModifiedHeader(lastModifiedString: String): Long = {
    val dateFormat = httpDateFormat
    val lastModified = dateFormat.parse(lastModifiedString)
    lastModified.getTime
  }

  def httpDateFormat: SimpleDateFormat = {
    val dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    dateFormat
  }
}