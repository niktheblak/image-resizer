package org.ntb.imageresizer.service

import akka.actor.Props
import akka.routing.SmallestMailboxRouter
import org.ntb.imageresizer.actor.file.{ResizeActor, FileCacheImageBrokerActor, DownloadActor}
import spray.can.server.SprayCanHttpServerApp
import spray.io.IOServer.Unbind

object ServiceBootstrap extends App with SprayCanHttpServerApp {
  val resizeActor = system.actorOf(Props[ResizeActor].withRouter(SmallestMailboxRouter(2)))
  val downloadActor = system.actorOf(Props[DownloadActor])
  val imageBrokerActor = system.actorOf(Props(new FileCacheImageBrokerActor(downloadActor, resizeActor)))
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