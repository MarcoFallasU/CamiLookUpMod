package io.github.marcofallasu.camilookup.vanilla;

import io.github.marcofallasu.camilookup.api.info.InfoBuilder;
import io.github.marcofallasu.camilookup.api.provider.IBlockInfoProvider;
import io.github.marcofallasu.camilookup.api.target.BlockAccessor;
import io.github.marcofallasu.camilookup.config.ClientConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/** Client: hardness, tool, blast resistance, light, biome and coordinates. Enabled in the client config. */
final class BlockTechnicalProvider implements IBlockInfoProvider {
    @Override
    public void appendInfo(BlockAccessor accessor, InfoBuilder builder) {
        if (!ClientConfig.showTechnical()) {
            return;
        }
        Level level = accessor.level();
        BlockPos pos = accessor.pos();
        BlockState state = accessor.state();

        float hardness = state.getDestroySpeed(level, pos);
        builder.detail(Format.labeled("camilookup.technical.hardness", hardness < 0
                ? Component.translatable("camilookup.technical.unbreakable")
                : Component.literal(Format.number(hardness))));

        Component tool = toolName(state);
        if (tool != null) {
            builder.detail(Format.labeled("camilookup.technical.tool", tool));
        }
        if (hardness >= 0) {
            boolean harvestable = !state.requiresCorrectToolForDrops() || accessor.player().hasCorrectToolForDrops(state);
            builder.detail(Format.labeled("camilookup.technical.harvestable", Format.yesNo(harvestable)));
        }

        builder.detail(Format.labeled("camilookup.technical.blast_resistance",
                Format.number(state.getBlock().getExplosionResistance())));

        BlockPos lightPos = state.canOcclude() ? pos.above() : pos;
        builder.detail(Format.labeled("camilookup.technical.light", Component.translatable("camilookup.technical.light_value",
                state.getLightEmission(level, pos),
                level.getBrightness(LightLayer.BLOCK, lightPos),
                level.getBrightness(LightLayer.SKY, lightPos))));

        level.getBiome(pos).unwrapKey().ifPresent(key -> builder.detail(Format.labeled("camilookup.technical.biome",
                Component.translatable(key.identifier().toLanguageKey("biome")))));

        builder.detail(Format.labeled("camilookup.technical.position",
                pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
    }

    private static Component toolName(BlockState state) {
        String tool = null;
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            tool = "pickaxe";
        } else if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            tool = "axe";
        } else if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            tool = "shovel";
        } else if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
            tool = "hoe";
        }
        if (tool == null) {
            return null;
        }
        MutableComponent name = Component.translatable("camilookup.tool." + tool);
        String tier = null;
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            tier = "diamond";
        } else if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
            tier = "iron";
        } else if (state.is(BlockTags.NEEDS_STONE_TOOL)) {
            tier = "stone";
        }
        if (tier != null) {
            name.append(" (").append(Component.translatable("camilookup.tier." + tier)).append(")");
        }
        return name;
    }
}
