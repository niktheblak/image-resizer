package org.ntb.imageresizer.servlet

import org.scalatra._
import scalate.ScalateSupport

class ImageResizerServlet extends ImageResizerStack {
  get("/") {
    <html>
      <body>
        <h1>Hello, world!</h1>
        Say <a href="hello-scalate">hello to Scalate</a>.
      </body>
    </html>
  }
}
