package org.ntb.imageresizer.service

import spray.can.server.HttpServer
import spray.io._
import akka.actor._
import org.ntb.imageresizer.actor.ResizeActor
import org.ntb.imageresizer.actor.DownloadActor
import org.ntb.imageresizer.actor.FileCacheImageBrokerActor
import akka.routing.SmallestMailboxRouter

object ServiceBootstrap extends App {
  val system = ActorSystem("image-resizer")
  val ioBridge = new IOBridge(system).start()
  val resizeActor = system.actorOf(Props[ResizeActor].withRouter(SmallestMailboxRouter(2)), "resizer")
  val downloadActor = system.actorOf(Props[DownloadActor], "downloader")
  val imageBrokerActor = system.actorOf(Props[FileCacheImageBrokerActor].withRouter(SmallestMailboxRouter(2)), "imagebroker")
  val handler = system.actorOf(Props[SprayRoutingImageResizeServiceActor])
  val server = system.actorOf(
    props = Props(new HttpServer(ioBridge, SingletonHandler(handler))),
    name = "http-server")
  server ! HttpServer.Bind("localhost", 8080)
  while (true) {
    Console.print("> ")
    Console.flush()
    val command = Console.readLine()
    if (command == "exit" || command == "quit") {
      ioBridge.stop()
      system.shutdown()
      sys.exit()
    }
  }
}