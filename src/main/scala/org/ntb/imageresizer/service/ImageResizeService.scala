package org.ntb.imageresizer.service

import java.net.URI

import org.ntb.imageresizer.actor.FileCacheImageBrokerActor.GetImageRequest
import org.ntb.imageresizer.actor.FileCacheImageBrokerActor.GetImageResponse
import org.ntb.imageresizer.imageformat.parseRequestedImageFormat
import org.ntb.imageresizer.service.ServiceUtils.serveFile

import akka.actor.Actor
import akka.actor.actorRef2Scala
import akka.dispatch.Await
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration.intToDurationInt
import spray.http._
import spray.http.HttpMethods._
import spray.http.MediaTypes
import spray.http.StatusCodes._
import spray.routing._

class ImageResizeServiceActor extends Actor with ImageResizeService {
  def actorRefFactory = context
  def receive = runRoute(resizeRoute)
}

trait ImageResizeService extends HttpService {
  implicit val timeout = Timeout(10 seconds)
  val resizePath = path("resize") & parameters('source.as[String], 'size.as[Int], 'format.as[String] ? "jpeg")
  val resizeRoute = resizePath { (source, size, format) =>
    get {
      val result = for (
        imageFormat <- parseRequestedImageFormat(format);
        mediaType <- MediaTypes.forExtension(imageFormat.extension)
      ) yield {
        val imageBroker = actorSystem.actorFor("/user/imagebroker")
        val request = GetImageRequest(new URI(source), size, imageFormat)
        val resizeTask = ask(imageBroker, request).mapTo[GetImageResponse]
        (resizeTask, mediaType)
      }
      detachTo(singleRequestServiceActor) {
        result match {
          case Some((resizeTask, mediaType)) =>
            val response = Await.result(resizeTask, timeout.duration)
            respondWithMediaType(mediaType) {
              getFromFile(response.data)
            }
          case None => complete(BadRequest, "")
        }
      }
    }
  }
}

class OldImageResizeServiceActor extends Actor {
  implicit val timeout = Timeout(10 seconds)
  
  def receive = {
    case request @ HttpRequest(GET, uri, _, _, _) if uri.startsWith("/resize") =>
      val client = sender
      val result = for (
        sourceUrl <- request.queryParams.get("source");
        preferredSize <- request.queryParams.get("size");
        formatString <- request.queryParams.get("format");
        format <- parseRequestedImageFormat(formatString);
        mimeType <- MediaTypes.forExtension(format.extension)
      ) yield {
        val imageBroker = context.actorFor("/user/imagebroker")
        val request = GetImageRequest(new URI(sourceUrl), preferredSize.toInt, format)
        val resizeTask = ask(imageBroker, request).mapTo[GetImageResponse]
        (resizeTask, mimeType)
      }
      result match {
        case Some((resizeTask, mimeType)) => resizeTask onComplete {
          case Right(response) =>
            serveFile(response.data, mimeType, client)
          case Left(t) =>
            client ! HttpResponse(status = StatusCodes.InternalServerError, entity = "Error while processing request: " + t.getMessage())
        }
        case None => client ! HttpResponse(status = StatusCodes.BadRequest, entity = "Missing mandatory parameters")
      }
    case HttpRequest(_, _, _, _, _) => sender ! HttpResponse(status = StatusCodes.BadRequest, entity = "Unknown endpoint")
  }
}