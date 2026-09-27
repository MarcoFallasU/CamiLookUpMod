package io.github.marcofallasu.camilookup.vanilla;

import io.github.marcofallasu.camilookup.api.info.InfoBuilder;
import io.github.marcofallasu.camilookup.api.provider.IBlockInfoProvider;
import io.github.marcofallasu.camilookup.api.target.BlockAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import java.util.List;

/** Server: input, fuel, output and progress of furnaces, blast furnaces and smokers. */
final class FurnaceProvider implements IBlockInfoProvider {
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_FUEL = 1;
    private static final int SLOT_RESULT = 2;

    @Override
    public void appendInfo(BlockAccessor accessor, InfoBuilder builder) {
        if (!(accessor.blockEntity() instanceof AbstractFurnaceBlockEntity furnace)) {
            return;
        }
        // The timers are not exposed; read them from the data the furnace saves.
        CompoundTag data = furnace.saveCustomOnly(accessor.level().registryAccess());
        int cookingTime = data.getIntOr("cooking_time_spent", 0);
        int cookingTotal = data.getIntOr("cooking_total_time", 0);
        int litTime = data.getIntOr("lit_time_remaining", 0);
        int litTotal = data.getIntOr("lit_total_time", 0);

        float progress = cookingTotal > 0 ? (float) cookingTime / cookingTotal : 0.0F;
        float fuel = litTotal > 0 ? (float) litTime / litTotal : 0.0F;
        builder.process(List.of(furnace.getItem(SLOT_INPUT)), furnace.getItem(SLOT_FUEL),
                List.of(furnace.getItem(SLOT_RESULT)), progress, fuel);

        if (cookingTime > 0 && cookingTotal > 0) {
            builder.detail(Format.labeled("camilookup.furnace.progress", Component.translatable(
                    "camilookup.furnace.progress_value", Math.round(progress * 100), Format.duration(cookingTotal - cookingTime))));
        }
        if (litTime > 0) {
            builder.detail(Format.labeled("camilookup.furnace.fuel", Format.duration(litTime)));
        }
    }
}
