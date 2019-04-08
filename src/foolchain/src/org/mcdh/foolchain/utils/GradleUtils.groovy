package org.mcdh.foolchain.utils

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.testfixtures.ProjectBuilder

final class GradleUtils {
 static MavenArtifactRepository addMavenRepository(final Project p, final String name, final String url) {
  return p.getRepositories().maven {
   final MavenArtifactRepository mar ->
    mar.setName(name)
    mar.setUrl(url)
  }
 }

 static FlatDirectoryArtifactRepository addFlatRepository(final Project p, final String name, final Object... dirs) {
  return p.getRepositories().flatDir {
   final FlatDirectoryArtifactRepository fdar ->
    fdar.setName(name)
    fdar.dirs(dirs)
  }
 }

 static <T extends Task> T makeTask(final Project p, final String name, final Class<T> type) {
  final HashMap<String, Object> map = new HashMap<>()
  map.put('name', name)
  map.put('type', type)
  return (T)p.task(map, name)
 }

 static Project getProject(final File buildFile, final Project parent) {
  ProjectBuilder builder = ProjectBuilder.builder()
  if (parent != null) {
   builder = builder.withParent(parent)
  }
  final Project project
  if (buildFile != null) {
   builder = builder
    .withProjectDir(buildFile.parentFile)
    .withName(buildFile.parentFile.name)
   project = builder.build()
   final HashMap<String, String> map = new HashMap<>()
   map.put('from', buildFile.absolutePath)
   project.apply(map)
  } else {
   builder = builder.withProjectDir(new File('.'))
   project = builder.build()
  }
  return project
 }
}
