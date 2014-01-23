package org.ntb.imageresizer.io

import java.net.URI
import org.apache.http.client.methods.HttpGet
import org.apache.http.{HttpResponse, Header}
import org.ntb.imageresizer.util.Loans.using

trait HttpBasicDownloader { self: HttpClientProvider ⇒
  def httpGetWithHeaders[A](headers: Seq[Header])(uri: URI)(f: HttpResponse ⇒ A): A = {
    val get = new HttpGet(uri)
    headers foreach get.setHeader
    using (httpClient.execute(get)) { response ⇒
      f(response)
    }
  }

  def httpGet[A](uri: URI)(f: HttpResponse ⇒ A): A = httpGetWithHeaders(Nil)(uri)(f)
}
