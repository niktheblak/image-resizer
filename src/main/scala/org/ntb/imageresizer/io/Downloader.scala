package org.ntb.imageresizer.io

import org.ntb.imageresizer.util.Loans.using
import org.apache.http._
import message.BasicHeader
import org.apache.http.HttpHeaders._
import org.apache.http.HttpStatus._
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpHead
import com.google.common.io.ByteStreams
import java.io.{FileOutputStream, File, InputStream, OutputStream}
import java.net.URI
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Date
import org.apache.http.util.EntityUtils
import scala.Some

trait Downloader { self: HttpClientProvider ⇒
  def httpGetWithHeaders[A](headers: List[Header])(uri: URI)(f: HttpResponse ⇒ A): A = {
    try {
      val get = new HttpGet(uri)
      headers foreach(get.setHeader(_))
      val response = httpClient.execute(get)
      f(response)
    } catch {
      case e: ClientProtocolException ⇒ throw new HttpException(e.getMessage, e)
    }
  }

  def httpGet[A](uri: URI)(f: HttpResponse ⇒ A): A = httpGetWithHeaders(Nil)(uri)(f)

  def download(uri: URI, output: OutputStream): Long = {
    download(uri, input ⇒ ByteStreams.copy(input, output))
  }

  def download(uri: URI, target: File): Long = {
    download(uri, input ⇒ using (new FileOutputStream(target)) { output ⇒
      ByteStreams.copy(input, output)
    })
  }

  def download[A](uri: URI, f: InputStream ⇒ A): A = {
    httpGet(uri) { response ⇒
      val statusLine = response.getStatusLine
      if (statusLine.getStatusCode != SC_OK) {
        val msg = if (response.getEntity != null) EntityUtils.toString(response.getEntity()) else ""
        throw new HttpException("Server responded with HTTP %d %s: %s".format(statusLine.getStatusCode, statusLine.getReasonPhrase, msg))
      }
      val entity = response.getEntity()
      using(entity.getContent()) { input ⇒
        f(input)
      }
    }
  }

  def download[A](uri: URI, f: (StatusLine, InputStream) ⇒ A): A = {
    httpGet(uri) { response ⇒
      val entity = response.getEntity
      using(entity.getContent) { input ⇒
        f(response.getStatusLine, input)
      }
    }
  }

  def downloadIfModified[A](uri: URI, lastModified: Long, f: Option[InputStream] ⇒ A): A = {
    val headers = List(new BasicHeader(IF_MODIFIED_SINCE, toLastModifiedHeader(lastModified)))
    httpGetWithHeaders(headers)(uri) { response ⇒
      if (response.getStatusLine.getStatusCode == SC_NOT_MODIFIED) {
        f(None)
      } else {
        val entity = response.getEntity
        using(entity.getContent) { input ⇒
          f(Some(input))
        }
      }
    }
  }

  def downloadIfModified[A](uri: URI, lastModified: Long, output: OutputStream): Option[Long] = {
    val copyIfModified: Option[InputStream] ⇒ Option[Long] = result ⇒ result match {
      case Some(input) ⇒
        val length = ByteStreams.copy(input, output)
        Some(length)
      case None ⇒ None
    }
    downloadIfModified(uri, lastModified, copyIfModified)
  }

  def lastModified(uri: URI): Long = {
    try {
      val head = new HttpHead(uri)
      val response = httpClient.execute(head)
      val lastModified = response.getFirstHeader(LAST_MODIFIED)
      fromLastModifiedHeader(lastModified.getValue)
    } catch {
      case e: ParseException ⇒ throw new HttpException(e.getMessage, e)
      case e: ClientProtocolException ⇒ throw new HttpException(e.getMessage, e)
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
    val dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"))
    dateFormat
  }
}