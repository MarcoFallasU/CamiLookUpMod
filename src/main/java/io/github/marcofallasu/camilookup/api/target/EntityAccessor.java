package io.github.marcofallasu.camilookup.api.target;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record EntityAccessor(Level level, Player player, Entity entity, boolean isServerSide) implements LookUpAccessor {
    @Override
    public TargetRef ref() {
        return new TargetRef.EntityRef(entity.getId());
    }

    @Override
    public Vec3 anchor() {
        return entity.getBoundingBox().getCenter();
    }
}
