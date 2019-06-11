import javax.imageio.ImageIO

import akka.actor.Props
import akka.routing.BalancingPool
import com.google.inject.AbstractModule
import org.ntb.imageresizer.actor.{ ImageBrokerActor, ResizeActor }
import play.api.libs.concurrent.AkkaGuiceSupport

class Module extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    ImageIO.setUseCache(false)
    val resizeNodes = math.max(Runtime.getRuntime.availableProcessors() - 1, 1)
    bindActor[ResizeActor]("resizer", _ => BalancingPool(resizeNodes).props(Props[ResizeActor]))
    bindActor[ImageBrokerActor]("imagebroker")
  }
}
