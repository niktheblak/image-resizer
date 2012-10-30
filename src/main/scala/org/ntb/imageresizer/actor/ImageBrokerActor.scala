package org.ntb.imageresizer.actor

import java.net.URI

import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Status
import akka.dispatch.Await
import akka.dispatch.Promise
import akka.pattern.ask
import akka.util.duration.intToDurationInt
import akka.util.ByteString
import akka.util.Timeout

import org.apache.http.HttpException
import org.ntb.imageresizer.resize.ResizingImageDownloader
import org.ntb.imageresizer.resize.UnsupportedImageFormatException
import CacheActor._

class ImageBrokerActor extends Actor with ActorLogging with ResizingImageDownloader {
  import context.dispatcher
  import ImageBrokerActor._
  implicit val timeout = Timeout(10 seconds)

  def receive = {
    case GetImageRequest(uri, preferredSize) =>
      val cacheActor = context.actorFor("/user/cache")
      val getFromCacheTask = cacheActor ? GetFromCacheRequest(uri, preferredSize)
      val getResizedPictureTask = getFromCacheTask flatMap {
        case DataFromCacheResponse(data) =>
          log.info("Picture %s was already cached, sending cached version".format(uri))
          Promise.successful(data)
        case NotCachedResponse =>
          log.info("Picture %s was not cached, downloading and resizing picture".format(uri))
          downloadAndResize(uri, preferredSize) map {
            case resizedData: ByteString =>
              cacheActor ! InsertToCacheRequest(uri, resizedData, preferredSize)
              resizedData
          }
      }
      try {
        val response = Await.result(getResizedPictureTask, timeout.duration)
        sender ! GetImageResponse(response)
      } catch {
        case e: UnsupportedImageFormatException =>
          sender ! Status.Failure(e)
        case e: HttpException =>
          sender ! Status.Failure(e)
        case e: Exception =>
          sender ! Status.Failure(e)
          throw e
      }
  }
}

object ImageBrokerActor {
  case class GetImageRequest(uri: URI, preferredSize: Int)
  case class GetImageResponse(data: ByteString)
}