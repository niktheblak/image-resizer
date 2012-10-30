package org.ntb.imageresizer.actor

import akka.actor.ActorRef

trait ActorNameCachePath {
  val self: ActorRef
  
  def cachePath: String =
    self.path.parent.name + escape(self.path.name)
  
  def escape(str: String): String =
    str.replace('$', '_')
}