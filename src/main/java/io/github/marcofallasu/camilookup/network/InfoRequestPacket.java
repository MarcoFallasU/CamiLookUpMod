package io.github.marcofallasu.camilookup.network;

import io.github.marcofallasu.camilookup.api.target.TargetRef;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Asks the server for the information about a target. */
public record InfoRequestPacket(TargetRef target) {
    public static final StreamCodec<RegistryFriendlyByteBuf, InfoRequestPacket> STREAM_CODEC =
            TargetRef.STREAM_CODEC.<RegistryFriendlyByteBuf>cast().map(InfoRequestPacket::new, InfoRequestPacket::target);
}
