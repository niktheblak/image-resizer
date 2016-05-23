package org.ntb.imageresizer.storage

import java.io.File

case class FilePosition(storage: File, offset: Long, size: Long)
