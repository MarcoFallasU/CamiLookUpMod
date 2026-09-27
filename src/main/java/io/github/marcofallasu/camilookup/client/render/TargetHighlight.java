package io.github.marcofallasu.camilookup.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.marcofallasu.camilookup.api.target.BlockAccessor;
import io.github.marcofallasu.camilookup.api.target.EntityAccessor;
import io.github.marcofallasu.camilookup.api.target.LookUpAccessor;
import io.github.marcofallasu.camilookup.client.CursorMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.event.RenderHighlightEvent;
import org.jetbrains.annotations.Nullable;

/**
 * In cursor mode, highlights the target under the cursor instead of the crosshair target: blocks get an outline and
 * entities get the vanilla glowing effect.
 */
public final class TargetHighlight {
    private static final int BLOCK_COLOR = 0xE6FFFFFF;
    /** Maximum distance between an entity's interpolated position and its render state to consider them the same. */
    private static final double MATCH_DISTANCE_SQR = 1.0E-4;

    private TargetHighlight() {
    }

    /**
     * Listener for both highlight events, fired every frame right after entities are extracted for rendering.
     *
     * @return {@code true} to cancel the vanilla crosshair outline
     */
    public static boolean onHighlight(RenderHighlightEvent event) {
        if (!CursorMode.isActive()) {
            return false;
        }
        LookUpAccessor target = LookUpOverlay.hovered();
        if (target instanceof BlockAccessor) {
            event.setCustomRenderer(TargetHighlight::renderBlockOutline);
            return false;
        }
        if (target instanceof EntityAccessor entity) {
            glow(event.getLevelRenderState(), entity.entity());
        }
        // In cursor mode the crosshair target is not what the player is pointing at.
        return true;
    }

    /** Gives the entity the same outline as the vanilla glowing effect, for this frame only. */
    private static void glow(LevelRenderState state, Entity entity) {
        EntityRenderState renderState = findRenderState(state, entity);
        if (renderState != null) {
            renderState.outlineColor = ARGB.opaque(entity.getTeamColor());
            state.haveGlowingEntities = true;
        }
    }

    private static @Nullable EntityRenderState findRenderState(LevelRenderState state, Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean frozen = minecraft.level != null && minecraft.level.tickRateManager().isEntityFrozen(entity);
        Vec3 position = entity.getPosition(minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(!frozen));
        EntityRenderState closest = null;
        double closestDistance = MATCH_DISTANCE_SQR;
        for (EntityRenderState renderState : state.entityRenderStates) {
            if (renderState.entityType != entity.getType()) {
                continue;
            }
            double distance = position.distanceToSqr(renderState.x, renderState.y, renderState.z);
            if (distance <= closestDistance) {
                closest = renderState;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private static void renderBlockOutline(MultiBufferSource.BufferSource buffers, PoseStack poseStack, boolean translucent,
                                           LevelRenderState state) {
        if (translucent || !CursorMode.isActive() || !(LookUpOverlay.hovered() instanceof BlockAccessor block)) {
            return;
        }
        BlockPos pos = block.pos();
        VoxelShape shape = block.state().getShape(block.level(), pos, CollisionContext.of(block.player()));
        if (shape.isEmpty()) {
            shape = Shapes.block();
        }
        Vec3 camera = state.cameraRenderState.pos;
        VertexConsumer lines = buffers.getBuffer(RenderTypes.lines());
        ShapeRenderer.renderShape(poseStack, lines, shape, pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z,
                BLOCK_COLOR, Minecraft.getInstance().getWindow().getAppropriateLineWidth());
        buffers.endLastBatch();
    }
}
