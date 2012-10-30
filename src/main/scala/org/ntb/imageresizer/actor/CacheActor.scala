package org.ntb.imageresizer.actor

import java.net.URI

import org.ntb.imageresizer.cache.TempFileCacheProvider

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.actorRef2Scala
import akka.util.ByteString

class CacheActor extends Actor with ActorLogging with TempFileCacheProvider[Tuple2[URI, Int]] {
  import CacheActor._
  
  override def cacheDirectoryName: String = self.path.name
  
  def receive = {
    case InsertToCacheRequest(uri, data, size) =>
      log.info("Caching %s, %d pixels, %d bytes".format(uri, data, size))
      cache.put((uri, size), data)
    case GetFromCacheRequest(uri, size) =>
      cache.get((uri, size)) match {
        case Some(data) => sender ! DataFromCacheResponse(data)
        case None => sender ! NotCachedResponse
      }
  }

  override def postStop() {
    log.info("Cleaning cache")
    clearCacheDirectory()
  }
}

object CacheActor {
  case class InsertToCacheRequest(url: URI, data: ByteString, size: Int)
  case class GetFromCacheRequest(url: URI, size: Int)
  case class DataFromCacheResponse(data: ByteString)
  case object NotCachedResponse
}