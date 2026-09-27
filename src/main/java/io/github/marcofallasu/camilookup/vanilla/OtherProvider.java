package io.github.marcofallasu.camilookup.vanilla;

import io.github.marcofallasu.camilookup.api.info.InfoBuilder;
import io.github.marcofallasu.camilookup.api.target.BlockAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Sculk sensors and suspicious sand or gravel. */
final class OtherProvider {
    private OtherProvider() {
    }

    /** Client: sculk sensor phase and power. */
    static void appendClient(BlockAccessor accessor, InfoBuilder builder) {
        BlockState state = accessor.state();
        if (state.getBlock() instanceof SculkSensorBlock) {
            builder.text(Format.labeled("camilookup.advanced.state", Component.translatable(
                    "camilookup.sculk." + state.getValue(SculkSensorBlock.PHASE).getSerializedName())));
            builder.text(Format.labeled("camilookup.redstone.power", String.valueOf(state.getValue(SculkSensorBlock.POWER))));
        }
    }

    /** Server: the last vibration a sculk sensor heard. */
    static void appendSculk(BlockAccessor accessor, InfoBuilder builder) {
        if (accessor.blockEntity() instanceof SculkSensorBlockEntity sensor && sensor.getLastVibrationFrequency() > 0) {
            builder.text(Format.labeled("camilookup.other.vibration", String.valueOf(sensor.getLastVibrationFrequency())));
        }
    }

    /** Server: the item hidden in suspicious sand or gravel. Its category is disabled by default. */
    static void appendSuspicious(BlockAccessor accessor, InfoBuilder builder) {
        if (!(accessor.blockEntity() instanceof BrushableBlockEntity brushable)) {
            return;
        }
        ItemStack item = brushable.getItem();
        if (!item.isEmpty()) {
            builder.iconText(item, Component.translatable("camilookup.other.hidden_item", item.getHoverName()));
        } else if (brushable.saveCustomOnly(accessor.level().registryAccess()).contains("LootTable")) {
            // The item is only rolled when someone starts brushing.
            builder.text(Component.translatable("camilookup.container.unlooted").withStyle(ChatFormatting.GRAY));
        }
    }
}
