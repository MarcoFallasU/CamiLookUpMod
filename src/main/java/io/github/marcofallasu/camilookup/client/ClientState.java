package io.github.marcofallasu.camilookup.client;

import io.github.marcofallasu.camilookup.config.ClientConfig;
import io.github.marcofallasu.camilookup.network.ServerSettingsPacket;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/** What the client knows about the server it is connected to. */
public final class ClientState {
    private static @Nullable ServerSettingsPacket settings;

    private ClientState() {
    }

    static void setSettings(@Nullable ServerSettingsPacket newSettings) {
        settings = newSettings;
    }

    /** Whether the server has Cami LookUp; without it the mod works with client-side information only. */
    public static boolean serverHasMod() {
        return settings != null;
    }

    public static double maxDistance() {
        double distance = ClientConfig.inspectDistance();
        return settings == null ? distance : Math.min(distance, settings.maxDistance());
    }

    public static boolean isCategoryAllowed(Identifier category) {
        return settings == null || !settings.disabledCategories().contains(category);
    }
}
