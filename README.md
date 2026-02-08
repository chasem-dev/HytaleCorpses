# HytaleCorpses

Standalone Hytale server mod that adds persistent player corpses with a loot UI:

- On player death, spawns an interactable corpse NPC (`HC_Player_Corpse`)
- Copies the dead player's inventory into the corpse
- Opens a dual-panel loot page (player inventory on the left, corpse inventory on the right)

## Build

```bash
./gradlew build
```

The fat jar is produced at `corpses-mod/build/libs/corpses-mod-*-all.jar`.

## Config (System Properties)

- `-Dcorpses.corpse.lifetimeSeconds=300` (default: 300)

