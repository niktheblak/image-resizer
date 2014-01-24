package org.ntb.imageresizer.io

import java.net.URI
import org.apache.http.client.methods.HttpGet
import org.apache.http.{ HttpException, HttpResponse, Header }
import org.ntb.imageresizer.util.Loans.using
import org.apache.http.client.ClientProtocolException

trait BasicHttpOperations { self: HttpClientProvider ⇒
  def httpGetWithHeaders[A](headers: Seq[Header])(uri: URI)(f: HttpResponse ⇒ A): A = {
    val get = new HttpGet(uri)
    headers foreach get.setHeader
    try {
      using (httpClient.execute(get)) { response ⇒
        f(response)
      }
    } catch {
      case e: ClientProtocolException ⇒ throw new HttpException(e.getMessage, e)
    }
  }

  def httpGet[A](uri: URI)(f: HttpResponse ⇒ A): A = httpGetWithHeaders(Nil)(uri)(f)
}
