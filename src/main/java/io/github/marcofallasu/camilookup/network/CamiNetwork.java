package io.github.marcofallasu.camilookup.network;

import io.github.marcofallasu.camilookup.CamiLookUp;
import io.github.marcofallasu.camilookup.client.ClientPacketHandler;
import io.github.marcofallasu.camilookup.server.ServerInfoHandler;
import net.minecraft.network.Connection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class CamiNetwork {
    public static final int PROTOCOL_VERSION = 1;

    // The channel is optional on both sides so either side can run without the other having the mod.
    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(Identifier.fromNamespaceAndPath(CamiLookUp.MODID, "main"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .optional()
            .simpleChannel()
            .play()
                .clientbound()
                    .addMain(ServerSettingsPacket.class, ServerSettingsPacket.STREAM_CODEC,
                            (packet, context) -> ClientPacketHandler.onServerSettings(packet))
                    .addMain(InfoResponsePacket.class, InfoResponsePacket.STREAM_CODEC,
                            (packet, context) -> ClientPacketHandler.onInfoResponse(packet))
                .serverbound()
                    .addMain(InfoRequestPacket.class, InfoRequestPacket.STREAM_CODEC,
                            (packet, context) -> ServerInfoHandler.onRequest(packet, context.getSender()))
            .build();

    private CamiNetwork() {
    }

    public static void init() {
        // Loads the class so the channel is registered during mod construction.
    }

    public static boolean isPresent(Connection connection) {
        return CHANNEL.isRemotePresent(connection);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
    }

    public static void sendToServer(Object packet) {
        CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
    }
}
