package org.ntb.imageresizer.io

import org.apache.http.client.HttpClient

trait HttpClientProvider {
  val httpClient: HttpClient
}