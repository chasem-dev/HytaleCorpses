package dev.chasem.corpses.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import dev.chasem.corpses.component.PlayerCorpseComponent;
import dev.chasem.corpses.npc.builders.BuilderActionOpenCorpseLoot;
import dev.chasem.corpses.ui.CorpseLootPage;

import javax.annotation.Nonnull;

/**
 * NPC Action that opens the corpse loot UI when a player interacts with a corpse.
 * This action is used by the HC_Player_Corpse NPC role.
 */
public class ActionOpenCorpseLoot extends ActionBase {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public ActionOpenCorpseLoot(@Nonnull BuilderActionOpenCorpseLoot builder, @Nonnull BuilderSupport support) {
        super(builder);
    }

    @Override
    public boolean canExecute(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Role role,
            InfoProvider sensorInfo,
            double dt,
            @Nonnull Store<EntityStore> store
    ) {
        boolean baseCanExecute = super.canExecute(ref, role, sensorInfo, dt, store);
        Ref<EntityStore> target = role.getStateSupport().getInteractionIterationTarget();
        LOGGER.atFine().log("[CorpseLoot] canExecute: base=%s, target=%s", baseCanExecute, target);
        return baseCanExecute && target != null;
    }

    @Override
    public boolean execute(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Role role,
            InfoProvider sensorInfo,
            double dt,
            @Nonnull Store<EntityStore> store
    ) {
        super.execute(ref, role, sensorInfo, dt, store);

        // Get the player who is interacting
        Ref<EntityStore> playerReference = role.getStateSupport().getInteractionIterationTarget();
        if (playerReference == null) {
            LOGGER.atWarning().log("[CorpseLoot] No interaction target found");
            return false;
        }

        PlayerRef playerRefComponent = store.getComponent(playerReference, PlayerRef.getComponentType());
        Player playerComponent = store.getComponent(playerReference, Player.getComponentType());
        if (playerRefComponent == null || playerComponent == null) {
            LOGGER.atWarning().log("[CorpseLoot] Missing player components");
            return false;
        }

        // Get the corpse component directly from this NPC (no proxy indirection)
        PlayerCorpseComponent corpseComponent = store.getComponent(ref, PlayerCorpseComponent.getComponentType());
        if (corpseComponent == null) {
            LOGGER.atWarning().log("[CorpseLoot] NPC does not have PlayerCorpseComponent");
            return false;
        }

        // Get the NPC entity (this corpse) for inventory access
        NPCEntity corpseNpcEntity = store.getComponent(ref, NPCEntity.getComponentType());
        if (corpseNpcEntity == null) {
            LOGGER.atWarning().log("[CorpseLoot] Corpse missing NPCEntity component");
            return false;
        }

        Inventory corpseInventory = corpseNpcEntity.getInventory();

        // Get corpse's skin for preview
        PlayerSkinComponent corpseSkinComp = store.getComponent(ref, PlayerSkinComponent.getComponentType());
        PlayerSkin corpseSkin = corpseSkinComp != null ? corpseSkinComp.getPlayerSkin() : null;

        // Get corpse position for preview entity spawning
        TransformComponent corpseTransform = store.getComponent(ref, TransformComponent.getComponentType());
        Vector3d corpsePosition = corpseTransform != null
                ? corpseTransform.getPosition()
                : new Vector3d(0, 0, 0);

        // Get player's inventory for bidirectional transfer
        Inventory playerInventory = playerComponent.getInventory();

        LOGGER.atInfo().log("[CorpseLoot] %s opening loot page for corpse of %s",
                playerRefComponent.getUsername(), corpseComponent.getPlayerName());

        // Create and open the loot page
        CorpseLootPage lootPage = new CorpseLootPage(
                playerRefComponent,
                corpseComponent.getPlayerName(),
                corpseInventory.getArmor(),
                corpseInventory.getStorage(),
                corpseInventory.getHotbar(),
                corpseInventory.getUtility(),
                corpseSkin,
                corpsePosition,
                playerInventory
        );

        playerComponent.getPageManager().openCustomPage(ref, store, lootPage);

        // Mark corpse as looted if first time
        if (!corpseComponent.isLooted()) {
            store.putComponent(ref, PlayerCorpseComponent.getComponentType(), corpseComponent.withLooted(true));
            LOGGER.atInfo().log("[CorpseLoot] Marked corpse of %s as looted", corpseComponent.getPlayerName());
        }

        return true;
    }
}
