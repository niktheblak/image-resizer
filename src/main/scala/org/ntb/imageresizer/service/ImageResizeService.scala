package org.ntb.imageresizer.service

import org.ntb.imageresizer.actor.FileCacheImageBrokerActor._
import akka.pattern.ask
import akka.actor._
import akka.util.Timeout
import akka.util.duration.intToDurationInt
import spray.can.server.HttpServer
import spray.util._
import spray.http._
import HttpMethods._
import MediaTypes._
import java.net.URI

class ImageResizeService extends Actor {
  implicit val timeout = Timeout(10 seconds)
  
  def receive = {
    case request@HttpRequest(GET, "/resize", _, _, _) =>
      request.queryParams.get("source") match {
        case Some(sourceUrl) =>
          val imageBroker = context.actorFor("/user/imagebroker")
          val preferredSize = request.queryParams.getOrElse("size", "200").toInt
          val resizeTask = ask(imageBroker, GetImageRequest(new URI(sourceUrl), preferredSize)).mapTo[GetImageResponse]
          resizeTask onComplete {
            case Right(response) =>
              val data = Array[Byte]()
              sender ! HttpResponse(entity = HttpBody(MediaTypes.`image/jpeg`, data))
            case Left(t) =>
              sender ! HttpResponse(status = StatusCodes.InternalServerError, entity = "Error while processing request: " + t.getMessage())
          }
        case None => sender ! HttpResponse(status = StatusCodes.BadRequest, entity = "Missing URL parameter \"source\"")
      }
  }
}