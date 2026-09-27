package io.github.marcofallasu.camilookup.vanilla;

import io.github.marcofallasu.camilookup.api.info.InfoBuilder;
import io.github.marcofallasu.camilookup.api.target.BlockAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ComparatorMode;

/** Repeaters, comparators, wire, observers, pistons, daylight detectors, hoppers and signal strength. */
final class RedstoneProvider {
    private RedstoneProvider() {
    }

    /** Client: everything the block state tells. */
    static void appendClient(BlockAccessor accessor, InfoBuilder builder) {
        BlockState state = accessor.state();
        Block block = state.getBlock();
        Level level = accessor.level();
        BlockPos pos = accessor.pos();

        switch (block) {
            case RepeaterBlock repeater -> {
                int delay = state.getValue(RepeaterBlock.DELAY);
                builder.text(Format.labeled("camilookup.redstone.delay",
                        Component.translatable("camilookup.redstone.delay_value", delay, delay * 2)));
                if (state.getValue(RepeaterBlock.LOCKED)) {
                    builder.text(Component.translatable("camilookup.redstone.locked").withStyle(ChatFormatting.RED));
                }
            }
            case ComparatorBlock comparator -> builder.text(Format.labeled("camilookup.redstone.mode",
                    Component.translatable(state.getValue(ComparatorBlock.MODE) == ComparatorMode.SUBTRACT
                            ? "camilookup.redstone.mode.subtract" : "camilookup.redstone.mode.compare")));
            case RedStoneWireBlock wire -> builder.text(Format.labeled("camilookup.redstone.power",
                    String.valueOf(state.getValue(RedStoneWireBlock.POWER))));
            case ObserverBlock observer -> {
                builder.text(Format.labeled("camilookup.redstone.observes", Format.direction(state.getValue(ObserverBlock.FACING))));
                if (state.getValue(ObserverBlock.POWERED)) {
                    builder.text(Component.translatable("camilookup.redstone.pulse").withStyle(ChatFormatting.RED));
                }
            }
            case PistonBaseBlock piston -> builder.text(Format.labeled("camilookup.redstone.extended",
                    Format.yesNo(state.getValue(PistonBaseBlock.EXTENDED))));
            case DaylightDetectorBlock detector -> {
                builder.text(Format.labeled("camilookup.redstone.power", String.valueOf(state.getValue(DaylightDetectorBlock.POWER))));
                builder.text(Format.labeled("camilookup.redstone.detects", Component.translatable(
                        state.getValue(DaylightDetectorBlock.INVERTED) ? "camilookup.redstone.detects.night" : "camilookup.redstone.detects.day")));
            }
            case HopperBlock hopper -> {
                boolean enabled = state.getValue(HopperBlock.ENABLED);
                builder.text(Format.labeled("camilookup.redstone.hopper_state", Component.translatable(
                        enabled ? "camilookup.redstone.hopper_state.active" : "camilookup.redstone.hopper_state.locked")
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED)));
            }
            default -> {
            }
        }

        if (!(block instanceof RedStoneWireBlock)) {
            int received = level.getBestNeighborSignal(pos);
            if (received > 0) {
                builder.text(Format.labeled("camilookup.redstone.received", String.valueOf(received)));
            }
        }
        // The client does not know a comparator's output; the server provider reports it.
        if (state.isSignalSource() && !(block instanceof RedStoneWireBlock) && !(block instanceof ComparatorBlock)) {
            int emitted = 0;
            for (Direction direction : Direction.values()) {
                emitted = Math.max(emitted, state.getSignal(level, pos, direction));
            }
            if (emitted > 0) {
                builder.text(Format.labeled("camilookup.redstone.emitted", String.valueOf(emitted)));
            }
        }
    }

    /** Server: a comparator's output signal. */
    static void appendServer(BlockAccessor accessor, InfoBuilder builder) {
        if (accessor.blockEntity() instanceof ComparatorBlockEntity comparator) {
            builder.text(Format.labeled("camilookup.redstone.output", String.valueOf(comparator.getOutputSignal())));
        }
    }
}
