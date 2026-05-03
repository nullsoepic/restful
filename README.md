
Restful
=======

A NeoForge mod that allows players to have multiple respawn points. When you die, choose where to respawn from your saved locations.

Features:
- Track multiple beds (default: 5)
- Works with beds (Overworld) and Respawn Anchors (Nether)
- Select which bed to respawn at from a visual grid on death
- Commands: `/restful list`, `/restful remove <index>`, `/restful clear`

Death Flow:
-----------
1. Player dies → Shows vanilla death screen
2. Click "Respawn" → Shows backup bed selection grid
3. Click a bed tile → Validates bed (loads chunk if needed) → Respawns there
4. If bed is destroyed → Shows updated selection without that bed

Technical Notes:
----------------
**Chunk Loading**: When selecting a bed from another dimension (e.g., Nether anchor while in Overworld), the mod loads the target chunk to validate the bed still exists. This is a full chunk load (same as when a player teleports there). The chunk remains loaded as part of normal Minecraft chunk management after respawn.

**Data Storage**: Uses NeoForge DataAttachments for per-player storage. Beds persist across deaths and are copied when players respawn/clone.

Installation
============

Clone this repository and open in IntelliJ IDEA or Eclipse.

Run `gradlew --refresh-dependencies` if missing libraries.  
Run `gradlew clean` to reset build state.

Mapping Names
=============
By default, uses official Mojang mappings. License: https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional Resources
====================
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/
