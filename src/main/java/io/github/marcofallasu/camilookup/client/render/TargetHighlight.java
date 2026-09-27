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
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.event.RenderHighlightEvent;

/**
 * In cursor mode, replaces the vanilla crosshair outline with an outline around the block or entity under the cursor.
 */
public final class TargetHighlight {
    private static final int COLOR = 0xE6FFFFFF;
    /** Grows entity boxes slightly so the outline does not hide inside the model. */
    private static final double ENTITY_MARGIN = 0.02;

    private TargetHighlight() {
    }

    /** Listener for both highlight events; returns {@code true} to cancel the vanilla outline. */
    public static boolean onHighlight(RenderHighlightEvent event) {
        if (!CursorMode.isActive()) {
            return false;
        }
        if (LookUpOverlay.hovered() == null) {
            // The crosshair target is not what the player is pointing at in cursor mode.
            return true;
        }
        event.setCustomRenderer(TargetHighlight::render);
        return false;
    }

    private static void render(MultiBufferSource.BufferSource buffers, PoseStack poseStack, boolean translucent, LevelRenderState state) {
        LookUpAccessor target = LookUpOverlay.hovered();
        if (translucent || target == null || !CursorMode.isActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 camera = state.cameraRenderState.pos;
        VoxelShape shape;
        Vec3 origin;
        switch (target) {
            case BlockAccessor block -> {
                BlockPos pos = block.pos();
                shape = block.state().getShape(block.level(), pos, CollisionContext.of(block.player()));
                if (shape.isEmpty()) {
                    shape = Shapes.block();
                }
                origin = Vec3.atLowerCornerOf(pos);
            }
            case EntityAccessor entityAccessor -> {
                Entity entity = entityAccessor.entity();
                Vec3 position = entity.getPosition(minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
                AABB box = entity.getBoundingBox().move(entity.position().scale(-1)).inflate(ENTITY_MARGIN);
                shape = Shapes.create(box);
                origin = position;
            }
        }
        VertexConsumer lines = buffers.getBuffer(RenderTypes.lines());
        ShapeRenderer.renderShape(poseStack, lines, shape, origin.x - camera.x, origin.y - camera.y, origin.z - camera.z,
                COLOR, minecraft.getWindow().getAppropriateLineWidth());
        buffers.endLastBatch();
    }
}
