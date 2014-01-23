package org.ntb.imageresizer.io

import org.apache.http.impl.client.CloseableHttpClient

trait HttpClientProvider {
  val httpClient: CloseableHttpClient
}
