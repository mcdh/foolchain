import com.github.abrarsyed.jastyle.ASFormatter
import com.github.abrarsyed.jastyle.FileWildcardFilter
import com.github.abrarsyed.jastyle.OptParser
import com.google.common.base.Joiner
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import com.google.common.io.ByteStreams
import com.google.common.io.Files
import org.gradle.api.DefaultTask
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction

import org.mcdh.toolchain.utils.extrastuff.GLConstantFixer
import org.mcdh.toolchain.utils.patching.ContextualPatch

import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DecompileTask extends DefaultTask {
 //A private inner class to be used with the MCPPatches only.
 private static class ContextProvider implements ContextualPatch.IContextProvider {
  private Map<String, String> fileMap
  private final int STRIP = 1

  ContextProvider(Map<String, String> fileMap) {
   this.fileMap = fileMap
  }

  private String strip(String target) {
   target = target.replace('\\', '/')
   int index = 0
   for (int x = 0; x < STRIP; x++) {
    index = target.indexOf('/', index) + 1
   }
   return target.substring(index)
  }

  @Override
  List<String> getData(String target) {
   target = strip(target)
   if (fileMap.containsKey(target)) {
    String[] lines = fileMap.get(target).split("\r\n|\r|\n")
    List<String> ret = new ArrayList<String>()
    for (String line : lines) {
     ret.add(line)
    }
    return ret
   }
   return null
  }

  @Override
  void setData(String target, List<String> data) {
   fileMap.put(strip(target), Joiner.on(System.getProperty('line.separator')).join(data))
  }
 }

 public def input
 public def output
 public def fernflower
 public def patch
 public def astyleConfig

 private final HashMap<String, String> sourceMap = new HashMap<>()
 private final HashMap<String, byte[]> resourceMap = new HashMap<>()

 @TaskAction
 def decompile() {
  final File input = project.file(this.input)
  final File output = project.file(this.output)
  final File fernflower = project.file(this.fernflower)
  final File patch = project.file(this.patch)
  final File astyleConfig = project.file(this.astyleConfig)

  final File temp = new File(getTemporaryDir(), input.getName())

  getLogger().info("Decompiling Jar: '${inJar.absolutePath}'")
  //Execute FernFlower
  project.javaexec {
   args = [
    fernJar.absolutePath,
    '-din=1',
    '-rbr=0',
    '-dgs=1',
    '-asc=1',
    '-log=ERROR',
    input.absolutePath,
    getTemporaryDir().absolutePath
   ]
   main = '-jar'
   workingDir = fernflower.getParentFile()
   //TODO
//   classpath = Constants.getClasspath()
   standardOutput = System.out
   errorOutput = System.err
   maxHeapSize = '512M'
  }

  getLogger().info("Loading Jar: '${temp.absolutePath}'")
  readJarAndFix(temp)

  saveJar(new File(getTemporaryDir(), "${input.getName()}.fixed.jar"))

  getLogger().info('Applying MCP patches')
  applyMCPPatch(patch)

  saveJar(new File(getTemporaryDir(), "${input.getName()}.fixed.jar"))

  getLogger().info("Cleaning source")
  aplyMCPCleanup(astyleConfig)
 }

 private void readJarAndFix(final File jar) {
  final ZipInputStream zin = new ZipInputStream(new FileInputStream(jar))
  ZipEntry entry
  String fileStr

  //TODO
//  BaseExtension exten = (BaseExtension)getProject().getExtensions().getByName(EXT_NAME_MC);
//  boolean fixInterfaces = !exten.getVersion().equals("1.7.2");

  while ((entry = zin.getNextEntry()) != null) {
   // no META or dirs. we'll take care of dirs later.
   if (entry.getName().contains("META-INF")) {
    continue
   }

   // resources or directories.
   if (entry.isDirectory() || !entry.getName().endsWith(".java")) {
    resourceMap.put(entry.getName(), ByteStreams.toByteArray(zin))
   } else {
    // source!
    fileStr = new String(ByteStreams.toByteArray(zin), Charset.defaultCharset())

    // fix
    fileStr = FFPatcher.processFile(new File(entry.getName()).getName(), fileStr, fixInterfaces)
    sourceMap.put(entry.getName(), fileStr)
   }
  }
  zin.close()
 }

 private void saveJar(final File output) {
  final ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(output))
  //Write in resources
  resourceMap.each {
   zout.putNextEntry(new ZipEntry(it.key))
   zout.write(it.value)
   zout.closeEntry()
  }
  //Write in sources
  sourceMap.each {
   zout.putNextEntry(new ZipEntry(it.key))
   zout.write(it.value.getBytes())
   zout.closeEntry()
  }
  zout.close()
 }

 private void applyMCPPatch(final File file) {
  if (!file.isDirectory()) {
   ContextualPatch patch = ContextualPatch.create(Files.toString(file, Charset.defaultCharset()), new ContextProvider(sourceMap))
   printPatchErrors(patch.patch(false))
  } else {
   Multimap<String, File> patches = ArrayListMultimap.create()
   for (File f : patchDir.listFiles(new FileWildcardFilter("*.patch"))) {
    String base = f.getName()
    patches.put(base, f)
    for (File e : patchDir.listFiles(new FileWildcardFilter(base + ".*"))) {
     patches.put(base, e)
    }
   }

   for (String key : patches.keySet()) {
    ContextualPatch patch = findPatch(patches.get(key))
    if (patch == null) {
     getLogger().lifecycle("Patch not found for set: " + key) //This should never happen, but whatever
    } else {
     printPatchErrors(patch.patch(false))
    }
   }
  }
 }

 private void printPatchErrors(List<ContextualPatch.PatchReport> errors) throws Throwable {
  boolean fuzzed = false
  for (ContextualPatch.PatchReport report : errors) {
   if (!report.getStatus().isSuccess()) {
    getLogger().log(LogLevel.ERROR, "Patching failed: ${report.getTarget()}", report.getFailure())
    for (ContextualPatch.HunkReport hunk : report.getHunks()) {
     if (!hunk.getStatus().isSuccess()) {
      getLogger().error("Hunk ${hunk.getHunkID()} failed!")
     }
    }
    throw report.getFailure()
    // catch fuzzed patches
   } else if (report.getStatus() == ContextualPatch.PatchStatus.Fuzzed) {
    getLogger().log(LogLevel.INFO, "Patching fuzzed: ${report.getTarget()}", report.getFailure())
    fuzzed = true
    for (ContextualPatch.HunkReport hunk : report.getHunks()) {
     if (!hunk.getStatus().isSuccess()) {
      getLogger().info("Hunk ${hunk.getHunkID()} fuzzed ${hunk.getFuzz()} !")
     }
    }
   } else {
    getLogger().debug("Patch succeeded: " + report.getTarget())
   }
  }
  if (fuzzed) {
   getLogger().lifecycle("Patches Fuzzed!")
  }
 }

 private void applyMcpCleanup(File conf) throws IOException {
  ASFormatter formatter = new ASFormatter()
  OptParser parser = new OptParser(formatter)
  parser.parseOptionFile(conf)

  Reader reader
  Writer writer

  GLConstantFixer fixer = new GLConstantFixer()
  ArrayList<String> files = new ArrayList<String>(sourceMap.keySet())
  Collections.sort(files)
  // Just to make sure we have the same order.. shouldn't matter on anything but lets be careful.

  for (String file : files) {
   String text = sourceMap.get(file)

   getLogger().debug("Processing file: " + file)

   getLogger().debug("processing comments")
   text = McpCleanup.stripComments(text)

   getLogger().debug("fixing imports comments")
   text = McpCleanup.fixImports(text)

   getLogger().debug("various other cleanup")
   text = McpCleanup.cleanup(text)

   getLogger().debug("fixing OGL constants")
   text = fixer.fixOGL(text)

   getLogger().debug("formatting source")
   reader = new StringReader(text)
   writer = new StringWriter()
   formatter.format(reader, writer)
   reader.close()
   writer.flush()
   writer.close()
   text = writer.toString()

   getLogger().debug("applying FML transformations")
   text = BEFORE.matcher(text).replaceAll('$1')
   text = AFTER.matcher(text).replaceAll('$1')
   text = FmlCleanup.renameClass(text)

   sourceMap.put(file, text)
  }
 }
}
