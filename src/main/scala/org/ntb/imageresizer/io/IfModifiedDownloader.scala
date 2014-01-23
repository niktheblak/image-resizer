package org.ntb.imageresizer.io

import com.google.common.io.ByteStreams
import java.io.{OutputStream, InputStream}
import java.net.URI
import java.text.ParseException
import java.util.Locale
import org.apache.http.HttpException
import org.apache.http.HttpHeaders._
import org.apache.http.HttpStatus._
import org.apache.http.client.methods.HttpHead
import org.apache.http.message.BasicHeader
import org.joda.time.format.DateTimeFormat
import org.ntb.imageresizer.util.Loans._

trait IfModifiedDownloader extends BasicHttpOperations { self: HttpClientProvider ⇒
  val httpDateFormatter = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
    .withZoneUTC()
    .withLocale(Locale.US)

  def downloadIfModified[A](uri: URI, lastModified: Long, f: Option[InputStream] ⇒ A): A = {
    val headers = Seq(new BasicHeader(IF_MODIFIED_SINCE, toLastModifiedHeader(lastModified)))
    httpGetWithHeaders(headers)(uri) { response ⇒
      if (response.getStatusLine.getStatusCode == SC_NOT_MODIFIED) {
        f(None)
      } else {
        val entity = response.getEntity
        using(entity.getContent) { input ⇒ f(Some(input)) }
      }
    }
  }

  def downloadIfModified[A](uri: URI, lastModified: Long, output: OutputStream): Option[Long] = {
    val copyIfModified: Option[InputStream] ⇒ Option[Long] =
      result ⇒ result map(ByteStreams.copy(_, output))
    downloadIfModified(uri, lastModified, copyIfModified)
  }

  def lastModified(uri: URI): Long = {
    try {
      val head = new HttpHead(uri)
      using (httpClient.execute(head)) { response =>
        val lastModified = response.getFirstHeader(LAST_MODIFIED)
        fromLastModifiedHeader(lastModified.getValue)
      }
    } catch {
      case e: ParseException ⇒ throw new HttpException(e.getMessage, e)
    }
  }

  def toLastModifiedHeader(lastModified: Long): String = {
    httpDateFormatter.print(lastModified)
  }

  def fromLastModifiedHeader(lastModifiedString: String): Long = {
    httpDateFormatter.parseMillis(lastModifiedString)
  }
}
