package org.ntb.imageresizer.io

import org.ntb.imageresizer.util.Loans.using
import org.apache.http.HttpException
import org.apache.http.HttpStatus._
import org.apache.http.StatusLine
import org.apache.http.util.EntityUtils
import com.google.common.io.ByteStreams
import java.io.{FileOutputStream, File, InputStream, OutputStream}
import java.net.URI

trait Downloader extends HttpBasicDownloader { self: HttpClientProvider ⇒
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
}