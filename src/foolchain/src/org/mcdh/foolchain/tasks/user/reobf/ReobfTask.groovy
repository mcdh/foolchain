package org.mcdh.foolchain.tasks.user.reobf

import com.google.common.base.Charsets
import com.google.common.io.Files
import org.gradle.api.DefaultTask
import org.gradle.api.DomainObjectSet
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.api.tasks.TaskAction

import org.mcdh.toolchain.common.ToolchainConfigurationExtension
import org.mcdh.toolchain.common.Constants
import org.mcdh.toolchain.utils.SimpleFileCollection
import org.mcdh.toolchain.utils.extrastuff.ReobfExceptor

import java.util.stream.Collectors

class ReobfTask extends DefaultTask {
 private final DefaultDomainObjectSet<ObfArtifact> obfOutput = new DefaultDomainObjectSet<>()

 public boolean useRetroGaurd = false
 public String mcVersion
 public def srg
 public def fieldCsv
 public def methodCsv
 public def exceptorCfg
 public def deobfFile
 public def recompFile
 public List<String> extraSrg = new ArrayList<>()
 public List<String> extraSrgFiles = new ArrayList<>()

 public ReobfTask() {
  super()

  getInputs().files({ getFilesToObfuscate() })
  getOutputs().files({ getObfuscatedFiles() })
 }

 public DomainObjectSet<ObfArtifact> getObfuscated() {
  return obfOutput
 }

 public FileCollection getFilesToObfuscate() {
  return new SimpleFileCollection(obfOutput
   .stream()
   .filter({ it != null && it.getToObf() != null })
   .map({ it.getToObf() })
   .collect(Collectors.toList())
  )
 }

 public FileCollection getObfuscatedFiles() {
  return new SimpleFileCollection(obfOutput
   .stream()
   .filter({ it != null && it.getFile() != null })
   .map({ it.getFile() })
   .collect(Collectors.toList())
  )
 }

 public FileCollection getExtraSrgFiles() {
  final List<File> files = new ArrayList<>()
  for (final String sf : extraSrgFiles) {
   final File file = project.file(sf)
   if (file.isDirectory()) {
    for (final File subfile : project.fileTree(file)) {
     if (Files.getFileExtension(subfile.getName()).toLowerCase() == 'srg') {
      files.add(subfile)
     }
    }
   } else {
    if (Files.getFileExtension(file.getName()).toLowerCase() == 'srg') {
     files.add(file)
    }
   }
  }
  return new SimpleFileCollection(files)
 }

 @TaskAction
 def reobf() {
  final File srg = project.file(this.srg)
  final File fieldCsv = project.file(this.fieldCsv)
  final File methodCsv = project.file(this.methodCsv)
  final File exceptorCfg = project.file(this.exceptorCfg)
  final File deobfFile = project.file(this.deobfFile)
  final File recompFile = project.file(this.recompFile)
//  final List<File>

  //Do stuff.
  ReobfExceptor exc = null
  File tmpSrg = File.createTempFile("reobf-default", ".srg", getTemporaryDir())
  File extraSrg = File.createTempFile("reobf-extra", ".srg", getTemporaryDir())

//  UserExtension ext = (UserExtension)getProject().getExtensions().getByName(Constants.EXT_NAME_MC)
  ToolchainConfigurationExtension ext = (ToolchainConfigurationExtension)getProject().getExtensions().getByName(Constants.EXT_NAME_MC)

  if (ext.isDecomp()) {
//   exc = getExceptor()
   exc = new ReobfExceptor()
   exc.deobfJar = deobfFile
   exc.toReobfJar = recompFile
   exc.excConfig = exceptorCfg
   exc.fieldCSV = fieldCsv
   exc.methodCSV = methodCsv
   exc.buildSrg(srg, tmpSrg)
  } else {
   Files.copy(srg, tmpSrg)
  }

  //Generate extraSrg
  if (!extraSrg.exists()) {
   extraSrg.getParentFile().mkdirs()
   extraSrg.createNewFile()
  }

  BufferedWriter writer = Files.newWriter(extraSrg, Charsets.UTF_8)
  for (String line : extraSrg) {
   writer.write(line)
   writer.newLine()
  }

  writer.flush()
  writer.close()

  for (ObfArtifact obf : getObfuscated()) {
   obf.generate(exc, tmpSrg, extraSrg, getExtraSrgFiles())
  }

  //Cleanup
  tmpSrg.delete()
  extraSrg.delete()
 }
}
