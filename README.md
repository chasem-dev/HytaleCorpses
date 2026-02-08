# HytaleCorpses

<a href="https://x.com/chasemdev"><img alt="" title="Download" src="https://camo.githubusercontent.com/e8b78ea70d0e398cf80839dfe0f18cf3fcb555b3cce43acf3936f761c2826b78/68747470733a2f2f696d672e736869656c64732e696f2f62616467652f63686173656d6465762d2532333030303030302e7376673f7374796c653d666f722d7468652d6261646765266c6f676f3d58266c6f676f436f6c6f723d7768697465"/></a>

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

