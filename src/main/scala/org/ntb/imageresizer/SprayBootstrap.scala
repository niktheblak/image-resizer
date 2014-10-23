package org.ntb.imageresizer

import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

import akka.actor.{ ActorSystem, Props }
import akka.routing.BalancingPool
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.ntb.imageresizer.actor.{ DownloadActor, ImageBrokerActor, ResizeActor }
import spray.routing.SimpleRoutingApp

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn

object SprayBootstrap extends App with SimpleRoutingApp with ImageResizeService {
  val config = ConfigFactory.parseString("""
    bounded-mailbox {
      mailbox-type = "akka.dispatch.BoundedMailbox"
      mailbox-capacity = 50
    }
    akka {
      actor.mailbox.requirements {
        "akka.dispatch.BoundedMessageQueueSemantics" = bounded-mailbox
      }
      akka.loggers = ["akka.event.slf4j.Slf4jLogger"]
      stdout-loglevel = "OFF"
      loglevel = "INFO"
      log-dead-letters-during-shutdown = false
    }
  """)
  implicit val timeout = Timeout(30, TimeUnit.SECONDS)
  implicit val system = ActorSystem("image-resizer", ConfigFactory.load(config))
  implicit val context = system.dispatcher
  ImageIO.setUseCache(false)
  val log = akka.event.Logging(system, "SprayBootstrap")
  val resizeNodes = math.max(Runtime.getRuntime.availableProcessors() - 1, 1)
  log.info("Deploying {} resize actors", resizeNodes)
  val resizeActor = system.actorOf(BalancingPool(resizeNodes).props(Props[ResizeActor]), "resizer")
  val downloadActor = system.actorOf(Props[DownloadActor].withMailbox("bounded-mailbox"), "downloader")
  val imageBroker = system.actorOf(Props(classOf[ImageBrokerActor], downloadActor, resizeActor), "imagebroker")

  val startTask = startServer(interface = "localhost", port = 8080)(resizeRoute)
  val boundEvent = Await.result(startTask, 10.seconds)

  println(s"Server started at ${boundEvent.localAddress}. Press ENTER to quit...")
  StdIn.readLine()
  system.stop(imageBroker)
  system.shutdown()
}
