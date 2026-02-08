package dev.chasem.hg.corpses.npc.builders;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.InstructionType;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import dev.chasem.hg.corpses.npc.ActionOpenCorpseLoot;

import java.util.EnumSet;
import javax.annotation.Nonnull;

public class BuilderActionOpenCorpseLoot extends BuilderActionBase {

    @Nonnull
    @Override
    public String getShortDescription() {
        return "Open the corpse loot UI for the current player";
    }

    @Nonnull
    @Override
    public String getLongDescription() {
        return getShortDescription();
    }

    @Nonnull
    @Override
    public Action build(@Nonnull BuilderSupport builderSupport) {
        return new ActionOpenCorpseLoot(this, builderSupport);
    }

    @Nonnull
    @Override
    public BuilderDescriptorState getBuilderDescriptorState() {
        return BuilderDescriptorState.Stable;
    }

    @Nonnull
    public BuilderActionOpenCorpseLoot readConfig(@Nonnull JsonElement data) {
        this.requireInstructionType(EnumSet.of(InstructionType.Interaction));
        return this;
    }
}
