package org.ntb.imageresizer

import actor.file.DownloadActor
import org.apache.http.impl.client.CloseableHttpClient

class TestDownloadActor(backingHttpClient: CloseableHttpClient) extends DownloadActor {
  override val httpClient = backingHttpClient
}
