package org.mcdh.foolchain.tasks

import com.google.common.io.ByteStreams
import org.apache.shiro.util.AntPathMatcher
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.mcdh.foolchain.tasks.utils.CachedTask

import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

class ExtractConfigTask extends CachedTask {
 private final AntPathMatcher antMatcher = new AntPathMatcher()
 public List<String> excludes = new LinkedList<>()
 public List<Closure<Boolean>> excludeCalls = new LinkedList<>()
 public List<String> includes = new LinkedList<>()
 public String config
 public def out

 @TaskAction
 def doTask() throws ZipException, IOException {
  final File out = project.file(out)
  out.mkdirs()

  for (File source : getConfigFiles()) {
   getLogger().debug("Extracting: ${source}")

   ZipFile input = new ZipFile(source)
   try {
    Enumeration<? extends ZipEntry> itr = input.entries()

    while (itr.hasMoreElements()) {
     ZipEntry entry = itr.nextElement()
     if (shouldExtract(entry.getName())) {
      File outFile = new File(out, entry.getName())
      getLogger().debug("  ${outFile}")
      if (!entry.isDirectory()) {
       File outParent = outFile.getParentFile()
       if (!outParent.exists()) {
        outParent.mkdirs()
       }

       FileOutputStream fos = new FileOutputStream(outFile)
       InputStream ins = input.getInputStream(entry)

       ByteStreams.copy(ins, fos)

       fos.close()
       ins.close()
      }
     }
    }
   } finally {
    input.close()
   }
  }
 }

 @Optional
 @InputFiles
 FileCollection getConfigFiles() {
  return getProject().getConfigurations().getByName(config)
 }

 ExtractConfigTask exclude(String... patterns) {
  for (String pattern : patterns) {
   excludes.add(pattern)
  }
  return this
 }

 ExtractConfigTask include(String... patterns) {
  for (String pattern : patterns) {
   includes.add(pattern)
  }
  return this
 }

 private boolean shouldExtract(String path) {
  for (String exclude : excludes) {
   if (antMatcher.matches(exclude, path)) {
    return false
   }
  }

  for (Closure<Boolean> exclude : excludeCalls) {
   if (exclude.call(path).booleanValue()) {
    return false
   }
  }

  for (String include : includes) {
   if (antMatcher.matches(include, path)) {
    return true
   }
  }

  return includes.size() == 0
  //If it gets to here, then it matches nothing. default to true, if no includes were specified
 }

 @Override
 boolean defaultCache() {
  return false
 }
}
