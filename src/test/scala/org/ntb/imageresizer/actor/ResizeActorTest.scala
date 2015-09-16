package org.ntb.imageresizer.actor

import java.io.File

import akka.actor.{ Status, ActorSystem }
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import akka.util.ByteString
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.io.ByteStringIO
import org.ntb.imageresizer.resize.ResolutionReader
import org.scalatest._

import scala.concurrent.duration._

class ResizeActorTest
    extends TestKit(ActorSystem("TestActorSystem"))
    with ImplicitSender
    with FlatSpecLike
    with Matchers
    with BeforeAndAfter
    with BeforeAndAfterAll
    with ResolutionReader {
  import ResizeActor._

  val sampleImage = new File("src/test/resources/test_image.jpeg")
  val testTimeout = 2.seconds
  var resizeActor: TestActorRef[ResizeActor] = _

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  before {
    resizeActor = TestActorRef(new ResizeActor)
  }

  after {
    resizeActor.stop()
  }

  "ResizeActor" should "resize sample image to requested dimensions" in {
    val source = ByteStringIO.read(sampleImage)
    resizeActor ! ResizeImageRequest(source, 200, JPEG)
    expectMsgPF(testTimeout) {
      case ResizeImageResponse(data) ⇒
        val (width, height) = readResolution(data)
        width shouldEqual 118
        height shouldEqual 200
    }
  }

  it should "throw with invalid image data" in {
    val source = ByteString(1, 2, 3)
    resizeActor ! ResizeImageRequest(source, 200, JPEG)
    expectMsgPF(testTimeout) {
      case Status.Failure(t) ⇒
        t.getMessage should include ("Failed to decode image")
    }
  }
}
