package org.ntb.imageresizer

import org.ntb.imageresizer.MockHttpClients._
import org.ntb.imageresizer.imageformat.ImageFormat
import actor.DownloadActor._
import actor.FileCacheImageBrokerActor
import actor.FileCacheImageBrokerActor._
import actor.ResizeActor._
import com.google.common.io.Files
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import akka.actor.{ Actor, Props, ActorSystem }
import akka.pattern.ask
import akka.testkit.{ TestActorRef, ImplicitSender, TestKit, TestProbe }
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import java.io.File
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit
import akka.actor.ActorRef

class FileCacheImageBrokerActorTest extends TestKit(ActorSystem("TestSystem")) with ImplicitSender with FlatSpec with ShouldMatchers with BeforeAndAfterAll {
  import FileCacheImageBrokerActorTest._

  val testData: Array[Byte] = Array(1.toByte, 2.toByte, 3.toByte)
  val timeout = new FiniteDuration(5, TimeUnit.SECONDS)

  "FileCacheImageBrokerActor" should "serve existing file" in {
    val testFile = tempFile(testData)
    val downloadActor = TestProbe()
    val resizeActor = TestProbe()
    val imageBrokerActor = system.actorOf(Props(new TestFileCacheImageBrokerActor(downloadActor.ref, resizeActor.ref, (_ => testFile))))
    imageBrokerActor ! GetImageRequest(new URI("http://localhost/file.png"), 200)
    expectMsgPF(timeout) {
      case GetImageResponse(data) => data.getAbsolutePath should equal(testFile.getAbsolutePath)
    }
    testFile.delete()
    system.stop(imageBrokerActor)
  }

  it should "download and resize nonexisting file" in {
    val testFile = nonExistingFile()
    val downloadProbe = TestProbe()
    val resizeProbe = TestProbe()
    val imageBrokerActor = system.actorOf(Props(new TestFileCacheImageBrokerActor(downloadProbe.ref, resizeProbe.ref, (_ => testFile))))
    imageBrokerActor ! GetImageRequest(new URI("http://localhost/file.png"), 200)
    downloadProbe.expectMsgPF(timeout) {
      case DownloadRequest(uri, target) =>
        Files.write(testData, target)
        downloadProbe.reply(DownloadResponse(target.length()))
    }
    resizeProbe.expectMsgPF(timeout) {
      case ResizeImageRequest(source, target, _, _) =>
        Files.copy(source, target)
        resizeProbe.reply(ResizeImageResponse(target.length()))
    }
    expectMsgPF(timeout) {
      case GetImageResponse(data) =>
        testFile should be('exists)
        Files.toByteArray(testFile) should equal(testData)
        data.getAbsolutePath should equal(testFile.getAbsolutePath)
    }
    testFile.delete()
    system.stop(imageBrokerActor)
  }

  override def afterAll {
    system.shutdown()
  }
}

object FileCacheImageBrokerActorTest {
  type Key = (String, Int, ImageFormat)

  def nonExistingFile(): File = {
    val file = new File(UUID.randomUUID().toString)
    file.deleteOnExit()
    file
  }

  def tempFile(data: Array[Byte] = Array()): File = {
    val tempFile = File.createTempFile(getClass.getName, ".tmp")
    tempFile.deleteOnExit()
    if (data.length > 0) {
      Files.write(data, tempFile)
    }
    tempFile
  }

  class TestFileCacheImageBrokerActor(downloader: ActorRef, resizer: ActorRef, provider: Key => File) extends FileCacheImageBrokerActor(downloader, resizer) {
    override val cacheFileProvider = provider
  }
}
