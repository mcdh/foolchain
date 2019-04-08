package org.mcdh.foolchain.tasks.dev

import com.google.common.io.Files
import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.provider.ClassLoaderProvider
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.TaskAction
import org.mcdh.toolchain.utils.extrastuff.ReobfExceptor
import org.mcdh.toolchain.utils.URLUtils;

import java.nio.charset.Charset

class ObfuscateTask extends DefaultTask {
 public def outJar
 public def preFFJar
 public def srg
 public def exc
 public def buildFile
 public def methodsCsv
 public def fieldsCsv
 public String subTask = 'jar'

 private boolean reverse
 private LinkedList<Action<Project>> configureProject = new LinkedList<>()
 private LinkedList<String> extraSrg = new LinkedList<>()

 @TaskAction
 def doTask() {
//  final File outJar = project.file(this.outJar)
  final File preFFJar = project.file(this.preFFJar)
  File srg = project.file(this.srg)
  final File exc = project.file(this.exc)
  final File buildFile = project.file(this.buildFile)
  final File methodsCsv = project.file(this.methodsCsv)
  final File fieldsCsv = project.file(this.fieldsCsv)

  getLogger().debug("Building child project model...")
  Project childProj = FmlDevPlugin.getProject(buildFile, project)
  for (Action<Project> act : configureProject) {
   if (act != null) {
    act.execute(childProj)
   }
  }

  AbstractTask compileTask = (AbstractTask)childProj.getTasks().getByName("compileJava")
  AbstractTask jarTask = (AbstractTask)childProj.getTasks().getByName(subTask)

  //Executing jar task
  getLogger().debug("Executing child " + subTask + " task...")
  executeTask(jarTask)

  File inJar = (File)jarTask.property("archivePath")

  if (getExc() != null) {
   ReobfExceptor exceptor = new ReobfExceptor()
   exceptor.toReobfJar = inJar
   exceptor.deobfJar = preFFJar
   exceptor.excConfig = exc
   exceptor.methodCSV = methodsCsv
   exceptor.fieldCSV = fieldsCsv

   File outSrg = new File(getTemporaryDir(), "reobf_cls.srg")

   exceptor.doFirstThings()
   exceptor.buildSrg(srg, outSrg)

   srg = outSrg
  }

  // append SRG
  BufferedWriter writer = new BufferedWriter(new FileWriter(srg, true))
  for (String line : extraSrg) {
   writer.write(line)
   writer.newLine()
  }
  writer.flush()
  writer.close()

  getLogger().debug("Obfuscating jar...")
  obfuscate(inJar, (FileCollection)compileTask.property("classpath"), srg)
 }

 private void executeTask(AbstractTask task) {
  for (Object dep : task.getTaskDependencies().getDependencies(task)) {
   executeTask((AbstractTask)dep)
  }

  if (!task.getState().getExecuted()) {
   getLogger().lifecycle(task.getPath())
   task.execute()
  }
 }

 private void obfuscate(File inJar, FileCollection classpath, File srg) throws FileNotFoundException, IOException {
  // load mapping
  JarMapping mapping = new JarMapping()
  mapping.loadMappings(Files.newReader(srg, Charset.defaultCharset()), null, null, reverse)

  // make remapper
  JarRemapper remapper = new JarRemapper(null, mapping)

  // load jar
  Jar input = Jar.init(inJar)

  // ensure that inheritance provider is used
  JointProvider inheritanceProviders = new JointProvider()
  inheritanceProviders.add(new JarProvider(input))

  if (classpath != null)
   inheritanceProviders.add(new ClassLoaderProvider(new URLClassLoader(URLUtils.toUrls(classpath))))

  mapping.setFallbackInheritanceProvider(inheritanceProviders)

  File out = project.file(this.outJar)
  if (!out.getParentFile().exists()) //Needed because SS doesn't create it.
  {
   out.getParentFile().mkdirs()
  }

  // remap jar
  remapper.remapJar(input, out)
 }
}
