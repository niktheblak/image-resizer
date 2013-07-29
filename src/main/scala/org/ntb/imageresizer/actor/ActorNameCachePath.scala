package org.ntb.imageresizer.actor

import akka.actor.Actor

trait ActorNameCachePath { self: Actor ⇒
  val cacheRoot: String = ""
  def cachePath: String =
    cacheRoot + escape(self.self.path.parent.name) + escape(self.self.path.name)
  
  def escape(str: String): String =
    str.replace('$', '_')
}