package dev.chasem.corpses;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import dev.chasem.corpses.command.TestDeathCommand;
import dev.chasem.corpses.component.PlayerCorpseComponent;
import dev.chasem.corpses.npc.builders.BuilderActionOpenCorpseLoot;
import dev.chasem.corpses.system.PlayerCorpseCleanupSystem;
import dev.chasem.corpses.system.PlayerCorpseDeathSystem;

/**
 * Standalone player corpse mod:
 * - spawns an interactable corpse NPC on death
 * - transfers inventory to corpse
 * - provides a dual-panel loot UI
 */
public class CorpsesPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static ComponentType<EntityStore, PlayerCorpseComponent> playerCorpseComponentType;

    public CorpsesPlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("[HytaleCorpses] Initializing...");
    }

    @Override
    protected void setup() {
        // Register PlayerCorpseComponent for persistent corpse entities.
        playerCorpseComponentType = getEntityStoreRegistry().registerComponent(
                PlayerCorpseComponent.class,
                PlayerCorpseComponent::new
        );
        PlayerCorpseComponent.setComponentType(playerCorpseComponentType);

        // Register custom NPC action used by the corpse role JSON.
        NPCPlugin.get().registerCoreComponentType("OpenCorpseLoot", BuilderActionOpenCorpseLoot::new);

        // Register corpse systems.
        getEntityStoreRegistry().registerSystem(new PlayerCorpseDeathSystem());
        getEntityStoreRegistry().registerSystem(new PlayerCorpseCleanupSystem());

        // Debug/test command.
        getCommandRegistry().registerCommand(new TestDeathCommand());

        LOGGER.atInfo().log("[HytaleCorpses] Setup complete");
    }
}

