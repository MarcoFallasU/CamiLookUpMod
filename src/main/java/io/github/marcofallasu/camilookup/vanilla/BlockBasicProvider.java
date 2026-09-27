package io.github.marcofallasu.camilookup.vanilla;

import io.github.marcofallasu.camilookup.api.info.InfoBuilder;
import io.github.marcofallasu.camilookup.api.provider.IBlockInfoProvider;
import io.github.marcofallasu.camilookup.api.target.BlockAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;

/** Client: name, icon and block state properties. */
final class BlockBasicProvider implements IBlockInfoProvider {
    @Override
    public void appendInfo(BlockAccessor accessor, InfoBuilder builder) {
        BlockState state = accessor.state();
        builder.title(state.getBlock().getName());

        ItemStack icon = ItemStack.EMPTY;
        try {
            icon = state.getCloneItemStack(accessor.level(), accessor.pos(), false);
        } catch (RuntimeException ignored) {
            // Some blocks need data the client does not have; fall back to the block item.
        }
        if (icon.isEmpty()) {
            icon = new ItemStack(state.getBlock().asItem());
        }
        builder.icon(icon);

        for (Map.Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet()) {
            builder.detail(Component.literal(entry.getKey().getName() + ": ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(valueName(entry.getKey(), entry.getValue())).withStyle(ChatFormatting.WHITE)));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String valueName(Property<T> property, Comparable<?> value) {
        return property.getName((T) value);
    }
}
