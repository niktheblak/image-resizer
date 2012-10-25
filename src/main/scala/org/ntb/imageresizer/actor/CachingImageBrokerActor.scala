package org.ntb.imageresizer.actor

import java.net.URI
import java.util.concurrent.ExecutionException

import org.apache.http.HttpException
import org.ntb.imageresizer.cache.TempFileCacher
import org.ntb.imageresizer.resize.ResizingImageDownloader
import org.ntb.imageresizer.resize.UnsupportedImageFormatException

import com.google.common.util.concurrent.UncheckedExecutionException

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Status
import akka.actor.actorRef2Scala
import akka.dispatch.Await
import akka.util.ByteString
import akka.util.Timeout
import akka.util.duration.intToDurationInt

class CachingImageBrokerActor extends Actor with ActorLogging with ResizingImageDownloader with TempFileCacher[(URI, Int)] {
  import context.dispatcher
  implicit val timeout = Timeout(10 seconds)

  def receive = {
    case GetImageRequest(uri, preferredSize) =>
      try {
        log.info("Trying to get picture %s from cache".format(uri))
        val data = cache.get((uri, preferredSize), loadImage(uri, preferredSize))
        sender ! GetImageResponse(data)
      } catch {
        case e: Throwable =>
          handleExceptions(e, sender)
      }
  }

  override def postStop() {
    log.info("Cleaning cache directory")
    clearCacheDirectory()
  }
  
  def loadImage(uri: URI, preferredSize: Int)(): ByteString = {
    log.info("Picture %s not cached, downloading and resizing".format(uri))
    val task = downloadAndResize(uri, preferredSize)
    Await.result(task, timeout.duration)
  }

  def handleExceptions(e: Throwable, sender: ActorRef) =
    e match {
      case e: ExecutionException =>
        e.getCause() match {
          case e: UnsupportedImageFormatException =>
            sender ! Status.Failure(e)
          case e: HttpException =>
            sender ! Status.Failure(e)
          case e: Throwable =>
            sender ! Status.Failure(e)
            throw e
        }
      case e: UncheckedExecutionException =>
        val cause = e.getCause()
        sender ! Status.Failure(cause)
        throw cause
      case e: Throwable =>
        sender ! Status.Failure(e)
        throw e
    }
}