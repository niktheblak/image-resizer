package org.ntb.imageresizer.service

import org.ntb.imageresizer.actor.ResizeActor
import org.ntb.imageresizer.actor.DownloadActor
import org.ntb.imageresizer.actor.FileCacheImageBrokerActor
import akka.actor.Props
import akka.routing.SmallestMailboxRouter
import spray.can.server.SprayCanHttpServerApp
import spray.io.IOServer.Unbind

object ServiceBootstrap extends App with SprayCanHttpServerApp {
  val resizeActor = system.actorOf(Props[ResizeActor].withRouter(SmallestMailboxRouter(2)), "resizer")
  val downloadActor = system.actorOf(Props[DownloadActor], "downloader")
  val imageBrokerActor = system.actorOf(Props[FileCacheImageBrokerActor].withRouter(SmallestMailboxRouter(2)), "imagebroker")
  val service = system.actorOf(Props[ImageResizeServiceActor])
  val httpServer = newHttpServer(service)
  httpServer ! Bind(interface = "localhost", port = 8080)
  
  while (true) {
    Console.print("> ")
    Console.flush()
    val command = Console.readLine()
    if (command == "exit" || command == "quit") {
      httpServer ! Unbind
      system.shutdown()
      sys.exit()
    }
  }
}