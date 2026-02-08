package dev.chasem.hg.corpses.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.chasem.hg.corpses.component.PlayerCorpseComponent;
import it.unimi.dsi.fastutil.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Spawns a stationary, interactable NPC that represents a dead player.
 */
public final class PlayerCorpseSpawner {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String CORPSE_ROLE_NAME = "HC_Player_Corpse";
    private static final long DEFAULT_CORPSE_LIFETIME_SECONDS = 300L;
    private static final long CORPSE_LIFETIME_MILLIS = TimeUnit.SECONDS.toMillis(
            Long.getLong("corpses.corpse.lifetimeSeconds", DEFAULT_CORPSE_LIFETIME_SECONDS)
    );

    private PlayerCorpseSpawner() {
    }

    /**
     * Convenience overload that extracts player data from an entity reference.
     * Use when you have access to a live player entity.
     */
    @Nullable
    public static Ref<EntityStore> spawnCorpseForPlayer(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String spawnReason
    ) {
        return spawnCorpseForPlayer(store, playerRef, spawnReason, null);
    }

    /**
     * Convenience overload that extracts player data from an entity reference
     * and transfers inventory to the corpse.
     */
    @Nullable
    public static Ref<EntityStore> spawnCorpseForPlayer(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String spawnReason,
            @Nullable InventorySnapshot inventorySnapshot
    ) {
        com.hypixel.hytale.server.core.entity.entities.Player player =
                store.getComponent(playerRef, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
        com.hypixel.hytale.server.core.universe.PlayerRef playerRefComp =
                store.getComponent(playerRef, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
        com.hypixel.hytale.server.core.modules.entity.component.TransformComponent transform =
                store.getComponent(playerRef, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
        com.hypixel.hytale.server.core.modules.entity.component.HeadRotation headRotation =
                store.getComponent(playerRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
        PlayerSkinComponent skinComponent =
                store.getComponent(playerRef, PlayerSkinComponent.getComponentType());

        if (player == null || playerRefComp == null || transform == null || headRotation == null) {
            LOGGER.atWarning().log("[Corpse] Missing required components for player ref");
            return null;
        }

        return spawnCorpseForPlayer(
                store,
                playerRefComp.getUuid(),
                player.getDisplayName(),
                new Vector3d(transform.getPosition()),
                headRotation.getRotation().getYaw(),
                skinComponent != null ? skinComponent.getPlayerSkin() : null,
                spawnReason,
                inventorySnapshot
        );
    }

    /**
     * Spawns a corpse NPC with the given player data (without inventory).
     */
    @Nullable
    public static Ref<EntityStore> spawnCorpseForPlayer(
            @Nonnull Store<EntityStore> store,
            @Nonnull UUID playerUuid,
            @Nonnull String playerName,
            @Nonnull Vector3d position,
            float yaw,
            @Nullable PlayerSkin skin,
            @Nonnull String spawnReason
    ) {
        return spawnCorpseForPlayer(store, playerUuid, playerName, position, yaw, skin, spawnReason, null);
    }

    /**
     * Spawns a corpse NPC with the given player data and inventory.
     * This method should be called from outside Store processing (e.g., via World.execute()).
     */
    @Nullable
    public static Ref<EntityStore> spawnCorpseForPlayer(
            @Nonnull Store<EntityStore> store,
            @Nonnull UUID playerUuid,
            @Nonnull String playerName,
            @Nonnull Vector3d position,
            float yaw,
            @Nullable PlayerSkin skin,
            @Nonnull String spawnReason,
            @Nullable InventorySnapshot inventorySnapshot
    ) {
        NPCPlugin npcPlugin = NPCPlugin.get();
        if (!npcPlugin.hasRoleName(CORPSE_ROLE_NAME)) {
            LOGGER.atWarning().log("[Corpse] Missing NPC role '%s' - cannot spawn corpse", CORPSE_ROLE_NAME);
            return null;
        }

        Vector3d spawnPosition = new Vector3d(position.getX(), position.getY(), position.getZ());
        Vector3f spawnRotation = new Vector3f(0.0f, yaw, 0.0f);

        long createdAt = System.currentTimeMillis();
        long expiresAt = createdAt + CORPSE_LIFETIME_MILLIS;

        int roleIndex = npcPlugin.getIndex(CORPSE_ROLE_NAME);
        Pair<Ref<EntityStore>, NPCEntity> result = npcPlugin.spawnEntity(
                store,
                roleIndex,
                spawnPosition,
                spawnRotation,
                null,
                (npcComponent, holder, entityStore) -> {
                    // Corpse is invulnerable (movement is prevented by BodyMotion: Nothing in the role)
                    // NOTE: Do NOT add Frozen component - it prevents role ticking which breaks interactions
                    holder.ensureComponent(Invulnerable.getComponentType());

                    holder.putComponent(
                            PlayerCorpseComponent.getComponentType(),
                            new PlayerCorpseComponent(playerUuid, playerName, createdAt, expiresAt, false)
                    );

                    holder.putComponent(Nameplate.getComponentType(), new Nameplate(playerName));

                    // Create hybrid model BEFORE entity spawns: Player model ID (for skin support) + corpse animation sets
                    // This prevents the visual delay where NPC shows default model before hybrid is applied
                    ModelAsset playerModelAsset = ModelAsset.getAssetMap().getAsset("Player");
                    ModelAsset corpseModelAsset = ModelAsset.getAssetMap().getAsset("HC_Player_Corpse_Model");

                    if (playerModelAsset != null && corpseModelAsset != null) {
                        Model playerModel = Model.createScaledModel(playerModelAsset, 1.0f, null);
                        Model hybridModel = new Model(
                                playerModel.getModelAssetId(),
                                playerModel.getScale(),
                                playerModel.getRandomAttachmentIds(),
                                playerModel.getAttachments(),
                                playerModel.getBoundingBox(),
                                playerModel.getModel(),
                                playerModel.getTexture(),
                                playerModel.getGradientSet(),
                                playerModel.getGradientId(),
                                playerModel.getEyeHeight(),
                                playerModel.getCrouchOffset(),
                                corpseModelAsset.getAnimationSetMap(),
                                playerModel.getCamera(),
                                playerModel.getLight(),
                                playerModel.getParticles(),
                                playerModel.getTrails(),
                                playerModel.getPhysicsValues(),
                                playerModel.getDetailBoxes(),
                                playerModel.getPhobia(),
                                playerModel.getPhobiaModelAssetId()
                        );
                        holder.putComponent(ModelComponent.getComponentType(), new ModelComponent(hybridModel));
                        LOGGER.atFine().log("[Corpse] Applied hybrid model with death pose animation (pre-spawn)");
                    } else {
                        LOGGER.atWarning().log("[Corpse] Could not create hybrid model - playerModel=%s, corpseModel=%s",
                                playerModelAsset != null, corpseModelAsset != null);
                    }

                    // Apply player skin in holder (pre-spawn) for immediate skin display
                    if (skin != null) {
                        PlayerSkinComponent corpseSkin = new PlayerSkinComponent(skin);
                        corpseSkin.setNetworkOutdated();
                        holder.putComponent(PlayerSkinComponent.getComponentType(), corpseSkin);
                    }
                },
                (npcComponent, corpseRef, callbackStore) -> {
                    if (npcComponent != null) {
                        npcComponent.setInventorySize(
                                Inventory.DEFAULT_HOTBAR_CAPACITY,
                                Inventory.DEFAULT_STORAGE_CAPACITY,
                                Inventory.DEFAULT_UTILITY_CAPACITY
                        );
                        npcComponent.setDespawning(false);
                        npcComponent.setDespawnCheckRemainingSeconds(Float.MAX_VALUE);

                        // Copy inventory from snapshot to corpse
                        if (inventorySnapshot != null) {
                            copyInventoryToCorpse(npcComponent.getInventory(), inventorySnapshot);
                        }

                        // Force the death animation immediately on spawn
                        npcComponent.playAnimation(corpseRef, AnimationSlot.Status, "Dead", callbackStore);
                    }

                    // Remove EntityStatMap to hide the health bar (corpse is invulnerable anyway)
                    callbackStore.removeComponent(corpseRef, EntityStatsModule.get().getEntityStatMapComponentType());

                    LOGGER.atInfo().log(
                            "[Corpse] Spawned corpse for %s at (%.2f, %.2f, %.2f) [%s]",
                            playerName,
                            spawnPosition.getX(),
                            spawnPosition.getY(),
                            spawnPosition.getZ(),
                            spawnReason
                    );
                }
        );

        return result != null ? result.first() : null;
    }

    /**
     * Copies items from the inventory snapshot to the corpse's inventory.
     */
    private static void copyInventoryToCorpse(Inventory corpseInventory, InventorySnapshot snapshot) {
        // Copy armor
        copyContainerItems(snapshot.armor(), corpseInventory.getArmor());

        // Copy storage
        copyContainerItems(snapshot.storage(), corpseInventory.getStorage());

        // Copy hotbar
        copyContainerItems(snapshot.hotbar(), corpseInventory.getHotbar());

        // Copy utility/offhand
        copyContainerItems(snapshot.utility(), corpseInventory.getUtility());

        LOGGER.atInfo().log("[Corpse] Copied inventory: %d armor, %d storage, %d hotbar, %d utility items",
                countItems(snapshot.armor()),
                countItems(snapshot.storage()),
                countItems(snapshot.hotbar()),
                countItems(snapshot.utility()));
    }

    private static void copyContainerItems(ItemStack[] sourceItems, ItemContainer targetContainer) {
        if (sourceItems == null) {
            return;
        }
        short capacity = targetContainer.getCapacity();
        for (short slot = 0; slot < sourceItems.length && slot < capacity; slot++) {
            ItemStack item = sourceItems[slot];
            if (!ItemStack.isEmpty(item)) {
                targetContainer.setItemStackForSlot(slot, item);
            }
        }
    }

    private static int countItems(ItemStack[] items) {
        if (items == null) {
            return 0;
        }
        int count = 0;
        for (ItemStack item : items) {
            if (!ItemStack.isEmpty(item)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Snapshot of a player's inventory at time of death.
     * All ItemStacks are cloned to avoid reference issues.
     */
    public record InventorySnapshot(
            ItemStack[] armor,
            ItemStack[] storage,
            ItemStack[] hotbar,
            ItemStack[] utility
    ) {
        /**
         * Creates a snapshot from a player's inventory, cloning all items.
         */
        public static InventorySnapshot fromInventory(Inventory inventory) {
            return new InventorySnapshot(
                    cloneContainer(inventory.getArmor()),
                    cloneContainer(inventory.getStorage()),
                    cloneContainer(inventory.getHotbar()),
                    cloneContainer(inventory.getUtility())
            );
        }

        private static ItemStack[] cloneContainer(ItemContainer container) {
            short capacity = container.getCapacity();
            ItemStack[] items = new ItemStack[capacity];
            for (short slot = 0; slot < capacity; slot++) {
                ItemStack itemStack = container.getItemStack(slot);
                if (!ItemStack.isEmpty(itemStack)) {
                    // Create a copy using constructor with all fields
                    items[slot] = new ItemStack(
                            itemStack.getItemId(),
                            itemStack.getQuantity(),
                            itemStack.getDurability(),
                            itemStack.getMaxDurability(),
                            itemStack.getMetadata()
                    );
                }
            }
            return items;
        }
    }
}
