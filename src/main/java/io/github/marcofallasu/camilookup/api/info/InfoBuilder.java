package io.github.marcofallasu.camilookup.api.info;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Collects the content produced by a provider.
 * <p>
 * The title and icon are only used on the client; on the server only the added elements are sent.
 */
public interface InfoBuilder {
    InfoBuilder title(Component title);

    @Nullable
    Component title();

    InfoBuilder icon(ItemStack icon);

    ItemStack icon();

    InfoBuilder add(InfoElement element);

    default InfoBuilder text(Component text) {
        return add(new InfoElement.Text(text, Visibility.ALWAYS));
    }

    default InfoBuilder text(Component text, Visibility visibility) {
        return add(new InfoElement.Text(text, visibility));
    }

    /** A line shown only in the full detail. */
    default InfoBuilder detail(Component text) {
        return add(new InfoElement.Text(text, Visibility.DETAIL));
    }

    default InfoBuilder iconText(ItemStack icon, Component text) {
        return add(new InfoElement.IconText(icon, text, Visibility.ALWAYS));
    }

    default InfoBuilder items(List<ItemStack> slots, int columns) {
        return add(new InfoElement.ItemGrid(slots, columns, Visibility.ALWAYS));
    }

    default InfoBuilder itemRow(List<ItemStack> items) {
        return add(new InfoElement.ItemRow(items, Visibility.ALWAYS));
    }

    /** A furnace-like machine; see {@link InfoElement.Process}. */
    default InfoBuilder process(List<ItemStack> inputs, ItemStack fuel, List<ItemStack> outputs, float progress, float fuelLevel) {
        return add(new InfoElement.Process(inputs, fuel, outputs, progress, fuelLevel, Visibility.ALWAYS));
    }

    default InfoBuilder health(float health, float maxHealth) {
        return add(new InfoElement.Health(health, maxHealth, Visibility.ALWAYS));
    }
}
