package io.github.marcofallasu.camilookup.vanilla;

import io.github.marcofallasu.camilookup.api.info.InfoBuilder;
import io.github.marcofallasu.camilookup.api.target.BlockAccessor;
import io.github.marcofallasu.camilookup.api.target.EntityAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Server: contents of chests, barrels, shulker boxes, hoppers, container vehicles and chested mounts. */
final class ContainerProviders {
    private ContainerProviders() {
    }

    static void appendBlock(BlockAccessor accessor, InfoBuilder builder) {
        BlockEntity blockEntity = accessor.blockEntity();
        if (blockEntity == null) {
            return;
        }
        Level level = accessor.level();
        BlockState state = accessor.state();

        if (blockEntity instanceof ChestBlockEntity chest && state.getBlock() instanceof ChestBlock chestBlock) {
            if (hasLoot(chest) || hasLoot(otherChestHalf(level, accessor.pos(), state))) {
                unlooted(builder);
                return;
            }
            Container container = ChestBlock.getContainer(chestBlock, state, level, accessor.pos(), true);
            if (container != null) {
                builder.items(stacks(container), 9);
            }
            return;
        }

        if (blockEntity instanceof BarrelBlockEntity || blockEntity instanceof ShulkerBoxBlockEntity
                || blockEntity instanceof HopperBlockEntity || blockEntity instanceof DispenserBlockEntity) {
            if (hasLoot(blockEntity)) {
                unlooted(builder);
                return;
            }
            Container container = (Container) blockEntity;
            int columns = blockEntity instanceof DispenserBlockEntity ? 3 : columns(container.getContainerSize());
            builder.items(stacks(container), columns);
            return;
        }

        // Containers from other mods, through Forge's item handler capability.
        var key = ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(blockEntity.getType());
        if (key != null && !key.getNamespace().equals("minecraft")) {
            blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve()
                    .ifPresent(handler -> builder.items(stacks(handler), columns(handler.getSlots())));
        }
    }

    static void appendEntity(EntityAccessor accessor, InfoBuilder builder) {
        Entity entity = accessor.entity();
        if (entity instanceof ContainerEntity container) {
            if (container.getContainerLootTable() != null) {
                unlooted(builder);
                return;
            }
            List<ItemStack> stacks = new ArrayList<>(container.getItemStacks());
            builder.items(stacks, columns(stacks.size()));
        } else if (entity instanceof AbstractChestedHorse horse && horse.hasChest()) {
            horse.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve()
                    .ifPresent(handler -> builder.items(stacks(handler), Math.max(1, horse.getInventoryColumns())));
        }
    }

    private static BlockEntity otherChestHalf(Level level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(ChestBlock.TYPE) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return null;
        }
        return level.getBlockEntity(pos.relative(ChestBlock.getConnectedDirection(state)));
    }

    /** Reading a container with a pending loot table would generate its loot, so it must be checked first. */
    private static boolean hasLoot(BlockEntity blockEntity) {
        return blockEntity instanceof RandomizableContainerBlockEntity randomizable && randomizable.getLootTable() != null;
    }

    private static void unlooted(InfoBuilder builder) {
        builder.text(Component.translatable("camilookup.container.unlooted").withStyle(ChatFormatting.GRAY));
    }

    private static List<ItemStack> stacks(Container container) {
        List<ItemStack> stacks = new ArrayList<>(container.getContainerSize());
        for (int i = 0; i < container.getContainerSize(); i++) {
            stacks.add(container.getItem(i));
        }
        return stacks;
    }

    private static List<ItemStack> stacks(IItemHandler handler) {
        List<ItemStack> stacks = new ArrayList<>(handler.getSlots());
        for (int i = 0; i < handler.getSlots(); i++) {
            stacks.add(handler.getStackInSlot(i));
        }
        return stacks;
    }

    private static int columns(int size) {
        return size <= 5 ? Math.max(1, size) : 9;
    }
}
