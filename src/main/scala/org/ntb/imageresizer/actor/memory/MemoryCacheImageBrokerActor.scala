package org.ntb.imageresizer.actor.memory

import akka.actor.{ActorRef, Actor}
import akka.pattern.ask
import akka.util.{Timeout, ByteString}
import concurrent.Await
import concurrent.duration._
import java.net.URI
import language.postfixOps
import org.ntb.imageresizer.actor.ActorUtils
import org.ntb.imageresizer.actor.memory.DownloadActor._
import org.ntb.imageresizer.actor.memory.ResizeActor._
import org.ntb.imageresizer.cache.GuavaMemoryCache
import org.ntb.imageresizer.imageformat._
import java.io.IOException

class MemoryCacheImageBrokerActor(downloadActor: ActorRef, resizeActor: ActorRef) extends Actor
    with GuavaMemoryCache[MemoryCacheImageBrokerActor.Key, ByteString]
    with ActorUtils {
  import MemoryCacheImageBrokerActor._
  import context.dispatcher

  implicit val timeout: Timeout = 30 seconds
  override val maxCacheSize = 10L * 1024L * 1024L

  def receive = {
    case GetImageRequest(uri, preferredSize, imageFormat) ⇒
      requireArgument(sender)(preferredSize > 0, "Size must be positive")
      val source = uri.toString
      val key = (source, preferredSize, imageFormat)
      get(key) match {
        case Some(data) ⇒ sender ! GetImageResponse(data)
        case None ⇒
          val downloadAndResizeTask = for (
            downloadResponse <- ask(downloadActor, DownloadRequest(uri)).mapTo[DownloadResponse];
            resizeResponse <- ask(resizeActor, ResizeImageRequest(downloadResponse.data, preferredSize, imageFormat)).mapTo[ResizeImageResponse]
          ) yield resizeResponse.data
          actorTry(sender) {
            val data = Await.result(downloadAndResizeTask, timeout.duration)
            put(key, data)
            sender ! GetImageResponse(data)
          } actorCatch {
            case e: IOException ⇒
          }
      }
  }
}

object MemoryCacheImageBrokerActor {
  type Key = (String, Int, ImageFormat)
  case class GetImageRequest(uri: URI, preferredSize: Int, format: ImageFormat = JPEG)
  case class GetImageResponse(data: ByteString)
}
