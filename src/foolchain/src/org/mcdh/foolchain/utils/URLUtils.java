package org.mcdh.foolchain.utils;

import org.gradle.api.file.FileCollection;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public final class URLUtils {
 public static URL[] toUrls(FileCollection collection) {
  return (URL[])collection
   .getFiles()
   .stream()
   .map(f -> {
    URL url = null;
    try {
     url = f.toURI().toURL();
    } catch (MalformedURLException e) {}
    return url;
   })
   .filter(Objects::nonNull)
   .toArray();
//  ArrayList<URL> urls = new ArrayList<URL>();
//
//  for (File file : collection.getFiles()) {
//   urls.add(file.toURI().toURL());
//  }
//
//  return urls.toArray(new URL[urls.size()]);
 }
}
