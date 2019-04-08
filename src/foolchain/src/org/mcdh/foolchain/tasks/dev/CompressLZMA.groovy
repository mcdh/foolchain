package org.mcdh.foolchain.tasks.dev

import com.google.common.io.ByteStreams
import lzma.streams.LzmaOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class CompressLZMA extends DefaultTask {
 public def input
 public def output

 @TaskAction
 def compress() {
  final File input = project.file(input)
  final File output = project.file(output)

  final InputStream fis = new FileInputStream(input)
  final OutputStream fos = new LzmaOutputStream.Builder(new FileOutputStream(output))
   .useEndMakerMode(true)
   .build()

  ByteStreams.copy(fis, fos)
  fis.close()
  fos.close()
 }
}
