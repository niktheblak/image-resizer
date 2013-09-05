package org.ntb.imageresizer.service

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.google.common.io.Files
import java.net.MalformedURLException
import java.net.URI
import language.postfixOps
import org.apache.commons.codec.digest.DigestUtils.md5Hex
import org.ntb.imageresizer.actor.file.FileCacheImageBrokerActor._
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.util.FileUtils.createTempFile
import spray.http._
import spray.httpx.unmarshalling._
import spray.routing._
import spray.util.pimpFile
import scala.concurrent.ExecutionContext

trait ImageResizeService extends HttpService {
  implicit val context: ExecutionContext
  implicit val timeout: Timeout
  val chunkSize = 0xFFFF
  val imageBroker: ActorRef

  implicit val String2URIConverter = new FromStringDeserializer[URI] {
    def apply(value: String) = {
      try Right(new URI(value))
      catch {
        case e: MalformedURLException ⇒ Left(MalformedContent(e.getMessage, e))
      }
    }
  }

  implicit val String2ImageFormatConverter = new FromStringDeserializer[ImageFormat] {
    def apply(value: String) = {
      parseRequestedImageFormat(value) match {
        case Some(format) ⇒ Right(format)
        case None ⇒ Left(MalformedContent(s"Unsupported image format: $value"))
      }
    }
  }

  val resizeRoute = path("resize") {
    (get & parameters('source.as[URI], 'size.as[Int], 'format.as[ImageFormat] ?)) {
      (source, size, format) ⇒
        val imageFormat = format.getOrElse(JPEG)
        val mediaType = MediaTypes.forExtension(imageFormat.extension).get
        val request = GetImageRequest(source, size, imageFormat)
        val resizeTask = ask(imageBroker, request).mapTo[GetImageResponse]
        val result = resizeTask map { response ⇒
          response.data.toByteArrayStream(chunkSize)
        }
        respondWithMediaType(mediaType) {
          complete(result)
        }
    } ~ (post & parameters('size.as[Int], 'format.as[ImageFormat] ?)) {
      (size, format) ⇒
        entity(as[Array[Byte]]) { body ⇒
          val imageFormat = format.getOrElse(JPEG)
          val mediaType = MediaTypes.forExtension(imageFormat.extension).get
          val id = md5Hex(body)
          val tempFile = createTempFile()
          Files.write(body, tempFile)
          val request = GetLocalImageRequest(tempFile, id, size, imageFormat)
          val resizeTask = ask(imageBroker, request).mapTo[GetImageResponse]
          resizeTask onComplete {
            case _ ⇒ tempFile.delete()
          }
          val result = resizeTask map { response ⇒
            response.data.toByteArrayStream(chunkSize)
          }
          respondWithMediaType(mediaType) {
            complete(result)
          }
        }
    }
  }
}
