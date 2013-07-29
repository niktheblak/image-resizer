package org.ntb.imageresizer.cache

trait Cache[A, B] {
  def put(key: A, value: B)
  def get(key: A): Option[B]
  def get(key: A, loader: () ⇒ B): B
}