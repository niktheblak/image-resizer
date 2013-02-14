package org.ntb.imageresizer.actor

import akka.actor.ActorRef
import akka.actor.Status
import scala.util.{ Try, Success, Failure }

trait ActorUtils {
  def requireArgument(sender: ActorRef)(condition: Boolean, message: ⇒ String = "") {
    if (!condition) {
      val e = new IllegalArgumentException("Invalid parameter: " + message)
      sender ! Status.Failure(e)
      throw e
    }
  }

  def actorTry[A](sender: ActorRef)(action: ⇒ A): ActorTry[A] = {
    try {
      val v = action
      ActorSuccess(v)
    } catch {
      case t: Throwable ⇒ ActorFailure(sender, t)
    }
  }

  abstract class ActorTry[A] {
    def actorCatch(pf: PartialFunction[Throwable, A]): A
  }

  case class ActorSuccess[A](value: A) extends ActorTry[A] {
    def actorCatch(pf: PartialFunction[Throwable, A]): A = {
      value
    }
  }

  case class ActorFailure[A](sender: ActorRef, t: Throwable) extends ActorTry[A] {
    def actorCatch(pf: PartialFunction[Throwable, A]): A = {
      sender ! Status.Failure(t)
      if (pf.isDefinedAt(t)) pf.apply(t)
      else throw t
    }
  }
}