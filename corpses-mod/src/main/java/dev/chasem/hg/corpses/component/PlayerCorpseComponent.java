package dev.chasem.hg.corpses.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Marks an NPC entity as a player corpse and tracks ownership + lifecycle state.
 */
public class PlayerCorpseComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, PlayerCorpseComponent> COMPONENT_TYPE = new ComponentType<>();

    public static ComponentType<EntityStore, PlayerCorpseComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    public static void setComponentType(ComponentType<EntityStore, PlayerCorpseComponent> componentType) {
        COMPONENT_TYPE = componentType;
    }

    @Nonnull
    private final UUID playerUuid;

    @Nonnull
    private final String playerName;

    private final long createdAtMillis;
    private final long expiresAtMillis;
    private final boolean looted;

    /**
     * Default constructor required by the component registry.
     */
    public PlayerCorpseComponent() {
        this(new UUID(0L, 0L), "unknown", 0L, 0L, false);
    }

    public PlayerCorpseComponent(
            @Nonnull UUID playerUuid,
            @Nonnull String playerName,
            long createdAtMillis,
            long expiresAtMillis,
            boolean looted
    ) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.createdAtMillis = createdAtMillis;
        this.expiresAtMillis = expiresAtMillis;
        this.looted = looted;
    }

    @Nonnull
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    @Nonnull
    public String getPlayerName() {
        return playerName;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public long getExpiresAtMillis() {
        return expiresAtMillis;
    }

    public boolean isLooted() {
        return looted;
    }

    @Nonnull
    public PlayerCorpseComponent withLooted(boolean newLooted) {
        if (newLooted == this.looted) {
            return this;
        }
        return new PlayerCorpseComponent(playerUuid, playerName, createdAtMillis, expiresAtMillis, newLooted);
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new PlayerCorpseComponent(playerUuid, playerName, createdAtMillis, expiresAtMillis, looted);
    }
}
