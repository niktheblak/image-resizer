package org.ntb.imageresizer.actor.file

import org.apache.http.impl.client.CloseableHttpClient

class TestDownloadActor(backingHttpClient: CloseableHttpClient) extends DownloadActor {
  override val httpClient = backingHttpClient
}
