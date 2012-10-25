package org.ntb.imageresizer

import java.net.URI
import akka.actor.ActorSystem
import akka.actor.Status
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import org.apache.http.client.HttpClient
import org.apache.http.HttpException
import org.junit.runner.RunWith
import org.ntb.imageresizer.actor.DownloadActor
import org.ntb.imageresizer.actor.DownloadFileRequest
import org.ntb.imageresizer.actor.DownloadFileResponse
import org.ntb.imageresizer.io.FileDownloader
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DownloadActorTest extends TestKit(ActorSystem("TestSystem")) with ImplicitSender with Specification {
  import MockHttpClients._
  
  "DownloadActor" should {
    "reply with DownloadFileResponse when download succeeded" in {
      val testData: Array[Byte] = Array(1, 2, 3)
      val uri = URI.create("http://www.server.com/logo.png")
      val httpClient = successfulHttpClient(testData)
      val downloadActor = TestActorRef(new TestDownloadActor(httpClient))
      downloadActor ! DownloadFileRequest(uri)
      val response = expectMsgType[DownloadFileResponse]
      response.data.seq must_== testData.toSeq
    }
    
    "reply with failure status when download failed" in {
      val uri = URI.create("http://www.server.com/logo.png")
      val httpClient = statusCodeHttpClient(404)
      val downloadActor = TestActorRef(new TestDownloadActor(httpClient))
      downloadActor ! DownloadFileRequest(uri)
      val failure = expectMsgType[Status.Failure]
      failure.cause must beAnInstanceOf[HttpException]
    }
  }
  
  step {
    system.shutdown()
    success
  }
  
  class TestDownloadActor(backingHttpClient: HttpClient) extends DownloadActor with FileDownloader {
    override val httpClient = backingHttpClient
  }
}