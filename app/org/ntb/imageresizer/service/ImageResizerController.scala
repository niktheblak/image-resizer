package org.ntb.imageresizer.service

import javax.inject._

import akka.pattern.ask
import akka.util.Timeout
import org.ntb.imageresizer.actor.DownloadActor._
import org.ntb.imageresizer.actor.ImageBrokerActor._
import org.ntb.imageresizer.imageformat._
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.duration._

class ImageResizerController @Inject() (imageBrokerSystem: ImageBrokerSystem) extends Controller {
  import play.api.libs.concurrent.Execution.Implicits._
  implicit val timeout: Timeout = 10.seconds

  def resize(source: String, size: Int, format: String) = Action.async {
    val imageFormat = parseRequestedImageFormat(format).getOrElse(JPEG)
    val request = GetImageRequest(source, size, imageFormat)
    val resizeTask = ask(imageBrokerSystem.imageBroker, request).mapTo[GetImageResponse]
    resizeTask.map { r =>
      Ok(r.data).as(imageFormat.mimeType)
    } recoverWith {
      case e: IllegalArgumentException => Future(BadRequest(e.getMessage))
      case e: DownloadException => Future(BadGateway(e.getMessage))
      case e: RuntimeException => Future(InternalServerError(e.getMessage))
    }
  }
}
