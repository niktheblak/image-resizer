package org.ntb.imageresizer.io

import org.apache.http.impl.client.{ CloseableHttpClient, HttpClientBuilder }
import org.apache.http.config.SocketConfig

object HttpClients {
  val defaultHttpTimeout = 10000

  def createHttpClient(timeout: Int = defaultHttpTimeout): CloseableHttpClient = {
    val socketConfig = SocketConfig.custom().setSoTimeout(defaultHttpTimeout).build()
    HttpClientBuilder.create().setDefaultSocketConfig(socketConfig).build()
  }
}