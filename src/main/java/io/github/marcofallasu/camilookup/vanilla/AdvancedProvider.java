package io.github.marcofallasu.camilookup.vanilla;

import io.github.marcofallasu.camilookup.api.info.InfoBuilder;
import io.github.marcofallasu.camilookup.api.target.BlockAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Crafters, trial spawners, vaults, decorated pots and respawn anchors. */
final class AdvancedProvider {
    private static final int CRAFTER_SLOTS = 9;

    private AdvancedProvider() {
    }

    /** Client: block states. */
    static void appendClient(BlockAccessor accessor, InfoBuilder builder) {
        BlockState state = accessor.state();
        Block block = state.getBlock();
        switch (block) {
            case RespawnAnchorBlock anchor -> builder.text(Format.labeled("camilookup.advanced.charges",
                    Format.fraction(state.getValue(RespawnAnchorBlock.CHARGE), RespawnAnchorBlock.MAX_CHARGES)));
            case TrialSpawnerBlock spawner -> {
                builder.text(Format.labeled("camilookup.advanced.state", Component.translatable(
                        "camilookup.trial_spawner." + state.getValue(TrialSpawnerBlock.STATE).getSerializedName())));
                ominous(builder, state.getValue(TrialSpawnerBlock.OMINOUS));
            }
            case VaultBlock vault -> {
                builder.text(Format.labeled("camilookup.advanced.state", Component.translatable(
                        "camilookup.vault." + state.getValue(VaultBlock.STATE).getSerializedName())));
                ominous(builder, state.getValue(VaultBlock.OMINOUS));
            }
            case CrafterBlock crafter -> {
                if (state.getValue(CrafterBlock.TRIGGERED)) {
                    builder.text(Component.translatable("camilookup.advanced.triggered").withStyle(ChatFormatting.RED));
                }
            }
            default -> {
            }
        }
    }

    /** Server: crafter grid, trial spawner progress, vault key and decorated pot contents. */
    static void appendServer(BlockAccessor accessor, InfoBuilder builder) {
        switch (accessor.blockEntity()) {
            case CrafterBlockEntity crafter -> {
                List<ItemStack> grid = new ArrayList<>(CRAFTER_SLOTS);
                int disabled = 0;
                for (int i = 0; i < CRAFTER_SLOTS; i++) {
                    grid.add(crafter.getItem(i));
                    disabled += crafter.isSlotDisabled(i) ? 1 : 0;
                }
                builder.items(grid, 3);
                if (disabled > 0) {
                    builder.text(Format.labeled("camilookup.advanced.disabled_slots", String.valueOf(disabled)));
                }
            }
            case TrialSpawnerBlockEntity spawner -> {
                TrialSpawnerStateData.Packed data = spawner.getTrialSpawner().getStateData().pack();
                builder.text(Format.labeled("camilookup.advanced.players", String.valueOf(data.detectedPlayers().size())));
                builder.text(Format.labeled("camilookup.advanced.mobs", String.valueOf(data.currentMobs().size())));
                if (spawner.getState() == TrialSpawnerState.COOLDOWN) {
                    long remaining = data.cooldownEndsAt() - accessor.level().getGameTime();
                    builder.text(Format.labeled("camilookup.advanced.cooldown", Format.duration(remaining)));
                }
            }
            case VaultBlockEntity vault -> {
                ItemStack key = vault.getConfig().keyItem();
                if (!key.isEmpty()) {
                    builder.iconText(key, Component.translatable("camilookup.advanced.key", key.getHoverName()));
                }
            }
            case DecoratedPotBlockEntity pot -> {
                // Reading a pot with a pending loot table would generate its loot.
                if (pot.getLootTable() != null) {
                    builder.text(Component.translatable("camilookup.container.unlooted").withStyle(ChatFormatting.GRAY));
                } else if (!pot.getTheItem().isEmpty()) {
                    ItemStack item = pot.getTheItem();
                    builder.iconText(item, item.getHoverName().copy().append(" ×" + item.getCount()));
                } else {
                    builder.text(Component.translatable("camilookup.container.empty").withStyle(ChatFormatting.GRAY));
                }
            }
            case null, default -> {
            }
        }
    }

    private static void ominous(InfoBuilder builder, boolean ominous) {
        if (ominous) {
            builder.text(Component.translatable("camilookup.advanced.ominous").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
