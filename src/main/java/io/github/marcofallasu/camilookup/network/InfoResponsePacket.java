package io.github.marcofallasu.camilookup.network;

import io.github.marcofallasu.camilookup.api.info.InfoSection;
import io.github.marcofallasu.camilookup.api.target.TargetRef;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/** The server's answer to an {@link InfoRequestPacket}. */
public record InfoResponsePacket(TargetRef target, Status status, List<InfoSection> sections) {
    public static final StreamCodec<RegistryFriendlyByteBuf, InfoResponsePacket> STREAM_CODEC = StreamCodec.composite(
            TargetRef.STREAM_CODEC.cast(), InfoResponsePacket::target,
            ByteBufCodecs.idMapper(i -> Status.values()[i], Status::ordinal), InfoResponsePacket::status,
            InfoSection.STREAM_CODEC.apply(ByteBufCodecs.list(256)), InfoResponsePacket::sections,
            InfoResponsePacket::new);

    public enum Status {
        OK,
        /** The server refused to share information (restriction rule, line of sight...). */
        DENIED,
        /** The target is farther than the server allows. */
        OUT_OF_RANGE,
        /** The target no longer exists or is not loaded. */
        GONE
    }
}
