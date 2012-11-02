package org.ntb.imageresizer

import java.io.File
import java.net.URI
import org.apache.http.HttpException
import org.apache.http.client.HttpClient
import org.junit.runner.RunWith
import org.ntb.imageresizer.MockHttpClients.canBeEqual
import org.ntb.imageresizer.actor.BaseDownloadActor
import org.ntb.imageresizer.actor.BaseDownloadActor._
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
import org.ntb.imageresizer.io.Downloader

@RunWith(classOf[JUnitRunner])
class DownloadActorTest extends TestKit(ActorSystem("TestSystem")) with ImplicitSender with Specification {
  import MockHttpClients._
  
  "DownloadActor" should {
    "return downloaded data for DownloadRequest" in {
      val testData: Array[Byte] = Array(1, 2, 3)
      val uri = URI.create("http://localhost/logo.png")
      val httpClient = successfulHttpClient(testData)
      val downloadActor = TestActorRef(new TestDownloadActor(httpClient))
      downloadActor ! DownloadRequest(uri)
      val response = expectMsgType[DownloadResponse]
      system.stop(downloadActor)
      response.data.toSeq === testData.toSeq
    }
    
    "download data to file for DownloadFileRequest" in {
      val testData: Array[Byte] = Array(1, 2, 3)
      val uri = URI.create("http://localhost/logo.png")
      val httpClient = successfulHttpClient(testData)
      val target = File.createTempFile("DownloadActorTest", ".tmp")
      target.deleteOnExit()
      val downloadActor = TestActorRef(new TestDownloadActor(httpClient))
      downloadActor ! DownloadToFileRequest(uri, target)
      val response = expectMsgType[DownloadToFileResponse]
      system.stop(downloadActor)
      response.fileSize === 3
      Files.toByteArray(target).toSeq === testData.toSeq
    }
    
    "reply with failure status when download failed" in {
      val uri = URI.create("http://www.server.com/logo.png")
      val httpClient = statusCodeHttpClient(404)
      val target = File.createTempFile("DownloadActorTest", ".tmp")
      target.deleteOnExit()
      val downloadActor = TestActorRef(new TestDownloadActor(httpClient))
      downloadActor ! DownloadToFileRequest(uri, target)
      val failure = expectMsgType[Status.Failure]
      system.stop(downloadActor)
      failure.cause must beAnInstanceOf[HttpException]
    }
  }
  
  step {
    system.shutdown()
    success
  }
  
  class TestDownloadActor(backingHttpClient: HttpClient) extends BaseDownloadActor {
    override val httpClient = backingHttpClient
  }
}