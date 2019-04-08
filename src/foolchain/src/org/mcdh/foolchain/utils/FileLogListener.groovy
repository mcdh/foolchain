package org.mcdh.foolchain.utils

import com.google.common.io.Files
import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.StandardOutputListener

import java.nio.charset.Charset

class FileLogListener implements StandardOutputListener, BuildListener {
 private final File out
 private final BufferedWriter writer

 FileLogListener(final File file) {
  this.out = file
  try {
   if (out.exists()) {
    out.delete()
   } else {
    out.parentFile.mkdir()
   }
   out.createNewFile()
   writer = Files.newWriter(out, Charset.defaultCharset())
  } catch(Throwable t) {
   throw new ProjectConfigurationException('Could not setup logger!', t)
  }
 }

 @Override
 void buildStarted(Gradle gradle) {}

 @Override
 void settingsEvaluated(Settings settings) {}

 @Override
 void projectsLoaded(Gradle gradle) {
 }

 @Override
 void projectsEvaluated(Gradle gradle) {
 }

 @Override
 void buildFinished(BuildResult buildResult) {
 }

 @Override
 void onOutput(CharSequence charSequence) {
  try {
   writer.write(charSequence.toString())
  } catch (Throwable t) {}
 }
}
