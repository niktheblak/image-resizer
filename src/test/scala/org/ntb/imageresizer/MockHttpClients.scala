package org.ntb.imageresizer

import java.io.ByteArrayInputStream

import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet}
import org.apache.http.HttpEntity
import org.apache.http.StatusLine
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito.when
import org.mockito.Matchers.any
import org.apache.http.impl.client.CloseableHttpClient

trait MockHttpClients extends MockitoSugar {
  def successfulHttpClient(data: Array[Byte], statusCode: Int = 200): CloseableHttpClient = {
    val statusLine = mock[StatusLine]
    val entity = mock[HttpEntity]
    val response = mock[CloseableHttpResponse]
    val httpClient = mock[CloseableHttpClient]
    when(statusLine.getStatusCode).thenReturn(statusCode)
    when(entity.getContent).thenReturn(new ByteArrayInputStream(data)).thenThrow(new RuntimeException("getContent already called"))
    when(response.getStatusLine).thenReturn(statusLine)
    when(response.getEntity).thenReturn(entity)
    when(httpClient.execute(any[HttpGet])).thenReturn(response)
    httpClient
  }

  def statusCodeHttpClient(statusCode: Int): CloseableHttpClient = {
    val statusLine = mock[StatusLine]
    val response = mock[CloseableHttpResponse]
    val httpClient = mock[CloseableHttpClient]
    when(statusLine.getStatusCode).thenReturn(statusCode)
    when(response.getStatusLine).thenReturn(statusLine)
    when(httpClient.execute(any[HttpGet])).thenReturn(response)
    httpClient
  }

  def failingHttpClient(e: Exception) = {
    val httpClient = mock[CloseableHttpClient]
    when(httpClient.execute(any[HttpGet])).thenThrow(e)
    httpClient
  }
}