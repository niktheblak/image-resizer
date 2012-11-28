package org.ntb.imageresizer.service

import org.ntb.imageresizer.actor.FileCacheImageBrokerActor._
import org.ntb.imageresizer.imageformat.parseRequestedImageFormat
import org.ntb.imageresizer.service.ServiceUtils.serveFile
import org.ntb.imageresizer.util.FilePathUtils.createTempFile
import org.apache.commons.codec.digest.DigestUtils.md5Hex
import org.apache.http.client.utils.URLEncodedUtils
import com.google.common.io.Files
import akka.actor.Actor
import akka.actor.ActorRef
import akka.dispatch.Future
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration.intToDurationInt
import spray.http._
import spray.http.HttpMethods._
import scala.collection.JavaConversions._
import java.net.URI
import java.nio.charset.Charset

class ImageResizeServiceActor extends Actor {
  implicit val timeout = Timeout(30 seconds)

  def receive = {
    case HttpRequest(GET, url, _, _, _) if url.startsWith("/resize") =>
      val params = extractParams(url)
      val result = for (
        sourceUrl <- params.get("source");
        preferredSize <- params.get("size");
        formatString <- params.get("format");
        format <- parseRequestedImageFormat(formatString);
        mimeType <- MediaTypes.forExtension(format.extension)
      ) yield {
        val imageBroker = context.actorFor("/user/imagebroker")
        val request = GetImageRequest(new URI(sourceUrl), preferredSize.toInt, format)
        val resizeTask = ask(imageBroker, request).mapTo[GetImageResponse]
        (resizeTask, mimeType)
      }
      processResponse(sender, result)
    case HttpRequest(POST, url, _, entity, _) if url.startsWith("/resize") =>
      val params = extractParams(url)
      val buffer = entity.buffer
      val result = for (
        body <- if (buffer.length > 0) Some(buffer) else None;
        preferredSize <- params.get("size");
        formatString <- params.get("format");
        format <- parseRequestedImageFormat(formatString);
        mimeType <- MediaTypes.forExtension(format.extension)
      ) yield {
        val imageBroker = context.actorFor("/user/imagebroker")
        val id = md5Hex(body)
        val tempFile = createTempFile()
        Files.write(body, tempFile)
        val request = GetLocalImageRequest(tempFile, id, preferredSize.toInt, format)
        val resizeTask = ask(imageBroker, request).mapTo[GetImageResponse]
        resizeTask onComplete {
          case _ => tempFile.delete()
        }
        (resizeTask, mimeType)
      }
      processResponse(sender, result)
    case HttpRequest(_, _, _, _, _) => sender ! HttpResponse(status = StatusCodes.BadRequest, entity = "Unknown endpoint")
  }

  def processResponse(client: ActorRef, result: Option[Pair[Future[GetImageResponse], MediaType]]) {
    result match {
      case Some((resizeTask, mimeType)) => resizeTask onComplete {
        case Right(response) =>
          serveFile(response.data, mimeType, client)
        case Left(t) =>
          client ! HttpResponse(status = StatusCodes.InternalServerError, entity = "Error while processing request: " + t.getMessage())
      }
      case None => client ! HttpResponse(status = StatusCodes.BadRequest, entity = "Missing mandatory parameters")
    }
  }

  def extractParams(requestUrl: String): Map[String, String] = {
    val uri = new URI(requestUrl)
    val params = URLEncodedUtils.parse(uri.getQuery(), Charset.forName("UTF-8"))
    params.foldLeft(Map.empty[String, String])((map, param) => map + Pair(param.getName(), param.getValue()))
  }
}