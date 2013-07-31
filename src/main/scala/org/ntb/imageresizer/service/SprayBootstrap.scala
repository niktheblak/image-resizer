package org.ntb.imageresizer.service

import akka.util.Timeout
import akka.actor.{Props, ActorSystem}
import akka.routing.SmallestMailboxRouter
import org.ntb.imageresizer.actor.file.{FileCacheImageBrokerActor, DownloadActor, ResizeActor}
import spray.routing.SimpleRoutingApp
import org.slf4j.LoggerFactory
import com.typesafe.config.ConfigFactory
import java.util.concurrent.TimeUnit
import language.postfixOps

class SprayBootstrap extends App with SimpleRoutingApp with ImageResizeService {
  val logger = LoggerFactory.getLogger(getClass)
  val config = ConfigFactory.parseString("""
    akka {
      akka.loggers = ["akka.event.slf4j.Slf4jLogger"]
      stdout-loglevel = "OFF"
      loglevel = "DEBUG"
    }
  """)
  implicit val timeout = Timeout(30, TimeUnit.SECONDS)
  implicit val system = ActorSystem("image-resizer", ConfigFactory.load(config))
  implicit val context = system.dispatcher
  val resizeActor = system.actorOf(Props[ResizeActor].withRouter(SmallestMailboxRouter(2)))
  val downloadActor = system.actorOf(Props[DownloadActor])
  val imageBroker = system.actorOf(Props(new FileCacheImageBrokerActor(downloadActor, resizeActor)))

  startServer(interface = "localhost", port = 8080)(resizeRoute)
}
