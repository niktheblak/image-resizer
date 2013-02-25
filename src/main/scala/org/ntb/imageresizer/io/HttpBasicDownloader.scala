package org.ntb.imageresizer.io

import org.apache.http.{HttpException, HttpResponse, Header}
import java.net.URI
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.ClientProtocolException

trait HttpBasicDownloader { self: HttpClientProvider ⇒
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
}
