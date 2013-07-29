package org.ntb.imageresizer.servlet

import _root_.akka.actor.{ActorSystem, ActorRef}
import _root_.akka.pattern.ask
import _root_.akka.util.Timeout
import com.google.common.hash.Hashing
import com.google.common.io.{Files, ByteStreams}
import org.slf4j.LoggerFactory
import java.net.URI
import language.postfixOps
import org.ntb.imageresizer.actor.file.FileCacheImageBrokerActor._
import org.ntb.imageresizer.imageformat._
import org.ntb.imageresizer.util.FileUtils
import org.scalatra._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ImageResizerServlet(val actorSystem: ActorSystem, val imageBroker: ActorRef) extends ScalatraServlet with FutureSupport {
  val logger = LoggerFactory.getLogger(getClass)
  implicit val timeout: Timeout = 30 seconds
  protected implicit def executor: ExecutionContext = actorSystem.dispatcher

  get("/") {
    val source: String = params("source")
    val size: Int = params.getOrElse("size", halt(400)).toInt
    val format: String = params.getOrElse("format", "jpeg")
    val imageFormat = parseRequestedImageFormat(format).getOrElse(halt(400))
    contentType = imageFormat.mimeType
    val req = GetImageRequest(URI.create(source), size, imageFormat)
    val resizeTask = ask(imageBroker, req).mapTo[GetImageResponse]
    new AsyncResult { val is =
      resizeTask map { r ⇒
        Ok(r.data)
      } recover {
        case e: Exception ⇒
          logger.error("Error while resizing image", e)
          InternalServerError(reason = e.getMessage)
      }
    }
  }

  post("/") {
    val size: Int = params.getOrElse("size", halt(400)).toInt
    val format: String = params.getOrElse("format", "jpeg")
    val imageFormat = parseRequestedImageFormat(format).getOrElse(halt(400))
    contentType = imageFormat.mimeType
    val tempFile = FileUtils.createTempFile()
    ByteStreams.copy(request.inputStream, Files.newOutputStreamSupplier(tempFile))
    val id = Files.hash(tempFile, Hashing.goodFastHash(32)).toString
    val req = GetLocalImageRequest(tempFile, id, size, imageFormat)
    val resizeTask = ask(imageBroker, req).mapTo[GetImageResponse]
    new AsyncResult { val is =
      resizeTask map { r ⇒
        Ok(r.data)
      } recover {
        case e: Exception ⇒
          logger.error("Error while resizing image", e)
          InternalServerError(reason = e.getMessage)
      }
    }
  }
}
