package org.ntb.imageresizer

import org.ntb.imageresizer.MockHttpClients._
import org.ntb.imageresizer.actor.DownloadActor._
import com.google.common.io.Files
import org.apache.http.HttpException
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.pattern.ask
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import java.io.File
import java.net.URI
import java.util.concurrent.TimeUnit

class DownloadActorTest extends TestKit(ActorSystem("TestSystem")) with ImplicitSender with FlatSpec with ShouldMatchers with BeforeAndAfterAll {
  val testData: Array[Byte] = Array(1.toByte, 2.toByte, 3.toByte)
  val timeout = new FiniteDuration(2, TimeUnit.SECONDS)

  "DownloadActor" should "download data to file for DownloadFileRequest" in {
    val uri = URI.create("http://localhost/logo.png")
    val httpClient = successfulHttpClient(testData)
    val target = File.createTempFile("DownloadActorTest", ".tmp")
    target.deleteOnExit()
    val downloadActor = TestActorRef(new TestDownloadActor(httpClient))
    downloadActor ! DownloadRequest(uri, target)
    expectMsgPF(timeout) {
      case DownloadResponse(size) ⇒
        size should equal(3)
        Files.toByteArray(target) should equal(testData)
    }
  }

  it should "reply with failure status when download failed" in {
    val uri = URI.create("http://localhost/logo.png")
    val httpClient = statusCodeHttpClient(404)
    val downloadActor = TestActorRef(new TestDownloadActor(httpClient))
    val file = mock[File]
    val downloadTask = ask(downloadActor, DownloadRequest(uri, file))(timeout)
    evaluating {
      Await.result(downloadTask, timeout)
    } should produce[HttpException]
  }

  override def afterAll {
    system.shutdown()
  }
}