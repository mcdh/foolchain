package org.mcdh.foolchain.tasks.user

import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.RemapperProcessor
import net.md_5.specialsource.provider.ClassLoaderProvider
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import java.util.stream.Collectors

class SingleDeobfTask extends DefaultTask {
 public Set<String> classpath = new HashSet<>()
 public def input
 public def output = project.file(getTemporaryDir(), "${this.hashCode()}-deobf.jar")
 public def mappings

 @TaskAction
 def deobf() {
  final File fi = project.file(input), fo = files(output), maps = files(mappings)
  //Load mapping
  final JarMapping mapping = new JarMapping()
  mapping.loadMappings(maps)
  //Create processor
  final RemapperProcessor srgProcessor = new RemapperProcessor(null, mapping, null)
  //Make jar remapper
  final JarRemapper remapper = new JarRemapper(srgProcessor, mapping, null)
  //Load jar
  final Jar inJar = Jar.init(fi)
  //Ensure that inheritance provider is used
  final JointProvider inheritanceProviders = new JointProvider()
  inheritanceProviders.add(new JarProvider())
  inheritanceProviders.add(new ClassLoaderProvider(new URLClassLoader(convertToURLs(classpath))))
  //Remap jar
  remapper.remapJar(inJar, fo)
 }

 private static URL[] convertToURLs(final Collection<String> c) {
  return c
   .stream()
   .map({new URL(it)})
   .collect(Collectors.toList())
   .toArray(new URL[0])
 }
}