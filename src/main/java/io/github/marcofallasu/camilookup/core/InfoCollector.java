package io.github.marcofallasu.camilookup.core;

import com.mojang.logging.LogUtils;
import io.github.marcofallasu.camilookup.api.info.InfoBuilder;
import io.github.marcofallasu.camilookup.api.info.InfoElement;
import io.github.marcofallasu.camilookup.api.info.InfoSection;
import io.github.marcofallasu.camilookup.api.provider.ProviderSide;
import io.github.marcofallasu.camilookup.api.target.BlockAccessor;
import io.github.marcofallasu.camilookup.api.target.EntityAccessor;
import io.github.marcofallasu.camilookup.api.target.LookUpAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/** Runs providers for a target and gathers their output into sections. */
public final class InfoCollector implements InfoBuilder {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<Identifier> FAILED = ConcurrentHashMap.newKeySet();

    private @Nullable Component title;
    private ItemStack icon = ItemStack.EMPTY;
    private final List<InfoSection> sections = new ArrayList<>();
    private List<InfoElement> current = new ArrayList<>();

    public static InfoCollector collect(LookUpAccessor accessor, Predicate<Identifier> categoryAllowed) {
        InfoCollector collector = new InfoCollector();
        switch (accessor) {
            case BlockAccessor block -> {
                for (var entry : LookUpRegistry.get().blockProviders(sideOf(block))) {
                    if (categoryAllowed.test(entry.category())) {
                        collector.run(entry, () -> entry.provider().appendInfo(block, collector));
                    }
                }
            }
            case EntityAccessor entity -> {
                for (var entry : LookUpRegistry.get().entityProviders(sideOf(entity))) {
                    if (categoryAllowed.test(entry.category())) {
                        collector.run(entry, () -> entry.provider().appendInfo(entity, collector));
                    }
                }
            }
        }
        return collector;
    }

    private static ProviderSide sideOf(LookUpAccessor accessor) {
        return accessor.isServerSide()
                ? ProviderSide.SERVER
                : ProviderSide.CLIENT;
    }

    private void run(LookUpRegistry.Entry<?> entry, Runnable provider) {
        current = new ArrayList<>();
        try {
            provider.run();
        } catch (RuntimeException e) {
            if (FAILED.add(entry.id())) {
                LOGGER.error("Cami LookUp provider {} failed", entry.id(), e);
            }
        }
        if (!current.isEmpty()) {
            sections.add(new InfoSection(entry.id(), entry.priority(), current));
        }
    }

    public List<InfoSection> sections() {
        return sections;
    }

    @Override
    public InfoBuilder title(Component title) {
        this.title = title;
        return this;
    }

    @Override
    public @Nullable Component title() {
        return title;
    }

    @Override
    public InfoBuilder icon(ItemStack icon) {
        this.icon = icon.copy();
        return this;
    }

    @Override
    public ItemStack icon() {
        return icon;
    }

    @Override
    public InfoBuilder add(InfoElement element) {
        current.add(element);
        return this;
    }
}
