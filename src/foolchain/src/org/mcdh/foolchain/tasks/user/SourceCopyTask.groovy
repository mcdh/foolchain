package org.mcdh.foolchain.tasks.user

import com.google.common.base.Charsets
import com.google.common.io.Files
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryTree
import org.gradle.api.file.FileTree
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.util.PatternSet

import java.util.regex.Pattern

class SourceCopyTask extends DefaultTask {
 public SourceDirectorySet source
 HashMap<String, Object> replacements = new HashMap<>()
 ArrayList<String> includes = new ArrayList<>()
 def public output

 @TaskAction
 def doTask() {
  getLogger().debug("INPUTS >> " + source)
  getLogger().debug("OUTPUTS >> " + getOutput())

  // get the include/exclude patterns from the source (this is different than what's returned by getFilter)
  PatternSet patterns = new PatternSet()
  patterns.setIncludes(source.getIncludes())
  patterns.setExcludes(source.getExcludes())

  // get output
  File out = project.file(output)
  if (out.exists())
   deleteDir(out)

  out.mkdirs()
  out = out.getCanonicalFile()

  // resolve replacements
  HashMap<String, String> repl = new HashMap<String, String>(replacements.size())
  for (Map.Entry<String, Object> e : replacements.entrySet()) {
   if (e.getKy() == null || e.getValue() == null) {
    // we dont deal with nulls.
    continue
   }

   Object val = e.getValue()
   while (val instanceof Closure) {
    val = ((Closure<Object>)val).call()
   }

   repl.put(Pattern.quote(e.getKey()), val.toString())
  }

  getLogger().debug("REPLACE >> " + repl)

  // start traversing tree
  for (DirectoryTree dirTree : source.getSrcDirTrees()) {
   File dir = dirTree.getDir()
   getLogger().debug("PARSING DIR >> " + dir)

   // handle nonexistant srcDirs
   if (!dir.exists() || !dir.isDirectory()) {
    continue
   } else {
    dir = dir.getCanonicalFile()
   }

   // this could be written as .matching(source), but it doesn't actually work
   // because later on gradle casts it directly to PatternSet and crashes
   FileTree tree = getProject().fileTree(dir).matching(source.getFilter()).matching(patterns)

   for (File file : tree) {
    File dest = getDest(file, dir, out)
    dest.getParentFile().mkdirs()
    dest.createNewFile()

    if (isIncluded(file)) {
     getLogger().debug("PARSING FILE IN >> " + file)
     String text = Files.toString(file, Charsets.UTF_8)

     for (Map.Entry<String, String> entry : repl.entrySet()) {
      text = text.replaceAll(entry.getKey(), entry.getValue())
     }

     getLogger().debug("PARSING FILE OUT >> " + dest)
     Files.write(text, dest, Charsets.UTF_8)
    } else {
     Files.copy(file, dest)
    }
   }
  }
 }

 private File getDest(File fin, File base, File baseOut) throws IOException {
  String relative = fin.getCanonicalPath().replace(base.getCanonicalPath(), "")
  return new File(baseOut, relative)
 }

 private boolean isIncluded(File file) throws IOException {
  if (includes.isEmpty()) {
   return true
  }

  String path = file.getCanonicalPath().replace('\\', '/')
  for (String include : includes) {
   if (path.endsWith(include.replace('\\', '/'))) {
    return true
   }
  }

  return false
 }

 private boolean deleteDir(File dir) {
  if (dir.exists()) {
   File[] files = dir.listFiles()
   if (null != files) {
    for (int i = 0; i < files.length; i++) {
     if (files[i].isDirectory()) {
      deleteDir(files[i])
     } else {
      files[i].delete()
     }
    }
   }
  }
  return (dir.delete())
 }
}