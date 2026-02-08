# HytaleCorpses

![HytaleCorpses](https://media.discordapp.net/attachments/1467336738992160864/1470080319343427677/image.png?ex=6989fe7c&is=6988acfc&hm=9aaf1fe2f77935b6994dfb3e018a81af7ead14a6a655d3d19f81847838ffa5dd&=&format=webp&quality=lossless&width=2282&height=1536)

Standalone Hytale server mod that adds persistent player corpses with a loot UI:

- On player death, spawns an interactable corpse NPC (`HC_Player_Corpse`)
- Copies the dead player's inventory into the corpse
- Opens a dual-panel loot page (player inventory on the left, corpse inventory on the right)

## Build

```bash
./gradlew build
```

The fat jar is produced at `build/libs/HytaleCorpses-*-all.jar`.

## Config (System Properties)

- `-Dcorpses.corpse.lifetimeSeconds=300` (default: 300)

