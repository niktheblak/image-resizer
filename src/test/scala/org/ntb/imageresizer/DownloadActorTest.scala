package org.ntb.imageresizer

import java.io.File
import java.net.URI
import java.util.concurrent.TimeUnit
import org.apache.http.HttpException
import org.apache.http.client.HttpClient
import org.junit.runner.RunWith
import org.ntb.imageresizer.MockHttpClients.canBeEqual
import org.ntb.imageresizer.actor.DownloadActor
import org.ntb.imageresizer.actor.DownloadActor._
import org.ntb.imageresizer.io.Downloader
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import com.google.common.io.Files
import MockHttpClients.statusCodeHttpClient
import MockHttpClients.successfulHttpClient
import akka.actor.ActorSystem
import akka.actor.Status
import akka.dispatch.Await
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.pattern.ask
import akka.util.ByteString
import akka.util.FiniteDuration
import org.ntb.imageresizer.io.HttpClientProvider

@RunWith(classOf[JUnitRunner])
class DownloadActorTest extends TestKit(ActorSystem("TestSystem")) with ImplicitSender with Specification {
  import MockHttpClients._
  val timeout = new FiniteDuration(5, TimeUnit.SECONDS)
  
  "DownloadActor" should {
    "return downloaded data for DownloadRequest" in {
      val testData: Array[Byte] = Array(1, 2, 3)
      val uri = URI.create("http://localhost/logo.png")
      val httpClient = successfulHttpClient(testData)
      val downloadActor = TestActorRef(testDownloadActor(httpClient))
      val downloadTask = ask(downloadActor, DownloadRequest(uri))(timeout)
      Await.result(downloadTask, timeout) match {
        case DownloadResponse(data) => data === ByteString(1, 2, 3)
        case msg => failure("Unexpected response " + msg)
      }
    }
    
    "download data to file for DownloadFileRequest" in {
      val testData: Array[Byte] = Array(1, 2, 3)
      val uri = URI.create("http://localhost/logo.png")
      val httpClient = successfulHttpClient(testData)
      val target = File.createTempFile("DownloadActorTest", ".tmp")
      target.deleteOnExit()
      val downloadActor = TestActorRef(testDownloadActor(httpClient))
      val downloadTask = ask(downloadActor, DownloadToFileRequest(uri, target))(timeout)
      Await.result(downloadTask, timeout) match {
        case DownloadToFileResponse(size) =>
          size === 3
          Files.toByteArray(target).toSeq === testData.toSeq
        case msg => failure("Unexpected response " + msg)
      }
    }
    
    "reply with failure status when download failed" in {
      val uri = URI.create("http://localhost/logo.png")
      val httpClient = statusCodeHttpClient(404)
      val downloadActor = TestActorRef(testDownloadActor(httpClient))
      val downloadTask = ask(downloadActor, DownloadRequest(uri))(timeout)
      Await.result(downloadTask, timeout) must throwA[HttpException]
    }
  }
  
  step {
    system.shutdown()
    success
  }
  
  def testDownloadActor(backingHttpClient: HttpClient): DownloadActor =
    new TestDownloadActor(backingHttpClient)
  
  class TestDownloadActor(backingHttpClient: HttpClient) extends DownloadActor with HttpClientProvider {
    override val httpClient = backingHttpClient
  }
}