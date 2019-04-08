package org.mcdh.foolchain.tasks.user

import com.google.common.base.Joiner
import com.google.common.base.Throwables
import com.google.common.io.ByteStreams
import com.nothome.delta.GDiffPatcher
import lzma.sdk.lzma.Decoder
import lzma.streams.LzmaInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.api.tasks.TaskAction

import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.jar.Pack200
import java.util.regex.Pattern
import java.util.zip.Adler32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ApplyBinPatchesTask extends DefaultTask {
 static class ClassPatch {
  public final String name
  public final String sourceClassName
  public final String targetClassName
  public final boolean existsAtTarget
  public final byte[] patch
  public final int inputChecksum

  ClassPatch(String name, String sourceClassName, String targetClassName, boolean existsAtTarget, int inputChecksum, byte[] patch) {
   this.name = name
   this.sourceClassName = sourceClassName
   this.targetClassName = targetClassName
   this.existsAtTarget = existsAtTarget
   this.inputChecksum = inputChecksum
   this.patch = patch
  }

  @Override
  String toString() {
   return String.format("%s : %s => %s (%b) size %d", name, sourceClassName, targetClassName, existsAtTarget, patch.length)
  }
 }

 public def inJar
 public def classesJar
 public def outJar
 //This will be a patches.lzma
 public def patches
 //File tree
 public def resources

 private HashMap<String, ClassPatch> patchlist = new HashMap<>()
 private GDiffPatcher patcher = new GDiffPatcher()

 private int adlerHash(byte[] input) {
  Adler32 hasher = new Adler32()
  hasher.update(input)
  return (int)hasher.getValue()
 }

 def setup() {
  Pattern matcher = Pattern.compile(String.format("binpatch/merged/.*.binpatch"))

  JarInputStream jis
  try {
   LzmaInputStream binpatchesDecompressed = new LzmaInputStream(new FileInputStream(project.file(patches)), new Decoder())
   ByteArrayOutputStream jarBytes = new ByteArrayOutputStream()
   JarOutputStream jos = new JarOutputStream(jarBytes)
   Pack200.newUnpacker().unpack(binpatchesDecompressed, jos)
   jis = new JarInputStream(new ByteArrayInputStream(jarBytes.toByteArray()))
  } catch (Exception e) {
   throw Throwables.propagate(e)
  }

  log("Reading Patches:")
  JarEntry entry = jis.getNextJarEntry()
  while (entry != null) {
   try {
    if (matcher.matcher(entry.getName()).matches()) {
     ClassPatch cp = readPatch(entry, jis)
     patchlist.put(cp.sourceClassName.replace('.', '/') + ".class", cp)
    } else {
     jis.closeEntry()
    }
   } catch (IOException e) {}
   entry = jis.getNextJarEntry()
  }

  log("Read %d binary patches", patchlist.size())
  log("Patch list :\n\t%s", Joiner.on("\n\t").join(patchlist.entrySet()))
 }

 @TaskAction
 def doTask() {
  final File inJar = project.file(this.inJar)
  final File classesJar = project.file(this.classesJar)
  final File outJar = project.file(this.outJar)
  final File patches = project.file(this.patches)
  final File resources = project.file(this.resources)
  setup()

  if (outJar.exists()) {
   outJar.delete()
  }

  ZipFile zin = new ZipFile(inJar)
  ZipInputStream classesIn = new ZipInputStream(new FileInputStream(classesJar))
  final ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outJar)))
  final HashSet<String> entries = new HashSet<String>()

  try {
   // DO PATCHES
   log("Patching Class:")
   for (ZipEntry e : Collections.list(zin.entries())) {
    if (e.getName().contains("META-INF"))
     continue

    if (e.isDirectory()) {
     out.putNextEntry(e)
    } else {
     ZipEntry n = new ZipEntry(e.getName())
     n.setTime(e.getTime())
     out.putNextEntry(n)

     byte[] data = ByteStreams.toByteArray(zin.getInputStream(e))
     ClassPatch patch = patchlist.get(e.getName().replace('\\', '/'))

     if (patch != null) {
      log("\t%s (%s) (input size %d)", patch.targetClassName, patch.sourceClassName, data.length)
      int inputChecksum = adlerHash(data)
      if (patch.inputChecksum != inputChecksum) {
       throw new RuntimeException(String.format("There is a binary discrepency between the expected input class %s (%s) and the actual class. Checksum on disk is %x, in patch %x. Things are probably about to go very wrong. Did you put something into the jar file?", patch.targetClassName, patch.sourceClassName, inputChecksum, patch.inputChecksum))
      }
      synchronized (patcher) {
       data = patcher.patch(data, patch.patch)
      }
     }

     out.write(data)
    }

    // add the names to the hashset
    entries.add(e.getName())
   }

   // COPY DATA
   ZipEntry entry = null
   while ((entry = classesIn.getNextEntry()) != null) {
    if (entries.contains(entry.getName()))
     continue

    out.putNextEntry(entry)
    out.write(ByteStreams.toByteArray(classesIn))
    entries.add(entry.getName())
   }

   project.fileTree(resources).visit(new FileVisitor() {
    @Override
    void visitDir(FileVisitDetails dirDetails) {}

    @Override
    void visitFile(FileVisitDetails file) {
     try {
      String name = file.getRelativePath().toString().replace('\\', '/')
      if (!entries.contains(name)) {
       ZipEntry n = new ZipEntry(name)
       n.setTime(file.getLastModified())
       out.putNextEntry(n)
       ByteStreams.copy(file.open(), out)
       entries.add(name)
      }
     } catch (IOException e) {
      Throwables.propagateIfPossible(e)
     }
    }
   })
  } finally {
   classesIn.close()
   in.close()
   out.close()
  }
 }
}
