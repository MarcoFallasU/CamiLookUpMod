package io.github.marcofallasu.camilookup.client;

import io.github.marcofallasu.camilookup.api.info.InfoSection;
import io.github.marcofallasu.camilookup.api.target.TargetRef;
import io.github.marcofallasu.camilookup.network.CamiNetwork;
import io.github.marcofallasu.camilookup.network.InfoRequestPacket;
import io.github.marcofallasu.camilookup.network.InfoResponsePacket;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Requests server information for the targets being shown and keeps the latest answers. */
public final class ServerInfoCache {
    /** Latest status from the server, and the sections of the last successful answer. */
    public record Entry(InfoResponsePacket.Status status, List<InfoSection> sections) {
    }

    /** Entries not shown for this many ticks are forgotten. */
    private static final int EXPIRY_TICKS = 100;

    private static final Map<TargetRef, Entry> ENTRIES = new HashMap<>();
    private static final Map<TargetRef, Long> LAST_REQUESTED = new HashMap<>();
    private static final Map<TargetRef, Long> LAST_WANTED = new HashMap<>();
    private static long tick;

    private ServerInfoCache() {
    }

    /** Marks a target as shown, requesting fresh information every {@code intervalTicks}. */
    public static void want(TargetRef ref, int intervalTicks) {
        if (!ClientState.serverHasMod()) {
            return;
        }
        LAST_WANTED.put(ref, tick);
        Long last = LAST_REQUESTED.get(ref);
        if (last == null || tick - last >= intervalTicks) {
            LAST_REQUESTED.put(ref, tick);
            CamiNetwork.sendToServer(new InfoRequestPacket(ref));
        }
    }

    public static @Nullable Entry get(TargetRef ref) {
        return ENTRIES.get(ref);
    }

    static void onResponse(InfoResponsePacket packet) {
        if (!LAST_WANTED.containsKey(packet.target())) {
            return;
        }
        List<InfoSection> sections = packet.sections();
        if (packet.status() != InfoResponsePacket.Status.OK) {
            // Keep showing what was last known, marked with the status notice.
            Entry previous = ENTRIES.get(packet.target());
            sections = previous != null ? previous.sections() : List.of();
        }
        ENTRIES.put(packet.target(), new Entry(packet.status(), sections));
    }

    public static void tick() {
        tick++;
        LAST_WANTED.entrySet().removeIf(entry -> {
            if (tick - entry.getValue() > EXPIRY_TICKS) {
                ENTRIES.remove(entry.getKey());
                LAST_REQUESTED.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    public static void clear() {
        ENTRIES.clear();
        LAST_REQUESTED.clear();
        LAST_WANTED.clear();
    }
}
