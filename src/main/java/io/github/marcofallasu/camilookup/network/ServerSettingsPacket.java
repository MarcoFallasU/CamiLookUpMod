package io.github.marcofallasu.camilookup.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Sent by the server on login: tells the client the server has Cami LookUp and which limits apply. */
public record ServerSettingsPacket(double maxDistance, List<Identifier> disabledCategories) {
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerSettingsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, ServerSettingsPacket::maxDistance,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(256)), ServerSettingsPacket::disabledCategories,
            ServerSettingsPacket::new);
}
