package dev.chasem.hg.corpses.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.chasem.hg.corpses.util.PlayerCorpseSpawner;

import javax.annotation.Nonnull;

/**
 * Testing command for corpse spawning and death behavior.
 *
 * Usage:
 * - /testdeath         -> spawn a corpse at your feet without killing you
 * - /testdeath --kill  -> kill the player and let the death system spawn the corpse
 */
public class TestDeathCommand extends AbstractPlayerCommand {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    private final FlagArg killFlag = withFlagArg("kill", "Kill the player after running the test");

    public TestDeathCommand() {
        super("testdeath", "Spawn a player corpse for testing");
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        boolean shouldKill = killFlag.get(context);
        if (shouldKill) {
            Damage.CommandSource damageSource = new Damage.CommandSource(context.sender(), getName());
            DeathComponent.tryAddComponent(store, ref, new Damage(damageSource, DamageCause.COMMAND, Float.MAX_VALUE));
            playerRef.sendMessage(Message.raw("Triggered death. A corpse should spawn at your death location."));
            LOGGER.atInfo().log("[TestDeath] Killed %s via /testdeath --kill", playerRef.getUsername());
            return;
        }

        // Get player's inventory to copy to the corpse
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerCorpseSpawner.InventorySnapshot inventorySnapshot = null;
        if (player != null && player.getInventory() != null) {
            inventorySnapshot = PlayerCorpseSpawner.InventorySnapshot.fromInventory(player.getInventory());
        }

        Ref<EntityStore> corpseRef = PlayerCorpseSpawner.spawnCorpseForPlayer(store, ref, "command:testdeath", inventorySnapshot);
        if (corpseRef == null) {
            playerRef.sendMessage(Message.raw("Failed to spawn corpse (check logs for details)."));
            return;
        }

        playerRef.sendMessage(Message.raw("Spawned a test corpse with your inventory. Interact with it using Use."));
    }
}
