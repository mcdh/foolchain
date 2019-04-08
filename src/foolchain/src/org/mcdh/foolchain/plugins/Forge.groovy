package org.mcdh.foolchain.plugins

import org.gradle.api.Action
import org.gradle.api.Task

import org.mcdh.toolchain.tasks.ExtractConfigTask

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
  extractNatives.setOut(delayedFile(Constants.NATIVES_DIR))
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
}
