package io.github.marcofallasu.camilookup.vanilla;

import io.github.marcofallasu.camilookup.api.info.InfoBuilder;
import io.github.marcofallasu.camilookup.api.target.BlockAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SnifferEggBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Crop growth (not sugar cane or cactus), saplings, beehives, composters and eggs. */
final class FarmProvider {
    private FarmProvider() {
    }

    /** Client: everything the block state tells. */
    static void appendClient(BlockAccessor accessor, InfoBuilder builder) {
        BlockState state = accessor.state();
        Block block = state.getBlock();
        switch (block) {
            case CropBlock crop -> growth(builder, crop.getAge(state), crop.getMaxAge());
            case StemBlock stem -> growth(builder, state.getValue(StemBlock.AGE), StemBlock.MAX_AGE);
            case AttachedStemBlock stem -> growth(builder, 1, 1);
            case NetherWartBlock wart -> growth(builder, state.getValue(NetherWartBlock.AGE), NetherWartBlock.MAX_AGE);
            case CocoaBlock cocoa -> growth(builder, state.getValue(CocoaBlock.AGE), CocoaBlock.MAX_AGE);
            case SweetBerryBushBlock bush -> growth(builder, state.getValue(SweetBerryBushBlock.AGE), SweetBerryBushBlock.MAX_AGE);
            case PitcherCropBlock pitcher -> growth(builder, state.getValue(PitcherCropBlock.AGE), PitcherCropBlock.MAX_AGE);
            case SaplingBlock sapling -> builder.text(Format.labeled("camilookup.farm.stage",
                    Format.fraction(state.getValue(SaplingBlock.STAGE) + 1, 2)));
            case BeehiveBlock hive -> builder.text(Format.labeled("camilookup.farm.honey",
                    Format.fraction(state.getValue(BeehiveBlock.HONEY_LEVEL), BeehiveBlock.MAX_HONEY_LEVELS)));
            case ComposterBlock composter -> {
                int level = state.getValue(ComposterBlock.LEVEL);
                if (level > ComposterBlock.MAX_LEVEL) {
                    builder.text(Component.translatable("camilookup.farm.ready").withStyle(ChatFormatting.GREEN));
                } else {
                    builder.text(Format.labeled("camilookup.farm.compost", Format.fraction(level, ComposterBlock.MAX_LEVEL)));
                }
            }
            case TurtleEggBlock egg -> {
                builder.text(Format.labeled("camilookup.farm.eggs", String.valueOf(state.getValue(TurtleEggBlock.EGGS))));
                hatch(builder, state.getValue(TurtleEggBlock.HATCH), TurtleEggBlock.MAX_HATCH_LEVEL);
            }
            case SnifferEggBlock egg -> hatch(builder, state.getValue(SnifferEggBlock.HATCH), SnifferEggBlock.MAX_HATCH_LEVEL);
            default -> {
            }
        }
    }

    /** Server: bees inside a beehive or nest. */
    static void appendServer(BlockAccessor accessor, InfoBuilder builder) {
        if (accessor.blockEntity() instanceof BeehiveBlockEntity hive) {
            builder.text(Format.labeled("camilookup.farm.bees", Format.fraction(hive.getOccupantCount(), BeehiveBlockEntity.MAX_OCCUPANTS)));
            if (hive.isSedated()) {
                builder.text(Component.translatable("camilookup.farm.sedated").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    private static void growth(InfoBuilder builder, int age, int maxAge) {
        if (age >= maxAge) {
            builder.text(Format.labeled("camilookup.farm.growth",
                    Component.translatable("camilookup.farm.mature").withStyle(ChatFormatting.GREEN)));
        } else {
            builder.text(Format.labeled("camilookup.farm.growth", Math.round(100.0F * age / maxAge) + "%"));
        }
    }

    private static void hatch(InfoBuilder builder, int hatch, int maxHatch) {
        builder.text(Format.labeled("camilookup.farm.hatch", Format.fraction(hatch, maxHatch)));
    }
}
