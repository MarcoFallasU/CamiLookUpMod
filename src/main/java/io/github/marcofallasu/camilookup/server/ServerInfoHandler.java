package io.github.marcofallasu.camilookup.server;

import io.github.marcofallasu.camilookup.api.IRestrictionRule;
import io.github.marcofallasu.camilookup.api.InfoCategories;
import io.github.marcofallasu.camilookup.api.info.InfoSection;
import io.github.marcofallasu.camilookup.api.target.EntityAccessor;
import io.github.marcofallasu.camilookup.api.target.LookUpAccessor;
import io.github.marcofallasu.camilookup.config.ServerConfig;
import io.github.marcofallasu.camilookup.core.InfoCollector;
import io.github.marcofallasu.camilookup.core.LookUpRegistry;
import io.github.marcofallasu.camilookup.core.Targets;
import io.github.marcofallasu.camilookup.network.CamiNetwork;
import io.github.marcofallasu.camilookup.network.InfoRequestPacket;
import io.github.marcofallasu.camilookup.network.InfoResponsePacket;
import io.github.marcofallasu.camilookup.network.ServerSettingsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerInfoHandler {
    /** Extra distance allowed on top of the configured maximum, to absorb movement between client and server. */
    private static final double DISTANCE_TOLERANCE = 1.5;

    private static final Map<UUID, RateLimiter> RATE_LIMITERS = new ConcurrentHashMap<>();

    private ServerInfoHandler() {
    }

    public static void init() {
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                sendSettings(player);
            }
        });
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event -> RATE_LIMITERS.remove(event.getEntity().getUUID()));
    }

    public static void sendSettings(ServerPlayer player) {
        if (CamiNetwork.isPresent(player.connection.getConnection())) {
            CamiNetwork.sendToPlayer(player, new ServerSettingsPacket(
                    ServerConfig.maxDistance(), disabledCategories()));
        }
    }

    private static List<Identifier> disabledCategories() {
        List<Identifier> disabled = new ArrayList<>(ServerConfig.disabledCategories());
        if (!ServerConfig.playerDetails()) {
            disabled.add(InfoCategories.PLAYER_DETAILS);
        }
        return disabled;
    }

    public static void onRequest(InfoRequestPacket packet, @Nullable ServerPlayer player) {
        if (player == null) {
            return;
        }
        long gameTime = player.level().getGameTime();
        RateLimiter limiter = RATE_LIMITERS.computeIfAbsent(player.getUUID(), id -> new RateLimiter());
        if (!limiter.tryAcquire(gameTime, ServerConfig.maxRequestsPerSecond())) {
            return;
        }

        LookUpAccessor accessor = Targets.resolve(player.level(), player, packet.target(), true);
        InfoResponsePacket.Status status = check(player, accessor);
        List<InfoSection> sections = List.of();
        if (status == InfoResponsePacket.Status.OK) {
            Set<Identifier> disabled = Set.copyOf(disabledCategories());
            List<IRestrictionRule> rules = LookUpRegistry.get().restrictionRules();
            LookUpAccessor target = accessor;
            sections = InfoCollector.collect(target, category -> !disabled.contains(category)
                    && rules.stream().allMatch(rule -> rule.canShowCategory(player, target, category))).sections();
        }
        CamiNetwork.sendToPlayer(player, new InfoResponsePacket(packet.target(), status, sections));
    }

    private static InfoResponsePacket.Status check(ServerPlayer player, @Nullable LookUpAccessor accessor) {
        if (accessor == null) {
            return InfoResponsePacket.Status.GONE;
        }
        if (accessor instanceof EntityAccessor entity && (entity.entity() == player || entity.entity().isInvisibleTo(player))) {
            return InfoResponsePacket.Status.DENIED;
        }
        if (Targets.distance(player.getEyePosition(), accessor) > ServerConfig.maxDistance() + DISTANCE_TOLERANCE) {
            return InfoResponsePacket.Status.OUT_OF_RANGE;
        }
        for (IRestrictionRule rule : LookUpRegistry.get().restrictionRules()) {
            if (!rule.canInspect(player, accessor)) {
                return InfoResponsePacket.Status.DENIED;
            }
        }
        return InfoResponsePacket.Status.OK;
    }

    /** Token bucket refilled every game tick. */
    private static final class RateLimiter {
        private double tokens = -1;
        private long lastTick;

        synchronized boolean tryAcquire(long gameTime, int perSecond) {
            if (tokens < 0) {
                tokens = perSecond;
            } else {
                tokens = Math.min(perSecond, tokens + Math.max(0, gameTime - lastTick) * perSecond / 20.0);
            }
            lastTick = gameTime;
            if (tokens < 1) {
                return false;
            }
            tokens--;
            return true;
        }
    }
}
