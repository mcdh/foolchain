# foolchain
A rewrite of MinecrftForge's Forge Gradle build toolchian. Currently, foolchain only supports the same Minecraft and Forge versions as Forge Gradle 1.2.

## Plugins
#### Currently implemented plugins:
| Plugin | Description |
| :--- | :--- |
| `forge` | Plugin for developing Forge mods. Includes both Minecraft Forge and Forge Mod Loader. |
| `forgedev` | Plugin for developing Minecraft Forge itself. Sets up a development environment with the Forge sources. |
| `fml` | Plugin for developing FML mods. Only includes Forge Mod Loader. |
| `fmldev` | Plugin for developing FML. Sets up a development environment with the FML sources. |
| `revforge` | Plugin for patching closed-source mods. Given an input jar, the sources will automatically be deobfuscated, decompiled and setup in a standard `forge`-like development environment. Any changes made to the decompiled source will automatically be generated into patches, allowing you to distribute the changes without the source itself. |

#### Planned plugins:
| Plugin | Description |
| :--- | :--- |
| `revforgedev` | Plugin for developing closed-source distributions of Forge. |
| `launch4j` | Plugin for developing launch4j. |
| `liteloader` | Plugin for developing LiteLoader. |
| `cauldron` | Plugin for developing cauldron. |
| `mcedu` | Plugin for developing Minecraft Education Edition. |

# Tasks
| Task | Description |
| :--- | :--- |
