import _root_.akka.actor.{Props, ActorSystem}
import _root_.akka.routing.SmallestMailboxRouter
import com.typesafe.config.ConfigFactory
import javax.servlet.ServletContext
import org.ntb.imageresizer.actor.file.{FileCacheImageBrokerActor, DownloadActor, ResizeActor}
import org.ntb.imageresizer.servlet.ImageResizerServlet
import org.slf4j.LoggerFactory
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {
  val logger = LoggerFactory.getLogger(getClass)
  val config = ConfigFactory.parseString("""
    akka {
      akka.loggers = ["akka.event.slf4j.Slf4jLogger"]
      stdout-loglevel = "OFF"
      loglevel = "DEBUG"
    }
  """)
  val system = ActorSystem("ImageResizer", ConfigFactory.load(config))
  val resizeNodes = math.max(Runtime.getRuntime.availableProcessors() - 1, 1)
  val resizeActor = system.actorOf(Props[ResizeActor].withRouter(SmallestMailboxRouter(resizeNodes)))
  val downloadActor = system.actorOf(Props[DownloadActor])
  val imageBroker = system.actorOf(Props(classOf[FileCacheImageBrokerActor], downloadActor, resizeActor))

  override def init(context: ServletContext) {
    val environment = System.getProperty("org.scalatra.environment")
    logger.info(s"Starting application in $environment mode")
    logger.info(s"Deploying $resizeNodes resize actors")
    context.mount(new ImageResizerServlet(system, imageBroker), "/resize/*")
  }

  override def destroy(context:ServletContext) {
    logger.info("Stopping application")
    system.shutdown()
  }
}
