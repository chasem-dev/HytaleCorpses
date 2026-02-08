package dev.chasem.corpses.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.MoveTransaction;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Custom UI page for looting items from a player corpse.
 * Displays dual-panel inventory view with bidirectional item transfer.
 */
public class CorpseLootPage extends InteractiveCustomUIPage<CorpseLootPage.LootEventData> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String PAGE_LAYOUT = "Pages/CorpseLootPage.ui";
    private static final String SLOT_TEMPLATE = "Pages/CorpseLootSlot.ui";

    // Corpse data
    private final String corpsePlayerName;
    private final ItemContainer corpseArmor;
    private final ItemContainer corpseStorage;
    private final ItemContainer corpseHotbar;
    private final ItemContainer corpseUtility;
    @Nullable private final PlayerSkin corpseSkin;
    private final Vector3d corpsePosition;

    // Player inventory reference for bidirectional transfer
    private final Inventory playerInventory;

    // Change event listeners for network sync
    private EventRegistration corpseArmorChangeListener;
    private EventRegistration corpseStorageChangeListener;
    private EventRegistration corpseHotbarChangeListener;
    private EventRegistration corpseUtilityChangeListener;
    private EventRegistration playerArmorChangeListener;
    private EventRegistration playerStorageChangeListener;
    private EventRegistration playerHotbarChangeListener;
    private EventRegistration playerUtilityChangeListener;

    public CorpseLootPage(
            @Nonnull PlayerRef playerRef,
            @Nonnull String corpsePlayerName,
            @Nonnull ItemContainer corpseArmor,
            @Nonnull ItemContainer corpseStorage,
            @Nonnull ItemContainer corpseHotbar,
            @Nonnull ItemContainer corpseUtility,
            @Nullable PlayerSkin corpseSkin,
            @Nonnull Vector3d corpsePosition,
            @Nonnull Inventory playerInventory
    ) {
        super(playerRef, CustomPageLifetime.CanDismiss, LootEventData.CODEC);
        this.corpsePlayerName = corpsePlayerName;
        this.corpseArmor = corpseArmor;
        this.corpseStorage = corpseStorage;
        this.corpseHotbar = corpseHotbar;
        this.corpseUtility = corpseUtility;
        this.corpseSkin = corpseSkin;
        this.corpsePosition = corpsePosition;
        this.playerInventory = playerInventory;

        // Register change listeners for network sync
        registerChangeListeners();
    }

    /**
     * Register change event listeners on all inventory containers.
     * This ensures the UI rebuilds whenever ANY inventory changes,
     * enabling multi-player looting sync.
     */
    private void registerChangeListeners() {
        // Corpse inventory listeners
        this.corpseArmorChangeListener = corpseArmor.registerChangeEvent(EventPriority.LAST, e -> rebuild());
        this.corpseStorageChangeListener = corpseStorage.registerChangeEvent(EventPriority.LAST, e -> rebuild());
        this.corpseHotbarChangeListener = corpseHotbar.registerChangeEvent(EventPriority.LAST, e -> rebuild());
        this.corpseUtilityChangeListener = corpseUtility.registerChangeEvent(EventPriority.LAST, e -> rebuild());

        // Player inventory listeners (for bidirectional sync)
        this.playerArmorChangeListener = playerInventory.getArmor().registerChangeEvent(EventPriority.LAST, e -> rebuild());
        this.playerStorageChangeListener = playerInventory.getStorage().registerChangeEvent(EventPriority.LAST, e -> rebuild());
        this.playerHotbarChangeListener = playerInventory.getHotbar().registerChangeEvent(EventPriority.LAST, e -> rebuild());
        this.playerUtilityChangeListener = playerInventory.getUtility().registerChangeEvent(EventPriority.LAST, e -> rebuild());
    }

    /**
     * Unregister all change event listeners.
     */
    private void unregisterChangeListeners() {
        if (corpseArmorChangeListener != null) corpseArmorChangeListener.unregister();
        if (corpseStorageChangeListener != null) corpseStorageChangeListener.unregister();
        if (corpseHotbarChangeListener != null) corpseHotbarChangeListener.unregister();
        if (corpseUtilityChangeListener != null) corpseUtilityChangeListener.unregister();
        if (playerArmorChangeListener != null) playerArmorChangeListener.unregister();
        if (playerStorageChangeListener != null) playerStorageChangeListener.unregister();
        if (playerHotbarChangeListener != null) playerHotbarChangeListener.unregister();
        if (playerUtilityChangeListener != null) playerUtilityChangeListener.unregister();
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store
    ) {
        commandBuilder.append(PAGE_LAYOUT);

        // Set dynamic title on corpse panel with player name
        commandBuilder.set("#CorpsePanel #TitleLabel.Text", corpsePlayerName + "'s Corpse");

        // Build player panel (left side) - shows player's current inventory
        buildGridSection(commandBuilder, eventBuilder, "#PlayerArmorGrid", playerInventory.getArmor(), "player", "armor", (short) 4);
        buildGridSection(commandBuilder, eventBuilder, "#PlayerUtilityGrid", playerInventory.getUtility(), "player", "utility", (short) 4);
        buildGridSection(commandBuilder, eventBuilder, "#PlayerStorageGrid", playerInventory.getStorage(), "player", "storage", (short) 36);
        buildGridSection(commandBuilder, eventBuilder, "#PlayerHotbarGrid", playerInventory.getHotbar(), "player", "hotbar", (short) 9);

        // Build corpse panel (right side) - shows corpse's inventory
        buildGridSection(commandBuilder, eventBuilder, "#CorpseArmorGrid", corpseArmor, "corpse", "armor", (short) 4);
        buildGridSection(commandBuilder, eventBuilder, "#CorpseUtilityGrid", corpseUtility, "corpse", "utility", (short) 4);
        buildGridSection(commandBuilder, eventBuilder, "#CorpseStorageGrid", corpseStorage, "corpse", "storage", (short) 36);
        buildGridSection(commandBuilder, eventBuilder, "#CorpseHotbarGrid", corpseHotbar, "corpse", "hotbar", (short) 9);
    }

    private void buildGridSection(
            UICommandBuilder commandBuilder,
            UIEventBuilder eventBuilder,
            String gridSelector,
            ItemContainer container,
            String sourcePanel,
            String section,
            short maxSlots
    ) {
        short capacity = (short) Math.min(container.getCapacity(), maxSlots);

        // Clear any existing slots
        commandBuilder.clear(gridSelector);

        // Append slot templates for each inventory slot
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack itemStack = container.getItemStack(slot);
            String slotSelector = gridSelector + "[" + slot + "]";

            // Append the slot template
            commandBuilder.append(gridSelector, SLOT_TEMPLATE);

            // Only set item data for non-empty slots (same pattern as BuyShopPage)
            if (!ItemStack.isEmpty(itemStack)) {
                // Set the item ID on the slot (simpler than full ItemGridSlot)
                commandBuilder.set(slotSelector + " #ItemSlot.ItemId", itemStack.getItem().getId());

                // Bind click event for this slot
                eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        slotSelector + " #SlotButton",
                        EventData.of("SourcePanel", sourcePanel)
                                .put("Section", section)
                                .put("Slot", String.valueOf(slot)),
                        false
                );
            }
            // Empty slots: leave ItemSlot unset, renders as empty
        }
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull LootEventData data
    ) {
        String sourcePanel = data.getSourcePanel();
        String section = data.getSection();
        short slot = data.getSlot();

        LOGGER.atFine().log("[CorpseLoot] handleDataEvent: sourcePanel=%s, section=%s, slot=%d", sourcePanel, section, slot);

        if (sourcePanel == null || sourcePanel.isEmpty()) {
            LOGGER.atWarning().log("[CorpseLoot] No sourcePanel provided in event");
            return;
        }

        if (section == null || section.isEmpty()) {
            LOGGER.atWarning().log("[CorpseLoot] No section provided in event");
            return;
        }

        if (slot < 0) {
            LOGGER.atWarning().log("[CorpseLoot] Invalid slot index: %d", slot);
            return;
        }

        // Determine source and destination based on sourcePanel
        ItemContainer sourceContainer;
        ItemContainer destContainer;

        if ("corpse".equals(sourcePanel)) {
            // Transfer corpse -> player
            sourceContainer = getCorpseContainerForSection(section);
            destContainer = playerInventory.getCombinedHotbarFirst();
        } else if ("player".equals(sourcePanel)) {
            // Transfer player -> corpse
            sourceContainer = getPlayerContainerForSection(section);
            destContainer = getCorpseCombined();
        } else {
            LOGGER.atWarning().log("[CorpseLoot] Unknown sourcePanel: %s", sourcePanel);
            return;
        }

        if (sourceContainer == null) {
            LOGGER.atWarning().log("[CorpseLoot] Unknown section: %s", section);
            return;
        }

        if (slot >= sourceContainer.getCapacity()) {
            LOGGER.atWarning().log("[CorpseLoot] Slot index %d out of range for section %s (capacity: %d)",
                    slot, section, sourceContainer.getCapacity());
            return;
        }

        ItemStack itemStack = sourceContainer.getItemStack(slot);
        if (ItemStack.isEmpty(itemStack)) {
            LOGGER.atFine().log("[CorpseLoot] Slot %d in section %s is empty", slot, section);
            return;
        }

        // Try to move item to destination inventory
        MoveTransaction<ItemStackTransaction> transaction = sourceContainer.moveItemStackFromSlot(
                slot,
                destContainer
        );

        ItemStack remainder = transaction.getAddTransaction().getRemainder();
        if (ItemStack.isEmpty(remainder)) {
            // Successfully transferred all items
            if ("corpse".equals(sourcePanel)) {
                LOGGER.atInfo().log("[CorpseLoot] %s looted %s from %s's corpse",
                        playerRef.getUsername(),
                        itemStack.getItem().getId(),
                        corpsePlayerName);
            } else {
                LOGGER.atInfo().log("[CorpseLoot] %s deposited %s into %s's corpse",
                        playerRef.getUsername(),
                        itemStack.getItem().getId(),
                        corpsePlayerName);
            }
        } else if (remainder.getQuantity() < itemStack.getQuantity()) {
            // Partial transfer
            if ("corpse".equals(sourcePanel)) {
                LOGGER.atInfo().log("[CorpseLoot] %s partially looted %s from %s's corpse",
                        playerRef.getUsername(),
                        itemStack.getItem().getId(),
                        corpsePlayerName);
            } else {
                LOGGER.atInfo().log("[CorpseLoot] %s partially deposited %s into %s's corpse",
                        playerRef.getUsername(),
                        itemStack.getItem().getId(),
                        corpsePlayerName);
            }
        } else {
            // Destination full - send message to player
            String message = "corpse".equals(sourcePanel)
                    ? "Your inventory is full!"
                    : "The corpse's inventory is full!";
            playerRef.sendMessage(Message.raw(message));
        }
    }

    private ItemContainer getCorpseContainerForSection(String section) {
        return switch (section) {
            case "armor" -> corpseArmor;
            case "storage" -> corpseStorage;
            case "hotbar" -> corpseHotbar;
            case "utility" -> corpseUtility;
            default -> null;
        };
    }

    private ItemContainer getPlayerContainerForSection(String section) {
        return switch (section) {
            case "armor" -> playerInventory.getArmor();
            case "storage" -> playerInventory.getStorage();
            case "hotbar" -> playerInventory.getHotbar();
            case "utility" -> playerInventory.getUtility();
            default -> null;
        };
    }

    /**
     * Get a combined container for the corpse inventory (hotbar first, then storage).
     */
    private ItemContainer getCorpseCombined() {
        return new CombinedItemContainer(corpseHotbar, corpseStorage);
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        LOGGER.atFine().log("[CorpseLoot] Player %s closed loot page for %s",
                playerRef.getUsername(), corpsePlayerName);

        // Unregister all change listeners
        unregisterChangeListeners();
    }

    /**
     * Event data structure for loot click events.
     */
    public static class LootEventData {
        public static final BuilderCodec<LootEventData> CODEC = BuilderCodec.builder(LootEventData.class, LootEventData::new)
                .append(new KeyedCodec<>("SourcePanel", Codec.STRING), (d, v) -> d.sourcePanel = v, d -> d.sourcePanel)
                .add()
                .append(new KeyedCodec<>("Section", Codec.STRING), (d, v) -> d.section = v, d -> d.section)
                .add()
                .append(new KeyedCodec<>("Slot", Codec.STRING), (d, v) -> {
                    try {
                        d.slot = Short.parseShort(v);
                    } catch (NumberFormatException e) {
                        d.slot = -1;
                    }
                }, d -> String.valueOf(d.slot))
                .add()
                .build();

        private String sourcePanel;
        private String section;
        private short slot = -1;

        public String getSourcePanel() {
            return sourcePanel;
        }

        public String getSection() {
            return section;
        }

        public short getSlot() {
            return slot;
        }
    }
}
