package org.ntb.imageresizer.io

import org.apache.http.client.HttpClient
import org.apache.http.params.HttpConnectionParams

trait DefaultHttpClient {
  val httpClient = createDefaultHttpClient()
  
  val defaultHttpTimeout = 10000
  
  def createDefaultHttpClient(timeout: Int = defaultHttpTimeout): HttpClient = {
    val httpClient = new org.apache.http.impl.client.DefaultHttpClient()
    val params = httpClient.getParams()
    HttpConnectionParams.setConnectionTimeout(params, timeout)
    HttpConnectionParams.setSoTimeout(params, timeout)
    httpClient
  }
}