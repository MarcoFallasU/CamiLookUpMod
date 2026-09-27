package io.github.marcofallasu.camilookup.client;

import io.github.marcofallasu.camilookup.network.InfoResponsePacket;
import io.github.marcofallasu.camilookup.network.ServerSettingsPacket;

/** Handles packets from the server on the client thread. */
public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void onServerSettings(ServerSettingsPacket packet) {
        ClientState.setSettings(packet);
    }

    public static void onInfoResponse(InfoResponsePacket packet) {
        ServerInfoCache.onResponse(packet);
    }

    static void onDisconnect() {
        ClientState.setSettings(null);
        ServerInfoCache.clear();
    }
}
