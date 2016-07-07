import javax.imageio.ImageIO

import akka.actor.Props
import akka.routing.BalancingPool
import com.google.inject.AbstractModule
import org.ntb.imageresizer.actor.{ DownloadActor, ImageBrokerActor, ResizeActor }
import play.api.libs.concurrent.AkkaGuiceSupport

class Module extends AbstractModule with AkkaGuiceSupport {
  def configure() = {
    ImageIO.setUseCache(false)
    val resizeNodes = math.max(Runtime.getRuntime.availableProcessors() - 1, 1)
    bindActor[ResizeActor]("resizer", _ ⇒ BalancingPool(resizeNodes).props(Props[ResizeActor]))
    bindActor[DownloadActor]("downloader", props ⇒ props.withMailbox("bounded-mailbox"))
    bindActor[ImageBrokerActor]("imagebroker")
  }
}
