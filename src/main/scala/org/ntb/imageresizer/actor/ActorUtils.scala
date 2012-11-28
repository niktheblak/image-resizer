package org.ntb.imageresizer.actor

import akka.actor.ActorRef
import akka.actor.Status

object ActorUtils {
  def requireArgument(sender: ActorRef)(condition: Boolean, message: => String = "") {
    if (!condition) {
      val e = new IllegalArgumentException("Invalid parameter: " + message)
      sender ! Status.Failure(e)
      throw e
    }
  }
}