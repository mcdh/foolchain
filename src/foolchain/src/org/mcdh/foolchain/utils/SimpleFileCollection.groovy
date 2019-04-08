package org.mcdh.foolchain.utils

import org.gradle.api.internal.file.collections.FileCollectionAdapter
import org.gradle.api.internal.file.collections.ListBackedFileSet
import org.gradle.api.internal.file.collections.MinimalFileSet

class SimpleFileCollection extends FileCollectionAdapter {
 public SimpleFileCollection(final MinimalFileSet fileSet) {
  super(fileSet)
 }

 public SimpleFileCollection(final File... files) {
  super(new ListBackedFileSet(files))
 }

 public SimpleFileCollection(final Collection<File> files) {
  super(new ListBackedFileSet(files))
 }
}
