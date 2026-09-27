package io.github.marcofallasu.camilookup.api.target;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Everything a provider, pin action or restriction rule knows about the inspected target.
 * <p>
 * On the client, {@link #level()} and {@link #player()} are the client level and local player; on the server they
 * are the server level and the player who made the request.
 */
public sealed interface LookUpAccessor permits BlockAccessor, EntityAccessor {
    Level level();

    Player player();

    TargetRef ref();

    /** Point of the target used for distance checks and for anchoring pinned boxes. */
    Vec3 anchor();

    boolean isServerSide();
}
