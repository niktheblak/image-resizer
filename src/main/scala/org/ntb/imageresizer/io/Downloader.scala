package org.ntb.imageresizer.io

import org.ntb.imageresizer.util.Loans.using
import org.apache.http.{ HttpEntity, HttpException, StatusLine }
import org.apache.http.HttpStatus._
import org.apache.http.util.EntityUtils
import com.google.common.io.ByteStreams
import java.io.{ FileOutputStream, File, InputStream, OutputStream }
import java.net.URI

trait Downloader extends BasicHttpOperations { self: HttpClientProvider ⇒
  def download(uri: URI, output: OutputStream): Long =
    downloadWith(uri) { input ⇒
      ByteStreams.copy(input, output)
    }

  def download(uri: URI, target: File): Long =
    downloadWith(uri) { input ⇒
      using (new FileOutputStream(target)) { output ⇒
        ByteStreams.copy(input, output)
      }
    }

  def downloadWith[A](uri: URI)(f: InputStream ⇒ A): A = {
    httpGet(uri) { response ⇒
      val statusLine = response.getStatusLine
      val entity = response.getEntity
      statusLine.getStatusCode match {
        case SC_OK ⇒
          using(entity.getContent) { input ⇒
            f(input)
          }
        case SC_NOT_FOUND ⇒
          val msg = entityToString(entity)
          throw new HttpNotFoundException(msg)
        case _ ⇒
          val msg = entityToString(entity)
          throw new HttpException(s"Server responded with HTTP ${statusLine.getStatusCode} ${statusLine.getReasonPhrase}: $msg")
      }
    }
  }

  def downloadWithStatus[A](uri: URI)(f: (StatusLine, InputStream) ⇒ A): A = {
    httpGet(uri) { response ⇒
      val entity = response.getEntity
      using(entity.getContent) { input ⇒
        f(response.getStatusLine, input)
      }
    }
  }

  private def entityToString(entity: HttpEntity): String = {
    if (entity != null)
      EntityUtils.toString(entity)
    else ""
  }
}