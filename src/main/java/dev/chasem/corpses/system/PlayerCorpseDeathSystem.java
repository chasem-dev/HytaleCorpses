package dev.chasem.corpses.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.PlayerSkin;
import dev.chasem.corpses.util.PlayerCorpseSpawner;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

/**
 * Spawns a persistent corpse NPC when a player dies.
 * Captures the player's inventory and transfers it to the corpse.
 * Defers the spawn to next tick to avoid Store processing conflicts.
 */
public class PlayerCorpseDeathSystem extends DeathSystems.OnDeathSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return RootDependency.firstSet();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull DeathComponent deathComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // Capture all player data now before it might change
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRefComp = store.getComponent(ref, PlayerRef.getComponentType());
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
        PlayerSkinComponent skinComponent = store.getComponent(ref, PlayerSkinComponent.getComponentType());

        if (player == null || playerRefComp == null || transform == null || headRotation == null) {
            return;
        }

        // Capture values
        UUID playerUuid = playerRefComp.getUuid();
        String playerName = player.getDisplayName();
        Vector3d position = new Vector3d(transform.getPosition());
        float yaw = headRotation.getRotation().getYaw();
        PlayerSkin skin = skinComponent != null ? skinComponent.getPlayerSkin() : null;

        // Capture inventory snapshot BEFORE clearing (items are cloned)
        Inventory inventory = player.getInventory();
        PlayerCorpseSpawner.InventorySnapshot inventorySnapshot =
                PlayerCorpseSpawner.InventorySnapshot.fromInventory(inventory);

        // Clear player's inventory (they respawn with nothing)
        inventory.clear();
        LOGGER.atInfo().log("[Corpse] Cleared inventory for %s on death", playerName);

        // Defer spawn to next tick to avoid "Store is currently processing" error
        store.getExternalData().getWorld().execute(() -> {
            PlayerCorpseSpawner.spawnCorpseForPlayer(
                    store,
                    playerUuid,
                    playerName,
                    position,
                    yaw,
                    skin,
                    "death",
                    inventorySnapshot
            );
        });
    }
}
