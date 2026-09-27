package io.github.marcofallasu.camilookup.vanilla;

import io.github.marcofallasu.camilookup.api.info.InfoBuilder;
import io.github.marcofallasu.camilookup.api.target.BlockAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;

import java.util.List;

/** Server: brewing stands and campfires. Furnaces have their own provider. */
final class ProcessingProvider {
    private static final int BREWING_TIME = 400;
    private static final int MAX_BREWING_FUEL = 20;
    private static final int SLOT_INGREDIENT = 3;
    private static final int SLOT_FUEL = 4;

    private ProcessingProvider() {
    }

    static void appendServer(BlockAccessor accessor, InfoBuilder builder) {
        switch (accessor.blockEntity()) {
            case BrewingStandBlockEntity stand -> appendBrewingStand(accessor, stand, builder);
            case CampfireBlockEntity campfire -> appendCampfire(accessor, campfire, builder);
            case null, default -> {
            }
        }
    }

    private static void appendBrewingStand(BlockAccessor accessor, BrewingStandBlockEntity stand, InfoBuilder builder) {
        // Timers are not exposed; read them from the data the stand saves.
        CompoundTag data = stand.saveCustomOnly(accessor.level().registryAccess());
        int brewTime = data.getShortOr("BrewTime", (short) 0);
        int fuel = data.getByteOr("Fuel", (byte) 0);
        float progress = brewTime > 0 ? 1.0F - (float) brewTime / BREWING_TIME : 0.0F;
        builder.process(List.of(stand.getItem(SLOT_INGREDIENT)), stand.getItem(SLOT_FUEL),
                List.of(stand.getItem(0), stand.getItem(1), stand.getItem(2)), progress, (float) fuel / MAX_BREWING_FUEL);
        if (brewTime > 0) {
            builder.detail(Format.labeled("camilookup.processing.brewing", Component.translatable(
                    "camilookup.furnace.progress_value", Math.round(progress * 100), Format.duration(brewTime))));
        }
        builder.detail(Format.labeled("camilookup.processing.brewing_fuel", Format.fraction(fuel, MAX_BREWING_FUEL)));
    }

    private static void appendCampfire(BlockAccessor accessor, CampfireBlockEntity campfire, InfoBuilder builder) {
        CompoundTag data = campfire.saveCustomOnly(accessor.level().registryAccess());
        int[] progress = data.getIntArray("CookingTimes").orElse(new int[0]);
        int[] total = data.getIntArray("CookingTotalTimes").orElse(new int[0]);
        List<ItemStack> items = campfire.getItems();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            int done = i < progress.length ? progress[i] : 0;
            int needed = i < total.length ? total[i] : 0;
            Component status = needed > 0
                    ? Component.translatable("camilookup.furnace.progress_value", Math.round(100.0F * done / needed),
                    Format.duration(needed - done))
                    : Component.empty();
            builder.iconText(stack, stack.getHoverName().copy().append(" ").append(status));
        }
    }
}
