package org.ntb.imageresizer

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.google.common.io.Files
import org.ntb.imageresizer.actor.ImageBrokerActor._
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.util.DefaultHasher
import org.ntb.imageresizer.util.FileUtils.createTempFile
import spray.http.HttpHeaders._
import spray.http._
import spray.httpx.unmarshalling._
import spray.routing._

import scala.concurrent.ExecutionContext

trait ImageResizeService extends HttpService with DefaultHasher {
  import scala.language.postfixOps

  implicit val context: ExecutionContext
  implicit val timeout: Timeout
  val imageBroker: ActorRef

  implicit val String2ImageFormatConverter = new FromStringDeserializer[ImageFormat] {
    def apply(value: String) = {
      parseRequestedImageFormat(value) match {
        case Some(format) ⇒ Right(format)
        case None ⇒ Left(MalformedContent(s"Unsupported image format: $value"))
      }
    }
  }

  val resizeRoute = path("resize") {
    (get & parameters('source.as[String], 'size.as[Int], 'format.as[ImageFormat] ?)) {
      (source, size, format) ⇒
        val imageFormat = format.getOrElse(JPEG)
        val mediaType = MediaTypes.forExtension(imageFormat.extension).get
        val request = GetImageRequest(source, size, imageFormat)
        val resizeTask = ask(imageBroker, request).mapTo[GetImageResponse]
        val result = resizeTask map { response ⇒
          response.data
        }
        respondWithMediaType(mediaType) {
          complete(result)
        }
    } ~ (post & parameters('size.as[Int], 'format.as[ImageFormat] ?)) {
      (size, format) ⇒
        entity(as[Array[Byte]]) { body ⇒
          val imageFormat = format.getOrElse(JPEG)
          val mediaType = MediaTypes.forExtension(imageFormat.extension).get
          val id = hashBytes(body)
          val tempFile = createTempFile()
          Files.write(body, tempFile)
          val request = GetLocalImageRequest(tempFile, id, size, imageFormat)
          val resizeTask = ask(imageBroker, request).mapTo[GetImageResponse]
          resizeTask onComplete {
            case _ ⇒ tempFile.delete()
          }
          val result = resizeTask map { response ⇒
            response.data
          }
          respondWithMediaType(mediaType) {
            val loc = Uri(s"/resize?source=$id&size=$size&format=$imageFormat")
            respondWithHeader(Location(loc)) {
              complete(result)
            }
          }
        }
    }
  }
}
