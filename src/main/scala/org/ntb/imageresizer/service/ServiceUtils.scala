package org.ntb.imageresizer.service

import org.ntb.imageresizer.util.Loans.using

import com.google.common.io.Files

import akka.actor.ActorRef
import akka.actor.actorRef2Scala

import spray.http._
import spray.http.ContentType.apply

import java.io.File
import java.io.FileInputStream

object ServiceUtils {
  val chunkTreshold = 100000L
  
  def serveFile(file: File, mimeType: MediaType, sender: ActorRef) {
    if (file.length() > chunkTreshold) {
      val headers = List(HttpHeaders.`Content-Type`(mimeType))
      sender ! ChunkedResponseStart(HttpResponse(headers = headers))
      readFileChunked(file) { buf =>
        sender ! MessageChunk(buf)
      }
      sender ! ChunkedMessageEnd
    } else {
      val data = Files.toByteArray(file)
      sender ! HttpResponse(entity = HttpBody(mimeType, data))
    }
  }

  def readFileChunked(file: File, chunkSize: Int = 16 * 1024)(f: Array[Byte] => Unit) {
    using(new FileInputStream(file)) { input =>
      val buffer = new Array[Byte](chunkSize)
      var bytesRead = input.read(buffer)
      while (bytesRead > 0) {
        f(buffer.slice(0, bytesRead))
        bytesRead = input.read(buffer)
      }
    }
  }
}