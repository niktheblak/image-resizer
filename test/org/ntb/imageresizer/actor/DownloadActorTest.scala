package org.ntb.imageresizer.actor

import akka.actor.{ActorSystem, Status}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.ByteString
import com.google.common.io.ByteStreams
import org.scalatest._
import play.api.mvc._
import play.api.routing.sird._
import play.api.test.WsTestClient
import play.core.server.Server

import scala.concurrent.duration._

class DownloadActorTest
    extends TestKit(ActorSystem("TestActorSystem"))
    with ImplicitSender
    with FlatSpecLike
    with Matchers
    with BeforeAndAfterAll {
  import DownloadActor._

  val testTimeout = 2.seconds

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "DownloadActor" should "download file from valid URL" in {
    Server.withRouter() {
      case GET(p"/test_image.jpeg") => Action {
        Results.Ok.sendResource("test_image.jpeg").as("image/jpeg")
      }
    } { port =>
      WsTestClient.withClient { c =>
        val downloadActor = TestActorRef(new DownloadActor(c))
        downloadActor ! DownloadRequest(s"http://localhost:${port.value}/test_image.jpeg")
        expectMsgPF(testTimeout) {
          case DownloadResponse(data) =>
            val data = ByteStreams.toByteArray(getClass.getClassLoader.getResourceAsStream("test_image.jpeg"))
            data shouldEqual ByteString(data)
        }
      }
    }
  }

  it should "return error if download failed" in {
    Server.withRouter() {
      case GET(p"/test_image.jpeg") => Action {
        Results.NotFound
      }
    } { port =>
      WsTestClient.withClient { c =>
        val downloadActor = TestActorRef(new DownloadActor(c))
        downloadActor ! DownloadRequest(s"http://localhost:${port.value}/test_image.jpeg")
        expectMsgPF(testTimeout) {
          case Status.Failure(t) =>
            t.getMessage should startWith ("Server responded with HTTP 404 Not Found")
        }
      }
    }
  }
}