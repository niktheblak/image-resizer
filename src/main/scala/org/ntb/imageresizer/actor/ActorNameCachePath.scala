package org.ntb.imageresizer.actor

import akka.actor.ActorRef
import akka.actor.Actor

trait ActorNameCachePath { self: Actor =>
  def cachePath: String =
    self.self.path.parent.name + escape(self.self.path.name)
  
  def escape(str: String): String =
    str.replace('$', '_')
}