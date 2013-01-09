package org.ntb.imageresizer.service

import org.ntb.imageresizer.actor.ResizeActor
import org.ntb.imageresizer.actor.DownloadActor
import org.ntb.imageresizer.actor.FileCacheImageBrokerActor
import akka.actor.Props
import akka.routing.SmallestMailboxRouter
import spray.can.server.SprayCanHttpServerApp
import spray.io.IOServer.Unbind

object ServiceBootstrap extends App with SprayCanHttpServerApp {
  val resizeActor = system.actorOf(Props[ResizeActor].withRouter(SmallestMailboxRouter(2)))
  val downloadActor = system.actorOf(Props[DownloadActor])
  val imageBrokerActor = system.actorOf(Props(new FileCacheImageBrokerActor(downloadActor, resizeActor)).withRouter(SmallestMailboxRouter(2)))
  val service = system.actorOf(Props(new ImageResizeServiceActor(imageBrokerActor)))
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