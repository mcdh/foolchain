package org.mcdh.foolchain.tasks.dev

import com.google.common.base.CharMatcher
import com.google.common.base.Splitter
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Iterables
import com.google.common.io.ByteArrayDataOutput
import com.google.common.io.ByteStreams
import com.google.common.io.LineProcessor
import com.nothome.delta.Delta
import lzma.streams.LzmaOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import java.nio.charset.Charset
import java.nio.file.Files
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.jar.Pack200
import java.util.zip.Adler32
import java.util.zip.ZipEntry

class GenBinaryPatches extends DefaultTask {
 private final HashMap<String, String> obfMapping = new HashMap<>(), srgMapping = new HashMap<>()
 private final ArrayListMultimap<String, String> innerClasses = ArrayListMultimap.create()
 private final Set<String> patchedFiles = new HashSet<>()
 private final Delta delta = new Delta()

 public def cleanClient
 public def cleanServer
 public def cleanMerged
 public def dirtyJar
 public List<List<String>> patchList
// def public deobfDataLzma
 public def srg
 public def outJar

 @TaskAction
 def genPatches() {
  final File srg = project.file(this.srg)
  final File cleanClient = project.file(this.cleanClient)
  final File cleanServer = project.file(this.cleanServer)
  final File cleanMerged = project.file(this.cleanMerged)
  final File dirtyJar = project.file(this.dirtyJar)
  loadMappings(srg)

  for (final List<String> tree : patchList) {
   for (final String sfile : tree) {
    final File file = project.file(sfile)
    final String name = file.getName().replace(".java.patch", "")
    final String obfName = srgMapping.get(name)
    patchedFiles.add(obfName)
    addInnerClasses(name, patchedFiles)
   }
  }

  HashMap<String, byte[]> runtime = new HashMap<>()
  HashMap<String, byte[]> devtime = new HashMap<>()

  createBinPatches(runtime, "client/", cleanClient, dirtyJar)
  createBinPatches(runtime, "server/", cleanServer, dirtyJar)
  createBinPatches(devtime, "merged/", cleanMerged, dirtyJar)

  byte[] runtimedata = createPatchJar(runtime)
  runtimedata = pack200(runtimedata)
  runtimedata = compress(runtimedata)

  byte[] devtimedata = createPatchJar(devtime)
  devtimedata = pack200(devtimedata)
  devtimedata = compress(devtimedata)

  buildOutput(runtimedata, devtimedata)
 }

 private void addInnerClasses(String parent, Set<String> patchList) {
  for (final String inner : innerClasses.get(parent)) {
   patchedFiles.add(srgMapping.get(inner))
   addInnerClasses(inner, patchList)
  }
 }

 private final loadMappings(final File srg) {
  Files.readLines(srg, Charset.defaultCharset(), new LineProcessor<String>() {
   Splitter splitter = Splitter.on(CharMatcher.anyOf(": ")).omitEmptyStrings().trimResults();

   @Override
   public boolean processLine(String line) {
    if (!line.startsWith("CL")) {
     return true
    }

    String[] parts = Iterables.toArray(splitter.split(line), String.class)
    obfMapping.put(parts[1], parts[2])
    String srgName = parts[2].substring(parts[2].lastIndexOf('/') + 1)
    srgMapping.put(srgName, parts[1])
    int innerDollar = srgName.lastIndexOf('$')
    if (innerDollar > 0) {
     String outer = srgName.substring(0, innerDollar)
     innerClasses.put(outer, srgName)
    }
    return true
   }

   @Override
   public String getResult() {
    return null
   }
  })
 }

