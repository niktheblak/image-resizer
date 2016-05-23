package org.ntb.imageresizer.actor

import akka.actor.{ActorSystem, Status}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.ByteString
import com.google.common.io.ByteStreams
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.resize.{Resolution, ResolutionReader}
import org.scalatest._

import scala.concurrent.duration._

class ResizeActorTest
    extends TestKit(ActorSystem("TestActorSystem"))
    with ImplicitSender
    with FlatSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ResolutionReader {
  import ResizeActor._

  val testTimeout = 2.seconds

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "ResizeActor" should "resize sample image to requested dimensions" in {
    val resizeActor = TestActorRef(new ResizeActor)
    val source = ByteString(ByteStreams.toByteArray(getClass.getClassLoader.getResourceAsStream("test_image.jpeg")))
    resizeActor ! ResizeImageRequest(source, 200, JPEG)
    expectMsgPF(testTimeout) {
      case ResizeImageResponse(data) ⇒
        val Resolution(width, height) = readResolution(data)
        width shouldEqual 118
        height shouldEqual 200
    }
  }

  it should "throw with invalid image data" in {
    val resizeActor = TestActorRef(new ResizeActor)
    val source = ByteString(1, 2, 3)
    resizeActor ! ResizeImageRequest(source, 200, JPEG)
    expectMsgPF(testTimeout) {
      case Status.Failure(t) ⇒
        t.getMessage should include ("Failed to decode image")
    }
  }
}
