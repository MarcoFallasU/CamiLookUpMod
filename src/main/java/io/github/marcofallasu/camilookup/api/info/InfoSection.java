package io.github.marcofallasu.camilookup.api.info;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;

/** The elements produced by one provider. Sections are merged and ordered by {@link #priority()}. */
public record InfoSection(Identifier providerId, int priority, List<InfoElement> elements) {
    public static final StreamCodec<RegistryFriendlyByteBuf, InfoSection> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, InfoSection::providerId,
            ByteBufCodecs.VAR_INT, InfoSection::priority,
            InfoElement.STREAM_CODEC.apply(ByteBufCodecs.list(1024)), InfoSection::elements,
            InfoSection::new);

    public InfoSection {
        elements = List.copyOf(elements);
    }
}
