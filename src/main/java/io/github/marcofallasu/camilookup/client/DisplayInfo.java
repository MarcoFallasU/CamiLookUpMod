package io.github.marcofallasu.camilookup.client;

import io.github.marcofallasu.camilookup.api.info.InfoElement;
import io.github.marcofallasu.camilookup.api.info.InfoSection;
import io.github.marcofallasu.camilookup.api.target.BlockAccessor;
import io.github.marcofallasu.camilookup.api.target.EntityAccessor;
import io.github.marcofallasu.camilookup.api.target.LookUpAccessor;
import io.github.marcofallasu.camilookup.core.InfoCollector;
import io.github.marcofallasu.camilookup.core.ModNames;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Everything shown in one box: client information merged with the latest server answer. */
public record DisplayInfo(ItemStack icon, Component title, Component modName, List<InfoElement> elements,
                          @Nullable Component notice) {

    public static DisplayInfo build(LookUpAccessor accessor) {
        InfoCollector collector = InfoCollector.collect(accessor, ClientState::isCategoryAllowed);
        List<InfoSection> sections = new ArrayList<>(collector.sections());

        Component notice = null;
        ServerInfoCache.Entry server = ServerInfoCache.get(accessor.ref());
        if (server != null) {
            sections.addAll(server.sections());
            notice = switch (server.status()) {
                case OK -> null;
                case DENIED -> Component.translatable("camilookup.notice.denied");
                case OUT_OF_RANGE -> Component.translatable("camilookup.notice.out_of_range");
                case GONE -> Component.translatable("camilookup.notice.unavailable");
            };
        }
        sections.sort(Comparator.comparingInt(InfoSection::priority));

        List<InfoElement> elements = new ArrayList<>();
        for (InfoSection section : sections) {
            elements.addAll(section.elements());
        }

        Component title = collector.title() != null ? collector.title() : Component.literal("?");
        Component modName = Component.literal(ModNames.of(namespace(accessor)))
                .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC);
        if (notice != null) {
            notice = notice.copy().withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
        }
        return new DisplayInfo(collector.icon(), title, modName, List.copyOf(elements), notice);
    }

    /** The same information marked as no longer up to date. */
    public DisplayInfo unavailable() {
        return new DisplayInfo(icon, title, modName, elements, Component.translatable("camilookup.notice.unavailable")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    private static String namespace(LookUpAccessor accessor) {
        return switch (accessor) {
            case BlockAccessor block -> BuiltInRegistries.BLOCK.getKey(block.state().getBlock()).getNamespace();
            case EntityAccessor entity -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.entity().getType()).getNamespace();
        };
    }
}
