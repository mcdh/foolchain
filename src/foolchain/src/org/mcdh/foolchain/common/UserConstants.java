package org.mcdh.foolchain.common;

public final class UserConstants {
 //TODO move to Constants
 //Global constants
 public static final String
  CONFIG_USERDEV = "userDevPackageDepConfig",
  CONFIG_NATIVES = "minecraftNatives",
  CONFIG_START = "forgeGradleStartClass",
  CONFIG_DEPS = "minecraftDeps",
  CONFIG_MC = "minecraft",
  CLASSIFIER_DEOBF_SRG = "srg", //Classifiers
  CLASSIFIER_DECOMPILED = "decomp",
  CLASSIFIER_SOURCES = "sources",
  GRADLE_START_CLIENT = "GradleStart", //Gradle constants
  GRADLE_START_SERVER = "GradleStartServer";

 //Context dependents
 private final ToolchainConfigurationExtension extension;

 public final String
  apiGroup,
  apiName,
 //  apiVersion,
  buildDir,
  userDev,
  srgDir,
  mcpDataDir,
 //  mappingChannel,
//  mappingVersion,
  cacheDir,
  fmlVersion;

 //Context constants
 public final String
  RUN_CLIENT_TWEAKER,
  RUN_SERVER_TWEAKER,
  RUN_BOUNCE_CLIENT,
  RUN_BOUNCE_SERVER,
  MC_VERSION,
  FML_CACHE_DIR,
  API_PATH,
  API_VERSION,
  MAPPING_APPENDAGE,
  MAPPING_CHANNEL,
  MAPPING_VERSION,
  API_CACHE_DIR,
  SRG_CACHE_DIR,
  USER_DEV_CACHE_DIR,
  FORGE_JAVADOC_URL,
  NATIVES_DIR_OLD,
  SOURCES_DIR,
  CONF_DIR,
  MERGE_CFG,
  MCP_PATCH_DIR,
  ASTYLE_CFG,
  PACKAGED_SRG,
  PACKAGED_EXC,
  EXC_JSON,
  DEOBF_SRG_SRG,
  DEOBF_MCP_SRG,
  DEOBF_SRG_MCP_SRG,
  REOBF_SRG,
  REOBF_NOTCH_SRG,
  EXC_SRG,
  EXC_MCP,
  METHOD_CSV,
  FIELD_CSV,
  PARAM_CSV,
  DIRTY_DIR,
  RECOMP_SRC_DIR,
  RECOMP_CLS_DIR,
  //From Constants
  NATIVES_DIR,
  MCP_DATA_DIR,
  JAR_CLIENT_FRESH,
  JAR_SERVER_FRESH,
  JAR_MERGED,
  FERNFLOWER,
  EXCEPTOR,
  ASSETS,
  JSONS_DIR,
  VERSION_JSON;

