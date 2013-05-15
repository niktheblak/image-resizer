package org.ntb.imageresizer.servlet

import _root_.akka.actor.{ActorSystem, ActorRef}
import _root_.akka.pattern.ask
import _root_.akka.util.Timeout
import java.net.URI
import language.postfixOps
import org.ntb.imageresizer.actor.file.FileCacheImageBrokerActor._
import org.ntb.imageresizer.imageformat._
import org.scalatra._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ImageResizerServlet(val actorSystem: ActorSystem, val imageBroker: ActorRef) extends ScalatraServlet with FutureSupport {
  implicit val timeout = Timeout(30 seconds)
  protected implicit def executor: ExecutionContext = actorSystem.dispatcher

  get("/resize") {
    val source: String = params("source")
    val size: Int = params.getOrElse("size", halt(400)).toInt
    val format: String = params.getOrElse("format", "jpeg")
    val imageFormat = parseRequestedImageFormat(format).getOrElse(halt(400))
    contentType = imageFormat.mimeType
    val req = GetImageRequest(URI.create(source), size, imageFormat)
    val resizeTask = ask(imageBroker, req).mapTo[GetImageResponse]
    new AsyncResult { def is =
      resizeTask.map { response =>
        Ok(response.data)
      }
    }
  }

  get("/") {
    <html>
      <body>
        <h1>Hello, world!</h1>
        Say hello to Scalate.
      </body>
    </html>
  }
}
