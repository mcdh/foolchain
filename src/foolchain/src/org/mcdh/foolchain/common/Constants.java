package org.mcdh.foolchain.common;

import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import groovy.lang.Closure;
import org.gradle.api.Project;
import org.mcdh.foolchain.utils.StringUtils;
import org.mcdh.foolchain.utils.json.version.OS;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class Constants {
 // OS
 public enum SystemArch {
  BIT_32, BIT_64;

  public String toString() {
   return StringUtils.lower(name()).replace("bit_", "");
  }
 }

 public static final OS OPERATING_SYSTEM = OS.CURRENT;
 public static final SystemArch SYSTEM_ARCH = getArch();
 public static final String HASH_FUNC = "MD5";
 public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";

 // extension nam
 public static final String EXT_NAME_MC = "Minecraft";
 public static final String EXT_NAME_JENKINS = "jenkins";

 @SuppressWarnings("serial")
 public static final Closure<Boolean> CALL_FALSE = new Closure<Boolean>(null) {
  public Boolean call(Object o) {
   return false;
  }
 };

 // urls
 public static final String MC_JSON_URL = "http://s3.amazonaws.com/Minecraft.Download/versions/{MC_VERSION}/{MC_VERSION}.json";
 public static final String MC_JAR_URL = "http://s3.amazonaws.com/Minecraft.Download/versions/{MC_VERSION}/{MC_VERSION}.jar";
 public static final String MC_SERVER_URL = "http://s3.amazonaws.com/Minecraft.Download/versions/{MC_VERSION}/minecraft_server.{MC_VERSION}.jar";
 public static final String MCP_URL = "http://files.minecraftforge.net/fernflower-fix-1.0.zip";
 public static final String ASSETS_URL = "http://resources.download.Minecraft.net";
 public static final String LIBRARY_URL = "https://libraries.Minecraft.net/";
 public static final String FORGE_MAVEN = "http://files.minecraftforge.net/maven";
 public static final String ASSETS_INDEX_URL = "https://s3.amazonaws.com/Minecraft.Download/indexes/{ASSET_INDEX}.json";

 // MCP things
 public static final String CONFIG_MCP_DATA = "mcpSnapshotDataConfig";
 public static final String MCP_JSON_URL = "http://export.mcpbot.bspk.rs/versions.json";

 //Moved to UserConstants
//    // things in the cache dir.
//    public static final String NATIVES_DIR      = "{CACHE_DIR}/Minecraft/net/Minecraft/minecraft_natives/{MC_VERSION}";
//    public static final String MCP_DATA_DIR     = "{CACHE_DIR}/Minecraft/de/oceanlabs/mcp/mcp_{MAPPING_CHANNEL}/{MAPPING_VERSION}/";
//    public static final String JAR_CLIENT_FRESH = "{CACHE_DIR}/Minecraft/net/Minecraft/Minecraft/{MC_VERSION}/Minecraft-{MC_VERSION}.jar";
//    public static final String JAR_SERVER_FRESH = "{CACHE_DIR}/Minecraft/net/Minecraft/minecraft_server/{MC_VERSION}/minecraft_server-{MC_VERSION}.jar";
//    public static final String JAR_MERGED       = "{CACHE_DIR}/Minecraft/net/Minecraft/minecraft_merged/{MC_VERSION}/minecraft_merged-{MC_VERSION}.jar";
//    public static final String FERNFLOWER       = "{CACHE_DIR}/Minecraft/fernflower-fixed.jar";
//    public static final String EXCEPTOR         = "{CACHE_DIR}/Minecraft/exceptor.jar";
//    public static final String ASSETS           = "{CACHE_DIR}/Minecraft/assets";
//    public static final String JSONS_DIR        = "{CACHE_DIR}/Minecraft/versionJsons";
//    public static final String VERSION_JSON     = JSONS_DIR + "/{MC_VERSION}.json";

 // util
 public static final String NEWLINE = System.getProperty("line.separator");

 // helper methods
 public static File cacheFile(Project project, String... otherFiles) {
  return Constants.file(project.getGradle().getGradleUserHomeDir(), otherFiles);
 }

 public static File file(File file, String... otherFiles) {
  String othersJoined = Joiner.on('/').join(otherFiles);
  return new File(file, othersJoined);
 }

 public static File file(String... otherFiles) {
  String othersJoined = Joiner.on('/').join(otherFiles);
  return new File(othersJoined);
 }

 public static List<String> getClassPath() {
//        URL[] urls = ((URLClassLoader) DevExtension.class.getClassLoader()).getURLs();
  URL[] urls = ((URLClassLoader)Constants.class.getClassLoader()).getURLs();

  ArrayList<String> list = new ArrayList<String>();
  for (URL url : urls) {
   list.add(url.getPath());
  }
  return list;
 }

 public static File getMinecraftDirectory() {
  String userDir = System.getProperty("user.home");

  switch (OPERATING_SYSTEM) {
   case LINUX:
    return new File(userDir, ".Minecraft/");
   case WINDOWS:
    String appData = System.getenv("APPDATA");
    String folder = appData != null ? appData : userDir;
    return new File(folder, ".Minecraft/");
   case OSX:
    return new File(userDir, "Library/Application Support/Minecraft");
   default:
    return new File(userDir, "Minecraft/");
  }
 }

 private static SystemArch getArch() {
  String name = StringUtils.lower(System.getProperty("os.arch"));
  if (name.contains("64")) {
   return SystemArch.BIT_64;
  } else {
   return SystemArch.BIT_32;
  }
 }

 public static String hash(File file) {
  if (file.getPath().endsWith(".zip") || file.getPath().endsWith(".jar"))
   return hashZip(file, HASH_FUNC);
  else
   return hash(file, HASH_FUNC);
 }

 public static List<String> hashAll(File file) {
  LinkedList<String> list = new LinkedList<String>();

  if (file.isDirectory()) {
   for (File f : file.listFiles())
    list.addAll(hashAll(f));
  } else if (!file.getName().equals(".cache"))
   list.add(hash(file));

  return list;
 }

 public static String hash(File file, String function) {

  try {
   InputStream fis = new FileInputStream(file);
   byte[] array = ByteStreams.toByteArray(fis);
   fis.close();

   return hash(array, function);
  } catch (Exception e) {
   e.printStackTrace();
  }

  return null;
 }

 public static String hashZip(File file, String function) {
  try {
   MessageDigest hasher = MessageDigest.getInstance(function);

   ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
   ZipEntry entry = null;
   while ((entry = zin.getNextEntry()) != null) {
    hasher.update(entry.getName().getBytes());
    hasher.update(ByteStreams.toByteArray(zin));
   }
   zin.close();

   byte[] hash = hasher.digest();


   // convert to string
   String result = "";

   for (int i = 0; i < hash.length; i++) {
    result += Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(1);
   }
   return result;
  } catch (Exception e) {
   e.printStackTrace();
  }

  return null;
 }

 public static String hash(String str) {
  return hash(str.getBytes());
 }

 public static String hash(byte[] bytes) {
  return hash(bytes, HASH_FUNC);
 }

 public static String hash(byte[] bytes, String function) {
  try {
   MessageDigest complete = MessageDigest.getInstance(function);
   byte[] hash = complete.digest(bytes);

   String result = "";

   for (int i = 0; i < hash.length; i++) {
    result += Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(1);
   }
   return result;
  } catch (Exception e) {
   e.printStackTrace();
  }

  return null;
 }

 public static PrintStream getTaskLogStream(Project project, String name) {
  final File taskLogs = new File(project.getBuildDir(), "taskLogs");
  taskLogs.mkdirs();
  final File logFile = new File(taskLogs, name);
  logFile.delete(); //Delete the old log
  try {
   return new PrintStream(logFile);
  } catch (FileNotFoundException ignored) {
  }
  return null; // Should never get to here
 }

 /**
  * Throws a null runtime exception if the resource isnt found.
  *
  * @param resource String name of the resource your looking for
  * @return URL
  */
 public static URL getResource(String resource) {
//        ClassLoader loader = BaseExtension.class.getClassLoader();
  ClassLoader loader = Constants.class.getClassLoader();

  if (loader == null)
   throw new RuntimeException("ClassLoader is null! IMPOSSIBRU");

  URL url = loader.getResource(resource);

  if (url == null)
   throw new RuntimeException("Resource " + resource + " not found");

  return url;
 }
}