 public UserConstants(
  final ToolchainConfigurationExtension extension,
  final String runClientTweaker,
  final String runServerTweaker,
  final String runBounceClient,
  final String runBounceServer,
  final String apiGroup,
  final String apiName,
//  final String apiVersion,
  final String buildDir,
  final String userDev,
  final String srgDir,
  final String mcpDataDir,
//  final String mappingChannel,
//  final String mappingVersion,
  final String cacheDir
 ) {
  //Context dependents
  this.extension = extension;
  //TODO
  this.apiGroup = apiGroup;
  this.apiName = apiName;
  this.buildDir = buildDir;
  this.userDev = userDev;
  this.srgDir = srgDir;
  this.mcpDataDir = mcpDataDir;
//  this.apiVersion = apiVersion;
//  this.mappingChannel = mappingChannel;
//  this.mappingVersion = mappingVersion;
  this.cacheDir = cacheDir;
  this.fmlVersion = getFmlVersion(extension.version);

  //Context constants
  this.RUN_CLIENT_TWEAKER = runClientTweaker;
  this.RUN_SERVER_TWEAKER = runServerTweaker;
  this.RUN_BOUNCE_CLIENT = runBounceClient;
  this.RUN_BOUNCE_SERVER = runBounceServer;
  this.MC_VERSION = extension.version;
  this.FML_CACHE_DIR = cacheDir + "/minecraft/cpw/mods/fml/" + fmlVersion;
  this.API_PATH = apiGroup.replace('.', '/');
  this.API_VERSION = extension.getApiVersion();
  this.API_CACHE_DIR = cacheDir + "/minecraft/" + API_PATH + "/" + apiName + "/" + API_VERSION;
  if (extension.mappingsSet) {
   this.MAPPING_CHANNEL = extension.mappingsChannel;
   this.MAPPING_VERSION = extension.getMappingsVersion();
  } else {
   this.MAPPING_CHANNEL = "";
   this.MAPPING_VERSION = "";
  }
  this.MAPPING_APPENDAGE = MAPPING_CHANNEL + "/" + MAPPING_VERSION + "/";
  this.SRG_CACHE_DIR = API_CACHE_DIR + "/" + MAPPING_APPENDAGE + "srgs";
  this.USER_DEV_CACHE_DIR = API_CACHE_DIR + "/unpacked";
  this.FORGE_JAVADOC_URL = Constants.FORGE_MAVEN + "/net/minecraftforge/forge/" + API_VERSION + "/forge-" + API_VERSION + "-javadoc.zip";
  this.NATIVES_DIR_OLD = buildDir + "/natives";
  this.SOURCES_DIR = buildDir + "/sources";
  this.CONF_DIR = userDev + "/conf";
  this.MERGE_CFG = CONF_DIR + "/mcp_merge.cfg";
  this.MCP_PATCH_DIR = CONF_DIR + "/minecraft_ff";
  this.ASTYLE_CFG = CONF_DIR + "/astyle.cfg";
  this.PACKAGED_SRG = CONF_DIR + "/packaged.srg";
  this.PACKAGED_EXC = CONF_DIR + "/packaged.exc";
  this.EXC_JSON = CONF_DIR + "/exceptor.json";
  this.DEOBF_SRG_SRG = srgDir + "/notch-srg.srg";
  this.DEOBF_MCP_SRG = srgDir + "/notch-mcp.srg";
  this.DEOBF_SRG_MCP_SRG = srgDir + "/srg-mcp.srg";
  this.REOBF_SRG = srgDir + "/mcp-srg.srg";
  this.REOBF_NOTCH_SRG = srgDir + "/mcp-notch.srg";
  this.EXC_SRG = srgDir + "/srg.exc";
  this.EXC_MCP = srgDir + "/mcp.exc";
  this.METHOD_CSV = mcpDataDir + "/methods.csv";
  this.FIELD_CSV = mcpDataDir + "/fields.csv";
  this.PARAM_CSV = mcpDataDir + "/params.csv";
  this.DIRTY_DIR = buildDir + "/dirtyArtifacts";
  this.RECOMP_SRC_DIR = buildDir + "/tmp/recompSrc";
  this.RECOMP_CLS_DIR = buildDir + "/tmp/recompCls";
  //From Constants
  this.NATIVES_DIR = cacheDir + "/Minecraft/net/Minecraft/minecraft_natives/" + MC_VERSION;
  this.MCP_DATA_DIR = cacheDir + "/Minecraft/de/oceanlabs/mcp/mcp_" + MAPPING_CHANNEL + "/" + MAPPING_VERSION + "/";
  this.JAR_CLIENT_FRESH = cacheDir + "/Minecraft/net/Minecraft/Minecraft/" + MC_VERSION + "/Minecraft-" + MC_VERSION + ".jar";
  this.JAR_SERVER_FRESH = cacheDir + "/Minecraft/net/Minecraft/minecraft_server/" + MC_VERSION + "/minecraft_server-" + MC_VERSION + ".jar";
  this.JAR_MERGED = cacheDir + "/Minecraft/net/Minecraft/minecraft_merged/" + MC_VERSION + "/minecraft_merged-" + MC_VERSION + ".jar";
  this.FERNFLOWER = cacheDir + "/Minecraft/fernflower-fixed.jar";
  this.EXCEPTOR = cacheDir + "/Minecraft/exceptor.jar";
  this.ASSETS = cacheDir + "/Minecraft/assets";
  this.JSONS_DIR = cacheDir + "/Minecraft/versionJsons";
  this.VERSION_JSON = JSONS_DIR + "/" + MC_VERSION + ".json";
 }

 private static String getFmlVersion(final String mcVer) {
  //Hardcoded because MCP snapshots should be soon, and this will be removed
  if ("1.7.2".equals(mcVer)) {
   return "1.7.2-7.2.158.889";
  }
  if ("1.7.10".equals(mcVer)) {
   return "1.7.10-7.10.18.952";
  }
  return null;
 }
}
