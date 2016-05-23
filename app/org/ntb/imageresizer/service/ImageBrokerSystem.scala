package org.ntb.imageresizer.service

import javax.imageio.ImageIO
import javax.inject._

import akka.actor.{ActorSystem, Props}
import akka.routing.BalancingPool
import org.ntb.imageresizer.actor.{DownloadActor, ImageBrokerActor, ResizeActor}
import play.api.libs.ws.WSClient

@Singleton
class ImageBrokerSystem @Inject() (system: ActorSystem, ws: WSClient) {
  ImageIO.setUseCache(false)
  val resizeNodes = math.max(Runtime.getRuntime.availableProcessors() - 1, 1)
  val resizeActor = system.actorOf(BalancingPool(resizeNodes).props(Props[ResizeActor]), "resizer")
  val downloadActor = system.actorOf(Props(classOf[DownloadActor], ws).withMailbox("bounded-mailbox"), "downloader")
  val imageBroker = system.actorOf(Props(classOf[ImageBrokerActor], downloadActor, resizeActor), "imagebroker")
}
