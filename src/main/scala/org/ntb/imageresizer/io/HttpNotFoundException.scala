package org.ntb.imageresizer.io

import org.apache.http.HttpException

class HttpNotFoundException(msg: String) extends HttpException(msg)
