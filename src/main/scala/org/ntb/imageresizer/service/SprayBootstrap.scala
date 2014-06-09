package org.ntb.imageresizer.service

import akka.util.Timeout
import akka.actor.{ Props, ActorSystem }
import akka.routing.SmallestMailboxPool
import org.ntb.imageresizer.actor.file.{ FileCacheImageBrokerActor, DownloadActor, ResizeActor }
import spray.routing.SimpleRoutingApp
import org.slf4j.LoggerFactory
import com.typesafe.config.ConfigFactory
import java.util.concurrent.TimeUnit

object SprayBootstrap extends App with SimpleRoutingApp with ImageResizeService {
  val logger = LoggerFactory.getLogger(getClass)
  val config = ConfigFactory.parseString("""
    akka {
      akka.loggers = ["akka.event.slf4j.Slf4jLogger"]
      stdout-loglevel = "OFF"
      loglevel = "INFO"
    }
  """)
  implicit val timeout = Timeout(30, TimeUnit.SECONDS)
  implicit val system = ActorSystem("image-resizer", ConfigFactory.load(config))
  implicit val context = system.dispatcher
  val resizeNodes = math.max(Runtime.getRuntime.availableProcessors() - 1, 1)
  println(s"Deploying $resizeNodes resize actors")
  val resizeActor = system.actorOf(Props[ResizeActor].withRouter(SmallestMailboxPool(resizeNodes)))
  val downloadActor = system.actorOf(Props[DownloadActor])
  val imageBroker = system.actorOf(Props(classOf[FileCacheImageBrokerActor], downloadActor, resizeActor))

  startServer(interface = "localhost", port = 8080)(resizeRoute)
}
