import _root_.akka.actor.{Props, ActorSystem}
import _root_.akka.routing.SmallestMailboxRouter
import javax.servlet.ServletContext
import org.ntb.imageresizer.actor.file.{FileCacheImageBrokerActor, DownloadActor, ResizeActor}
import org.ntb.imageresizer.servlet.ImageResizerServlet
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {
  val system = ActorSystem()
  val resizeActor = system.actorOf(Props[ResizeActor].withRouter(SmallestMailboxRouter(2)))
  val downloadActor = system.actorOf(Props[DownloadActor])
  val imageBrokerActor = system.actorOf(Props(classOf[FileCacheImageBrokerActor], downloadActor, resizeActor))

  override def init(context: ServletContext) {
    context.mount(new ImageResizerServlet(system, imageBrokerActor), "/resize/*")
  }

  override def destroy(context:ServletContext) {
    system.shutdown()
  }
}
