package controllers

import javax.inject._

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import org.ntb.imageresizer.actor.DownloadException
import org.ntb.imageresizer.actor.ImageBrokerActor._
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.util.FileHasher
import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

@Singleton
class ImageResizerController @Inject() (@Named("imagebroker") imageBroker: ActorRef, implicit val ec: ExecutionContext) extends InjectedController with FileHasher {
  implicit val timeout: Timeout = 10.seconds

  def resize(source: String, size: Int, format: String) = Action.async {
    val imageFormat = parseRequestedImageFormat(format).getOrElse(JPEG)
    val request = GetImageRequest(source, size, imageFormat)
    val resizeTask = ask(imageBroker, request).mapTo[GetImageResponse]
    resizeTask.map { r =>
      val location = s"/resize?source=$source&size=$size&format=${imageFormat.extension}"
      Ok(r.data)
        .as(imageFormat.mimeType)
        .withHeaders(LOCATION → location)
    } recoverWith {
      case e: IllegalArgumentException => Future(BadRequest(e.getMessage))
      case e: DownloadException => Future(BadGateway(e.getMessage))
      case e: RuntimeException => Future(InternalServerError(e.getMessage))
    }
  }

  def resizeFromBody(id: Option[String], size: Int, format: String) = Action.async(parse.temporaryFile) { request =>
    val imageFormat = parseRequestedFormat(request.headers)
    val body = request.body
    val finalId = id match {
      case Some(i) => i
      case None => hash(body.file)
    }
    val message = GetLocalImageRequest(body.file, finalId, size, imageFormat)
    val resizeTask = ask(imageBroker, message).mapTo[GetImageResponse]
    resizeTask onComplete { _ =>
      body.delete()
    }
    resizeTask.map { r =>
      val location = s"/resize?source=$finalId&size=$size&format=${imageFormat.extension}"
      Ok(r.data)
        .as(imageFormat.mimeType)
        .withHeaders(LOCATION → location)
    } recoverWith {
      case e: IllegalArgumentException => Future(BadRequest(e.getMessage))
      case e: DownloadException => Future(BadGateway(e.getMessage))
      case e: RuntimeException => Future(InternalServerError(e.getMessage))
    }
  }

  def parseRequestedFormat(headers: Headers): ImageFormat = {
    val format = for {
      m ← headers.get(ACCEPT).orElse(headers.get(CONTENT_TYPE))
      f ← parseImageFormatFromMimeType(m)
    } yield f
    format.getOrElse(JPEG)
  }
}
