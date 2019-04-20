package org.mcdh.foolchain.plugins

import com.google.common.base.Throwables
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.logging.Logger
import org.mcdh.foolchain.common.Constants
import org.mcdh.foolchain.tasks.ExtractConfigTask
import org.mcdh.foolchain.common.UserConstants
import org.mcdh.foolchain.utils.json.JsonFactory

import static org.mcdh.foolchain.common.UserConstants.*

//Sets up an environment with FML and Forge
class Forge extends PluginBase {
 public static final String name = 'forge'
 private boolean hasAppliedJson = false
 private boolean hasScalaBefore = false
 private boolean hasGroovyBefore = false

 @Override
 boolean canOverlay(Class<?> clazz) {
  return false
 }

 @Override
 String getName() {
  return name
 }

 @Override
 String getApiGroup() {
  return 'net.minecraftforge'
 }

 @Override
 String getApiName() {
  return 'forge'
 }

 @Override
 String getClientTweaker() {
  return 'fml.common.launcher.FMLTweaker'
 }

 @Override
 String getServerTweaker() {
  return 'fml.common.launcher.FMLServerTweaker'
 }

 @Override
 String getClientRunClass() {
  return 'net.minecraft.launchwrapper.Launch'
 }

 @Override
 String getServerRunClass() {
  return getClientRunClass()
 }

 @Override
 void apply() {
  applyExternalPlugin('java')
  applyExternalPlugin('maven')
  applyExternalPlugin('idea')

  hasScalaBefore = plugins.hasPlugin("scala")
  hasGroovyBefore = plugins.hasPlugin("groovy")

  configureDeps()
 }

 protected void configureDeps() {
  // create configs
  project.getConfigurations().create(CONFIG_USERDEV)
  project.getConfigurations().create(CONFIG_NATIVES)
  project.getConfigurations().create(CONFIG_START)
  project.getConfigurations().create(CONFIG_DEPS)
  project.getConfigurations().create(CONFIG_MC)

  // special userDev stuff
  ExtractConfigTask extractUserDev = makeTask("extractUserDev", ExtractConfigTask.class)
  extractUserDev.setOut(delayedFile("{USER_DEV}"))
  extractUserDev.setConfig(CONFIG_USERDEV)
  extractUserDev.setDoesCache(true)
  extractUserDev.dependsOn("getVersionJson")
  extractUserDev.doLast(new Action<Task>()
  {
   @Override
   void execute(Task arg0) {
    readAndApplyJson(getDevJson().call(), CONFIG_DEPS, CONFIG_NATIVES, arg0.getLogger())
   }
  })
  project.getTasks().findByName("getAssetsIndex").dependsOn("extractUserDev")

  // special native stuff
  ExtractConfigTask extractNatives = makeTask("extractNatives", ExtractConfigTask.class)
  extractNatives.setOut(delayedFile(constants.NATIVES_DIR))
  extractNatives.setConfig(CONFIG_NATIVES)
  extractNatives.exclude("META-INF/**", "META-INF/**")
  extractNatives.doesCache()
  extractNatives.dependsOn("extractUserDev")

  // special gradleStart stuff
  project.getDependencies().add(CONFIG_START, project.files(delayedFile(getStartDir())))

  // extra libs folder.
  project.getDependencies().add("compile", project.fileTree("libs"))

  // make MC dependencies into normal compile classpath
  project.getDependencies().add("compile", project.getConfigurations().getByName(CONFIG_DEPS))
  project.getDependencies().add("compile", project.getConfigurations().getByName(CONFIG_MC))
  project.getDependencies().add("runtime", project.getConfigurations().getByName(CONFIG_START))
 }

 private void readAndApplyJson(File file, String depConfig, String nativeConfig, Logger log) {
  if (version == null) {
   try {
    version = JsonFactory.loadVersion(file, project.file(constants.JSONS_DIR))
   } catch (Exception e) {
    log.error("" + file + " could not be parsed")
    Throwables.propagate(e)
   }
  }

  if (hasAppliedJson) {
   return
  }

  // apply the dep info.
  DependencyHandler handler = project.getDependencies()

  // actual dependencies
  if (project.getConfigurations().getByName(depConfig).getState() == Configuration.State.UNRESOLVED) {
   for (org.mcdh.foolchain.utils.json.version.Library lib : version.getLibraries()) {
    if (lib.natives == null)
     handler.add(depConfig, lib.getArtifactName())
   }
  } else
   log.debug("RESOLVED: " + depConfig)

  // the natives
  if (project.getConfigurations().getByName(nativeConfig).getState() == Configuration.State.UNRESOLVED) {
   for (org.mcdh.foolchain.utils.json.version.Library lib : version.getLibraries()) {
    if (lib.natives != null) {
     handler.add(nativeConfig, lib.getArtifactName())
    }
   }
  } else {
   log.debug("RESOLVED: " + nativeConfig)
  }

  hasAppliedJson = true
 }
}
