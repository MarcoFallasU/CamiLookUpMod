package io.github.marcofallasu.camilookup.client;

import io.github.marcofallasu.camilookup.api.target.BlockAccessor;
import io.github.marcofallasu.camilookup.api.target.EntityAccessor;
import io.github.marcofallasu.camilookup.api.target.LookUpAccessor;
import io.github.marcofallasu.camilookup.core.Targets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Finds the block or entity under the free cursor. */
public final class CursorPicker {
    private static final double ITEM_PICK_MARGIN = 0.15;

    private CursorPicker() {
    }

    public static @Nullable LookUpAccessor pick(CameraProjection projection, double windowX, double windowY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return null;
        }
        double reach = ClientState.maxDistance();
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 start = projection.cameraPos();
        Vec3 direction = projection.directionAt(windowX, windowY);
        // The camera can be behind the player in third person; reach is measured from the eyes.
        double length = reach + start.distanceTo(eye);
        Vec3 end = start.add(direction.scale(length));

        BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        double blockDistance = blockHit.getType() == HitResult.Type.MISS ? length : start.distanceTo(blockHit.getLocation());

        Entity entityHit = pickEntity(level, player, minecraft.getCameraEntity(), start, direction, blockDistance);

        LookUpAccessor target = null;
        if (entityHit != null) {
            target = new EntityAccessor(level, player, entityHit, false);
        } else if (blockHit.getType() == HitResult.Type.BLOCK && !level.getBlockState(blockHit.getBlockPos()).isAir()) {
            target = BlockAccessor.of(level, player, blockHit.getBlockPos(), false);
        }
        if (target != null && Targets.distance(eye, target) > reach) {
            return null;
        }
        return target;
    }

    /** The nearest entity crossed by the ray before {@code maxDistance}. */
    private static @Nullable Entity pickEntity(ClientLevel level, LocalPlayer player, @Nullable Entity cameraEntity,
                                               Vec3 start, Vec3 direction, double maxDistance) {
        Vec3 end = start.add(direction.scale(maxDistance));
        Entity nearest = null;
        double nearestDistance = maxDistance * maxDistance;
        for (Entity entity : level.getEntities(player, new AABB(start, end).inflate(1.0))) {
            if (entity == cameraEntity || entity.isSpectator() || entity.isInvisibleTo(player)
                    || !(entity.isPickable() || entity instanceof ItemEntity)) {
                continue;
            }
            // Dropped items are tiny; a slightly larger box makes them practical to point at.
            double inflate = entity.getPickRadius() + (entity instanceof ItemEntity ? ITEM_PICK_MARGIN : 0.0);
            AABB box = entity.getBoundingBox().inflate(inflate);
            if (box.contains(start)) {
                return entity;
            }
            var hit = box.clip(start, end);
            if (hit.isPresent()) {
                double distance = start.distanceToSqr(hit.get());
                if (distance < nearestDistance) {
                    nearest = entity;
                    nearestDistance = distance;
                }
            }
        }
        return nearest;
    }
}
