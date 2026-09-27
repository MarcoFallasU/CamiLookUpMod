package io.github.marcofallasu.camilookup.api.target;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record BlockAccessor(
        Level level,
        Player player,
        BlockPos pos,
        BlockState state,
        @Nullable BlockEntity blockEntity,
        boolean isServerSide
) implements LookUpAccessor {
    public static BlockAccessor of(Level level, Player player, BlockPos pos, boolean serverSide) {
        return new BlockAccessor(level, player, pos.immutable(), level.getBlockState(pos), level.getBlockEntity(pos), serverSide);
    }

    @Override
    public TargetRef ref() {
        return new TargetRef.BlockRef(pos);
    }

    @Override
    public Vec3 anchor() {
        return Vec3.atCenterOf(pos);
    }
}
