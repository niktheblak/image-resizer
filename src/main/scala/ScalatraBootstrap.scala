import akka.actor.{ActorSystem, Props}
import org.ntb.imageresizer.servlet.ImageResizerServlet
import org.scalatra._
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {
  val system = ActorSystem()
  //val myActor = system.actorOf(Props[MyActor])

  override def init(context: ServletContext) {
    context.mount(new ImageResizerServlet, "/*")
    //context.mount(new MyActorApp(system, myActor), "/actors/*")
  }

  override def destroy(context:ServletContext) {
    system.shutdown()
  }
}
