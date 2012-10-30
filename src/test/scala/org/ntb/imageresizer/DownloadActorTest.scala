package org.ntb.imageresizer

import java.io.File
import java.net.URI
import org.apache.http.HttpException
import org.apache.http.client.HttpClient
import org.junit.runner.RunWith
import org.ntb.imageresizer.MockHttpClients.canBeEqual
import org.ntb.imageresizer.actor.DownloadActor
import org.ntb.imageresizer.actor.DownloadActor.DownloadRequest
import org.ntb.imageresizer.actor.DownloadActor.DownloadResponse
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import com.google.common.io.Files
import MockHttpClients.statusCodeHttpClient
import MockHttpClients.successfulHttpClient
import akka.actor.ActorSystem
import akka.actor.Status
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import org.ntb.imageresizer.io.FileDownloader

@RunWith(classOf[JUnitRunner])
class DownloadActorTest extends TestKit(ActorSystem("TestSystem")) with ImplicitSender with Specification {
  import MockHttpClients._
  
  "DownloadActor" should {
    "reply with DownloadResponse when download succeeded" in {
      val testData: Array[Byte] = Array(1, 2, 3)
      val uri = URI.create("http://localhost/logo.png")
      val httpClient = successfulHttpClient(testData)
      val target = File.createTempFile("DownloadActorTest", ".tmp")
      target.deleteOnExit()
      val downloadActor = TestActorRef(new TestDownloadActor(httpClient))
      downloadActor ! DownloadRequest(uri, target)
      val response = expectMsgType[DownloadResponse]
      response.fileSize === 3
      Files.toByteArray(target).toSeq === testData.toSeq
      success
    }
    
    "reply with failure status when download failed" in {
      val uri = URI.create("http://www.server.com/logo.png")
      val httpClient = statusCodeHttpClient(404)
      val target = File.createTempFile("DownloadActorTest", ".tmp")
      target.deleteOnExit()
      val downloadActor = TestActorRef(new TestDownloadActor(httpClient))
      downloadActor ! DownloadRequest(uri, target)
      val failure = expectMsgType[Status.Failure]
      failure.cause must beAnInstanceOf[HttpException]
    }
  }
  
  step {
    system.shutdown()
    success
  }
  
  class TestDownloadActor(backingHttpClient: HttpClient) extends DownloadActor {
    override val httpClient = backingHttpClient
  }
}