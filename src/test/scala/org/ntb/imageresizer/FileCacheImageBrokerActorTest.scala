package org.ntb.imageresizer

import actor.FileCacheImageBrokerActor
import actor.FileCacheImageBrokerActor._
import actor.ResizeActor._
import akka.actor.{Actor, Props, ActorSystem}
import akka.dispatch.Await
import akka.pattern.ask
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import akka.util.FiniteDuration
import com.google.common.io.Files
import imageformat.ImageFormat
import java.io.File
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.ntb.imageresizer.MockHttpClients._
import org.specs2.mutable.Specification

class FileCacheImageBrokerActorTest extends TestKit(ActorSystem("TestSystem")) with ImplicitSender with Specification {
  import FileCacheImageBrokerActorTest._

  val testData: Array[Byte] = Array(1.toByte, 2.toByte, 3.toByte)
  val timeout = new FiniteDuration(5, TimeUnit.SECONDS)

  "FileCacheImageBrokerActor" should {
    "serve existing file" in {
      val testFile = tempFile(testData)
      val imageBrokerActor = system.actorOf(Props(new TestFileCacheImageBrokerActor(_ => testFile)), "imagebroker")
      imageBrokerActor ! GetImageRequest(new URI("http://localhost/file.png"), 200)
      expectMsgPF(timeout) {
        case GetImageResponse(data) => data.getAbsolutePath === testFile.getAbsolutePath
      }
      testFile.delete()
      system.stop(imageBrokerActor)
      success
    }

    "download and resize nonexisting file" in {
      val httpClient = successfulHttpClient(testData)
      val testFile = nonExistingFile()
      val downloadActor = system.actorOf(Props(new TestDownloadActor(httpClient)), "downloader")
      val resizeActor = system.actorOf(Props[TestResizeActor], "resizer")
      val imageBrokerActor = system.actorOf(Props(new TestFileCacheImageBrokerActor(_ => testFile)), "imagebroker")
      imageBrokerActor ! GetImageRequest(new URI("http://localhost/file.png"), 200)
      expectMsgPF(timeout) {
        case GetImageResponse(data) =>
          testFile must exist
          Files.toByteArray(testFile).toSeq === testData.toSeq
          data.getAbsolutePath === testFile.getAbsolutePath
      }
      testFile.delete()
      system.stop(downloadActor)
      system.stop(resizeActor)
      system.stop(imageBrokerActor)
      success
    }

    step {
      system.shutdown()
      success
    }
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

  class TestFileCacheImageBrokerActor(provider: Key => File) extends FileCacheImageBrokerActor {
    override val cacheFileProvider = provider
  }

  class TestResizeActor extends Actor {
    def receive = {
      case ResizeImageToFileRequest(source, target, _, _) =>
        Files.copy(source, target)
        sender ! ResizeImageToFileResponse(target.length())
    }
  }
}
