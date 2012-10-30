package org.ntb.imageresizer

import java.io.ByteArrayInputStream
import java.net.URI

import org.apache.http.client.methods.HttpGet
import org.apache.http.client.HttpClient
import org.apache.http.HttpEntity
import org.apache.http.HttpException
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.ntb.imageresizer.io.Downloader
import org.specs2.runner.JUnitRunner
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

object MockHttpClients extends Mockito {
  def successfulHttpClient(data: Array[Byte], statusCode: Int = 200): HttpClient = {
      val statusLine = mock[StatusLine]
      val entity = mock[HttpEntity]
      val response = mock[HttpResponse]
      val httpClient = mock[HttpClient]
      statusLine.getStatusCode() returns statusCode
      entity.getContent() returns new ByteArrayInputStream(data) thenThrows new RuntimeException("getContent already called")
      response.getStatusLine() returns statusLine
      response.getEntity() returns entity
      httpClient.execute(any[HttpGet]) returns response
      httpClient
  }
  
  def statusCodeHttpClient(statusCode: Int): HttpClient = {
      val statusLine = mock[StatusLine]
      val response = mock[HttpResponse]
      val httpClient = mock[HttpClient]
      statusLine.getStatusCode() returns statusCode
      response.getStatusLine() returns statusLine
      httpClient.execute(any[HttpGet]) returns response
      httpClient
  }
  
  def failingHttpClient(e: Exception) = {
      val httpClient = mock[HttpClient]
      httpClient.execute(any[HttpGet]) throws e
      httpClient
  }
}