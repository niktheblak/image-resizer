import _root_.akka.actor.{Props, ActorSystem}
import _root_.akka.routing.SmallestMailboxRouter
import javax.servlet.ServletContext
import org.ntb.imageresizer.actor.file.{FileCacheImageBrokerActor, DownloadActor, ResizeActor}
import org.ntb.imageresizer.servlet.ImageResizerServlet
import org.slf4j.LoggerFactory
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {
  val logger = LoggerFactory.getLogger(getClass)
  val system = ActorSystem()
  val resizeNodes = math.max(Runtime.getRuntime.availableProcessors() - 1, 1)
  val resizeActor = system.actorOf(Props[ResizeActor].withRouter(SmallestMailboxRouter(resizeNodes)))
  val downloadActor = system.actorOf(Props[DownloadActor])
  val imageBrokerActor = system.actorOf(Props(classOf[FileCacheImageBrokerActor], downloadActor, resizeActor).withRouter(SmallestMailboxRouter(resizeNodes)))

  override def init(context: ServletContext) {
    logger.info("Starting application")
    logger.info(s"Deploying $resizeNodes resize actors")
    logger.info(s"Deploying $resizeNodes image broker actors")
    context.mount(new ImageResizerServlet(system, imageBrokerActor), "/resize/*")
  }

  override def destroy(context:ServletContext) {
    logger.info("Stopping application")
    system.shutdown()
  }
}
