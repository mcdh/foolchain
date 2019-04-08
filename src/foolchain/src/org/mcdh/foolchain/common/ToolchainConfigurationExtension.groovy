package org.mcdh.foolchain.common

import com.google.common.base.Strings

class ToolchainConfigurationExtension {
 public String version = "null"
 public String apiVersion = version
 public String mcpVersion = "unknown"
 public String runDir = "run"
 public LinkedList<String> srgExtra = new LinkedList<>()

 public Map<String, Map<String, int[]>> mcpJson
 public boolean mappingsSet = false
 public String mappingsChannel = null
 public int mappingsVersion = -1
 public String customVersion = null

 public boolean isDecomp = false

 String getMappings() {
  return "${mappingsChannel}_${customVersion == null ? mappingsVersion : customVersion}"
 }

// String getMappingsVersion() {
//  return customVersion == null ? "${mappingsVersion}" : customVersion
// }

 String getMappingsChannelNoSubtype() {
  final int underscore = mappingsChannel.indexOf('_')
  if (underscore <= 0) {
   return mappingsChannel
  }
  return mappingsChannel.substring(0, underscore)
 }

 void setMappings(String mappings) {
  if (Strings.isNullOrEmpty(mappings)) {
   mappingsChannel = null
   mappingsVersion = -1
   return
  }

  mappings = mappings.toLowerCase()

  if (!mappings.contains("_")) {
   throw new IllegalArgumentException("Mappings must be in format 'channel_version'. eg: snapshot_20140910")
  }

  int index = mappings.lastIndexOf('_')
  mappingsChannel = mappings.substring(0, index)
  customVersion = mappings.substring(index + 1)

  if (!customVersion.equals("custom")) {
   try {
    mappingsVersion = Integer.parseInt(customVersion)
    customVersion = null
   } catch (NumberFormatException e) {
    throw new RuntimeException("The mappings version must be a number! eg: channel_### or channel_custom (for custom mappings).")
   }
  }

  mappingsSet = true

  //Check
  checkMappings()
 }

 /**Checks that the set mappings are valid based on the channel, version, and MC version.
  * If the mappings are invalid, this method will throw a runtime exception.
  */
 protected void checkMappings() {
  //Mappings or mc version are null
  if (!mappingsSet || "null".equals(version) || Strings.isNullOrEmpty(version) || customVersion != null) {
   return
  }

  //Check if it exists
  Map<String, int[]> versionMap = mcpJson.get(version)
  if (versionMap == null)
   throw new RuntimeException("There are no mappings for MC " + version)

  String channel = getMappingsChannelNoSubtype()
  int[] channelList = versionMap.get(channel)
  if (channelList == null)
   throw new RuntimeException("There is no such MCP mapping channel named " + channel)

  //All is well with the world
  if (searchArray(channelList, mappingsVersion)) {
   return
  }

  //If it gets here, it wasn't found. Now we try to actually find it.
  for (Map.Entry<String, Map<String, int[]>> mcEntry : mcpJson.entrySet()) {
   for (Map.Entry<String, int[]> channelEntry : mcEntry.getValue().entrySet()) {
    //Found it!
    if (searchArray(channelEntry.getValue(), mappingsVersion)) {
     final boolean rightMc = mcEntry.getKey() == version
     final boolean rightChannel = channelEntry.getKey() == channel
     if (rightChannel && !rightMc) {
      //Right channel, but wrong mc
      throw new RuntimeException("This mapping '${getMappings()}' exists only for MC '${mcEntry.getKey()}'!")
     } else if (rightMc && !rightChannel) {
      //Right MC , but wrong channel
      throw new RuntimeException("This mapping '${getMappings()}' doesnt exist! Perhaps you meant '${channelEntry.getKey()}_${mappingsVersion}'?")
     }
    }
   }
  }
  //Wasn't found
  throw new RuntimeException("The specified mapping '${getMappings()}' does not exist!")
 }

 private static boolean searchArray(int[] array, int key) {
  Arrays.sort(array)
  final int foundIndex = Arrays.binarySearch(array, key)
  return foundIndex >= 0 && array[foundIndex] == key
 }
}
