package org.mcdh.foolchain.tasks.user.reobf

import com.google.common.io.Files
import org.gradle.api.Project
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.tasks.bundling.AbstractArchiveTask

class ArtifactSpec {
 private final Project project
 private boolean archiveSet = false
 private String baseName, appendix, version, classifier, extension, archiveName
 private Object classpath

 protected File srg

 public ArtifactSpec(final Project project) {
  this.project = project
 }

 public ArtifactSpec(File file, Project proj) {
  this.archiveName = file.getName()
  this.extension = Files.getFileExtension(file.getName())
  this.project = proj
 }

 public ArtifactSpec(String file, Project proj) {
  this.archiveName = file
  this.extension = Files.getFileExtension(file)
  this.project = proj
 }

 public ArtifactSpec(PublishArtifact artifact, Project proj) {
  this.baseName = artifact.getName()
  this.classifier = artifact.getClassifier()
  this.extension = artifact.getExtension()
  this.project = proj
 }

 public ArtifactSpec(final AbstractArchiveTask task) {
  project = task.getProject()
  baseName = task.getBaseName()
  appendix = task.getAppendix()
  version = task.getVersion()
  classifier = task.getClassifier()
  extension = task.getExtension()
  archiveName = task.getArchiveName()
  classpath = task.getSource()
 }

 public Object getBaseName() {
  return baseName
 }

 public void setBaseName(Object baseName) {
  this.baseName = baseName
 }

 public Object getAppendix() {
  return appendix
 }

 public void setAppendix(Object appendix) {
  this.appendix = appendix
 }

 public Object getVersion() {
  return version
 }

 public void setVersion(Object version) {
  this.version = version
 }

 public Object getClassifier() {
  return classifier
 }

 public void setClassifier(Object classifier) {
  this.classifier = classifier
 }

 public Object getExtension() {
  return extension
 }

 public void setExtension(Object extension) {
  this.extension = extension
 }

 public Object getClasspath() {
  if (classpath == null) {
   classpath = project.files((Object)new String[0])
  }
  return classpath
 }

 public void setClasspath(Object classpath) {
  this.classpath = classpath
 }

 public boolean isArchiveSet() {
  return archiveSet
 }

 public void setArchiveSet(boolean archiveSet) {
  this.archiveSet = archiveSet
 }

 public Object getArchiveName() {
  return archiveName
 }

 public void setArchiveName(Object archiveName) {
  this.archiveName = archiveName
  archiveSet = true
 }
}
