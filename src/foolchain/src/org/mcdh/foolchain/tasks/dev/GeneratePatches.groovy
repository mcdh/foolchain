package org.mcdh.foolchain.tasks.dev

import com.cloudbees.diff.Diff
import com.cloudbees.diff.Hunk
import com.google.common.base.Charsets
import com.google.common.io.ByteStreams
import com.google.common.io.Files
import net.minecraftforge.srg2source.util.io.FolderSupplier
import net.minecraftforge.srg2source.util.io.InputSupplier
import net.minecraftforge.srg2source.util.io.ZipInputSupplier
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.api.tasks.TaskAction

class GeneratePatches extends DefaultTask {
 public def patchDir
 public def changed
 public def original
 public def originalPrefix = ''
 public def changedPrefix = ''
 private final Set<String> created = new HashSet<>()

 @TaskAction
 def generate() {
  final File patchDir = project.file(this.patchDir)
  final File changed = project.file(this.changed)
  final File original = project.file(this.original)

  created.clear()
  patchDir.mkdirs()

  // fix and create patches.
  processFiles(getSupplier(original), getSupplier(changed))

  removeOld(patchDir)
 }

 private InputSupplier getSupplier(final File file) {
  if (file.isDirectory()) {
   return new FolderSupplier(file)
  }
  ZipInputSupplier zis = new ZipInputSupplier()
  zis.readZip(file)
  return zis
 }

 private void removeOld(final File dir) {
  final ArrayList<File> directories = new ArrayList<File>()
  FileTree tree = getProject().fileTree(dir)

  tree.visit(new FileVisitor() {
   @Override
   void visitDir(FileVisitDetails dir2) {
    directories.add(dir2.getFile())
   }

   @Override
   void visitFile(FileVisitDetails f) {
    File file = f.getFile()
    if (!created.contains(file)) {
     getLogger().debug("Removed patch: " + f.getRelativePath())
     file.delete()
    }
   }
  })

//  Collections.sort(directories, new Comparator<File>()
//  {
//   @Override
//   public int compare(File o1, File o2) {
//    int r = o1 <=> o2
//    if (r < 0) return 1
//    if (r > 0) return -1
//    return 0
//   }
//  })

  //We want things sorted in reverse order. Do that sub folders come before parents
  Collections.sort(directories, {
   a, b ->
    final int r = a <=> b
    if (r < 0) return 1
    if (r > 0) return -1
    return 0
  })

  for (File f : directories) {
   if (f.listFiles().length == 0) {
    getLogger().debug("Removing empty dir: " + f)
    f.delete()
   }
  }
 }

 private void processFiles(InputSupplier original, InputSupplier changed) {
  List<String> paths = original.gatherAll("")
  for (String path : paths) {
   path = path.replace('\\', '/')
   InputStream o = original.getInput(path)
   InputStream c = changed.getInput(path)
   try {
    processFile(path, o, c)
   } finally {
    if (o != null) o.close()
    if (c != null) c.close()
   }
  }
 }

 private void processFile(String relative, InputStream original, InputStream changed) throws IOException {
  getLogger().debug("Diffing: " + relative)
  File patchFile = new File(getPatchDir(), relative + ".patch")

  if (changed == null) {
   getLogger().debug("    Changed File does not exist")
   return
  }

  // We have to cache the bytes because diff reads the stream twice.. why.. who knows.
  byte[] oData = ByteStreams.toByteArray(original)
  byte[] cData = ByteStreams.toByteArray(changed)

  Diff diff = Diff.diff(
   new InputStreamReader(new ByteArrayInputStream(oData), Charsets.UTF_8),
   new InputStreamReader(new ByteArrayInputStream(cData), Charsets.UTF_8),
   false
  )

  if (!relative.startsWith("/")) {
   relative = "/" + relative
  }

  if (!diff.isEmpty()) {
   String unidiff = diff.toUnifiedDiff(
    originalPrefix + relative, changedPrefix + relative,
    new InputStreamReader(new ByteArrayInputStream(oData), Charsets.UTF_8),
    new InputStreamReader(new ByteArrayInputStream(cData), Charsets.UTF_8),
    3
   )
   //Normalize lines
   unidiff = unidiff.replace("\r\n", "\n")
   //We give 0 shits about this.
   unidiff = unidiff.replace("\n" + Hunk.ENDING_NEWLINE + "\n", "\n")

   String olddiff = ""
   if (patchFile.exists()) {
    olddiff = Files.toString(patchFile, Charsets.UTF_8)
   }

   if (!olddiff.equals(unidiff)) {
    getLogger().debug("Writing patch: " + patchFile)
    patchFile.getParentFile().mkdirs()
    Files.touch(patchFile)
    Files.write(unidiff, patchFile, Charsets.UTF_8)
   } else {
    getLogger().debug("Patch did not change")
   }
   created.add(patchFile)
  }
 }

 FileCollection getChanged() {
  File f = changed
  if (f.isDirectory()) {
   return getProject().fileTree(f)
  } else {
   return getProject().files(f)
  }
 }
}
