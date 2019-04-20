package org.mcdh.foolchain.plugins

import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.logging.Logger
import org.gradle.api.logging.LoggingManager
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.plugins.PluginContainer

import org.mcdh.foolchain.common.Constants
import org.mcdh.foolchain.common.ToolchainConfigurationExtension
import org.mcdh.foolchain.common.UserConstants
import org.mcdh.foolchain.utils.FileLogListener
import org.mcdh.foolchain.utils.json.version.Version

abstract class PluginBase implements Plugin<Project> {
 public Project project
 public Logger globalLogger
 public ExtensionContainer extensionContainer
 public PluginContainer plugins
 public ToolchainConfigurationExtension toolchainExtension
 //For legacy compatibility
 public UserConstants constants
 public Version version

 @Override
 void apply(final Project project) {
  this.project = project
  this.globalLogger = project.getLogger()
  this.plugins = project.getPlugins()
  this.extensionContainer = project.getExtensions()
  this.constants = new UserConstants(
    toolchainExtension,
    //{RUN_SERVER_TWEAKER}
    getClientTweaker(),
    //{RUN_CLIENT_TWEAKER}
    getServerTweaker(),
    //{RUN_BOUNCE_CLIENT}
    getClientRunClass(),
    //{RUN_BOUNCE_SERVER}
    getServerRunClass(),
    //{API_GROUP}
    getApiGroup(),
    //{API_NAME}
    getApiName(),
//    //{API_VERSION}
//    getApiVersion(),
    //{CACHE_DIR}
    "${project.getGradle().getGradleUserHomeDir().getAbsolutePath().replace('\\', '/')}/caches"
  )
  checkForOverlays()
  //Check for illegal characters in the path
  if (project.getBuildDir().absolutePath.contains('!')) {
//   globalLogger.error("The build path contains '!' which denotes archive paths in java and thus must be removed.")
   throw new ProjectConfigurationException(
    "The build path contains '!' which denotes archive paths in java and thus must be removed.",
    (Throwable)null
   )
  }
  //Create build extension objects
  toolchainExtension = extensionContainer.create(Constants.EXT_NAME_MC, ToolchainConfigurationExtension.class)
  //Add build repositories
  project.allprojects {
   final Project p ->
    addMavenRepository(p, 'forge', Constants.FORGE_MAVEN)
    addMavenRepository(p, 'minecraft', Constants.LIBRARY_URL)
    p.getRepositories().mavenCentral()
  }
  apply()
 }

 private void checkForOverlays() {
  final PluginContainer plugins = project.getPlugins()
  for (final Plugin p1 : plugins) {
   for (final Plugin p2 : plugins) {
    if (p1 instanceof PluginBase && p2 instanceof PluginBase) {
     final PluginBase pb1 = (PluginBase)p1, pb2 = (PluginBase)pb2
     if (!((PluginBase)p1).canOverlay(p2.getClass())) {
      throw new ProjectConfigurationException(
       "The '${pb1.getName()}' plugin is incompatible with the '${pb2.getName()}' plugin!",
       (Throwable)null
      )
     }
    }
   }
  }
 }

 abstract boolean canOverlay(final Class<?> clazz)

 public <T extends PluginBase> void applyOverlay(final T t) {}

 abstract String getName()

 abstract void apply()

 void setupLogger() {
  File projectCacheDir = project.getGradle().getStartParameter().getProjectCacheDir()
  if (projectCacheDir == null) {
   projectCacheDir = project.file('.gradle')
  }
  final FileLogListener listener = new FileLogListener(new File(projectCacheDir, 'gradle.log'))
  final LoggingManager logger = project.getLogging()
  logger.addStandardOutputListener(listener)
  logger.addStandardErrorListener(listener)
  project.getGradle().addBuildListener(listener)
 }

 final <T extends Task> T makeTask(final String name, final Class<T> type) {
  return makeTask(project, name, type)
 }

 final void applyExternalPlugin(final String plugin) {
  final HashMap<String, Object> map = new HashMap<>()
  map.put("plugin", plugin)
  project.apply(map)
 }

 abstract String getApiGroup();

 abstract String getApiName();

// abstract String getApiVersion();

 abstract String getClientTweaker();

 abstract String getServerTweaker();

 abstract String getClientRunClass();

 abstract String getServerRunClass();
}