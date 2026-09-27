package io.github.marcofallasu.camilookup.core;

import io.github.marcofallasu.camilookup.api.target.BlockAccessor;
import io.github.marcofallasu.camilookup.api.target.EntityAccessor;
import io.github.marcofallasu.camilookup.api.target.LookUpAccessor;
import io.github.marcofallasu.camilookup.api.target.TargetRef;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Helpers shared by the client and the server to resolve and measure targets. */
public final class Targets {
    private Targets() {
    }

    /** Resolves a reference in a level, or returns {@code null} if the target does not exist or is not loaded. */
    public static @Nullable LookUpAccessor resolve(Level level, Player player, TargetRef ref, boolean serverSide) {
        return switch (ref) {
            case TargetRef.BlockRef block -> {
                BlockPos pos = block.pos();
                if (!level.isLoaded(pos) || level.getBlockState(pos).isAir()) {
                    yield null;
                }
                yield BlockAccessor.of(level, player, pos, serverSide);
            }
            case TargetRef.EntityRef entityRef -> {
                Entity entity = level.getEntity(entityRef.entityId());
                if (entity == null || entity.isRemoved()) {
                    yield null;
                }
                yield new EntityAccessor(level, player, entity, serverSide);
            }
        };
    }

    public static AABB bounds(LookUpAccessor accessor) {
        return switch (accessor) {
            case BlockAccessor block -> new AABB(block.pos());
            case EntityAccessor entity -> entity.entity().getBoundingBox();
        };
    }

    /** Distance from a point to the nearest point of the target's bounds. */
    public static double distance(Vec3 from, LookUpAccessor accessor) {
        AABB box = bounds(accessor);
        double x = Mth.clamp(from.x, box.minX, box.maxX);
        double y = Mth.clamp(from.y, box.minY, box.maxY);
        double z = Mth.clamp(from.z, box.minZ, box.maxZ);
        return from.distanceTo(new Vec3(x, y, z));
    }
}