 private void createBinPatches(HashMap<String, byte[]> patches, String root, File base, File target) throws Exception {
  JarFile cleanJar = new JarFile(base), dirtyJar = new JarFile(target)
  for (Map.Entry<String, String> entry : obfMapping.entrySet()) {
   String obf = entry.key, srg = entry.value
   if (!patchedFiles.contains(obf)) {
    continue
   }

   JarEntry cleanE = cleanJar.getJarEntry(obf + ".class"), dirtyE = dirtyJar.getJarEntry(obf + ".class")
   if (dirtyE == null) {
    continue
   }

   byte[] clean = (cleanE != null ? ByteStreams.toByteArray(cleanJar.getInputStream(cleanE)) : new byte[0])
   byte[] dirty = ByteStreams.toByteArray(dirtyJar.getInputStream(dirtyE))

   byte[] diff = delta.compute(clean, dirty)

   ByteArrayDataOutput out = ByteStreams.newDataOutput(diff.length + 50)
   //Clean name
   out.writeUTF(obf)
   //Source Notch name
   out.writeUTF(obf.replace('/', '.'))
   //Source SRG Name
   out.writeUTF(srg.replace('/', '.'))
   //Exists in Clean
   out.writeBoolean(cleanE != null)
   if (cleanE != null) {
    //Hash of Clean file
    out.writeInt(adlerHash(clean))
   }
   //Patch length
   out.writeInt(diff.length)
   //Patch
   out.write(diff)

   patches.put(root + srg.replace('/', '.') + ".binpatch", out.toByteArray())
  }
 }

 private int adlerHash(byte[] input) {
  Adler32 hasher = new Adler32()
  hasher.update(input)
  return (int)hasher.getValue()
 }

 private byte[] createPatchJar(HashMap<String, byte[]> patches) throws Exception {
  ByteArrayOutputStream out = new ByteArrayOutputStream()
  JarOutputStream jar = new JarOutputStream(out)
  for (Map.Entry<String, byte[]> entry : patches.entrySet()) {
   jar.putNextEntry(new JarEntry("binpatch/" + entry.getKey()))
   jar.write(entry.getValue())
  }
  jar.close()
  return out.toByteArray()
 }

 private byte[] pack200(byte[] data) throws Exception {
  JarInputStream jin = new JarInputStream(new ByteArrayInputStream(data))
  ByteArrayOutputStream out = new ByteArrayOutputStream()

  Pack200.Packer packer = Pack200.newPacker()

  SortedMap<String, String> props = packer.properties()
  props.put(Pack200.Packer.EFFORT, "9")
  props.put(Pack200.Packer.KEEP_FILE_ORDER, Pack200.Packer.TRUE)
  props.put(Pack200.Packer.UNKNOWN_ATTRIBUTE, Pack200.Packer.PASS)

  final PrintStream err = new PrintStream(System.err)
  System.setErr(new PrintStream(ByteStreams.nullOutputStream()))
  packer.pack(jin, out)
  System.setErr(err)

  jin.close()
  out.close()

  return out.toByteArray()
 }

 private byte[] compress(byte[] data) throws Exception {
  ByteArrayOutputStream out = new ByteArrayOutputStream()
  LzmaOutputStream lzma = new LzmaOutputStream.Builder(out).useEndMarkerMode(true).build()
  lzma.write(data)
  lzma.close()
  return out.toByteArray()
 }

 private void buildOutput(byte[] runtime, byte[] devtime) throws Exception {
  JarOutputStream out = new JarOutputStream(new FileOutputStream(getOutJar()))
  JarFile jin = new JarFile(getDirtyJar())

  if (runtime != null) {
   out.putNextEntry(new JarEntry("binpatches.pack.lzma"));
   out.write(runtime)
  }

  if (devtime != null) {
   out.putNextEntry(new JarEntry("devbinpatches.pack.lzma"));
   out.write(devtime)
  }

  for (JarEntry e : Collections.list(jin.entries())) {
   if (e.isDirectory()) {
    //out.putNextEntry(e); //Not quite sure how to filter out directories we dont care about..
   } else {
    //It's not a class, we don't want resources or anything
    //It's a base class and as such should be in the binpatches
    if (!e.getName().endsWith(".class") || obfMapping.containsKey(e.getName().replace(".class", ""))) {
     continue
    }

    ZipEntry n = new ZipEntry(e.getName());
    n.setTime(e.getTime());
    out.putNextEntry(n);
    out.write(ByteStreams.toByteArray(jin.getInputStream(e)));
   }
  }

  out.close()
  jin.close()
 }
}
