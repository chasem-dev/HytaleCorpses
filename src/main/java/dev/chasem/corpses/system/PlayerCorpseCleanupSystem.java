package dev.chasem.corpses.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.chasem.corpses.component.PlayerCorpseComponent;

import javax.annotation.Nonnull;

/**
 * Removes corpse NPCs after their configured lifetime expires.
 */
public class PlayerCorpseCleanupSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerCorpseComponent.getComponentType();
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        PlayerCorpseComponent corpseComponent = archetypeChunk.getComponent(index, PlayerCorpseComponent.getComponentType());
        if (corpseComponent == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now < corpseComponent.getExpiresAtMillis()) {
            return;
        }

        Ref<EntityStore> corpseRef = archetypeChunk.getReferenceTo(index);

        // Remove the corpse NPC
        commandBuffer.removeEntity(corpseRef, RemoveReason.REMOVE);
        LOGGER.atFine().log("[Corpse] Removed expired corpse for %s", corpseComponent.getPlayerName());
    }
}
