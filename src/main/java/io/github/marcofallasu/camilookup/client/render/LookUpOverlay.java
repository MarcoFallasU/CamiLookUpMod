package io.github.marcofallasu.camilookup.client.render;

import com.mojang.blaze3d.platform.Window;
import io.github.marcofallasu.camilookup.api.target.BlockAccessor;
import io.github.marcofallasu.camilookup.api.target.EntityAccessor;
import io.github.marcofallasu.camilookup.api.target.LookUpAccessor;
import io.github.marcofallasu.camilookup.api.target.TargetRef;
import io.github.marcofallasu.camilookup.client.CameraProjection;
import io.github.marcofallasu.camilookup.client.CursorMode;
import io.github.marcofallasu.camilookup.client.CursorPicker;
import io.github.marcofallasu.camilookup.client.DisplayInfo;
import io.github.marcofallasu.camilookup.client.pin.Pin;
import io.github.marcofallasu.camilookup.client.pin.PinManager;
import io.github.marcofallasu.camilookup.config.ClientConfig.PinAnchor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** HUD layer drawing the pinned boxes and the box under the cursor. */
public final class LookUpOverlay {
    /** Offset of a box from the point it belongs to, like vanilla tooltips. */
    private static final int OFFSET = 12;
    private static final int SCREEN_MARGIN = 4;

    private static @Nullable LookUpAccessor hovered;
    private static @Nullable DisplayInfo hoveredInfo;
    private static @Nullable TargetRef hoveredInfoRef;
    private static long hoveredInfoTick = -1;
    private static long tick;

    private LookUpOverlay() {
    }

    /** The target under the cursor in the last frame, if cursor mode is active and no pinned box covers it. */
    public static @Nullable LookUpAccessor hovered() {
        return hovered;
    }

    public static void tick() {
        tick++;
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            hovered = null;
            return;
        }
        CameraProjection projection = CameraProjection.current();
        if (projection == null) {
            hovered = null;
            return;
        }
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        Font font = minecraft.font;
        Window window = minecraft.getWindow();
        boolean cursor = CursorMode.isActive();
        double mouseX = cursor ? minecraft.mouseHandler.getScaledXPos(window) : Double.NaN;
        double mouseY = cursor ? minecraft.mouseHandler.getScaledYPos(window) : Double.NaN;
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int maxHeight = Math.max(60, (int) (screenHeight * 0.6));

        // Lay out the pins first so the one under the mouse is known before drawing.
        for (Pin pin : PinManager.pins()) {
            double[] point = pin.anchor() == PinAnchor.SCREEN
                    ? new double[]{pin.screenX(), pin.screenY()}
                    : projection.toGui(anchorOf(pin.accessor(), partialTick));
            if (point == null || point[0] < 0 || point[0] > screenWidth || point[1] < 0 || point[1] > screenHeight) {
                pin.hide();
                continue;
            }
            InfoPanel panel = InfoPanel.layout(pin.info(), true, font);
            int[] position = place(point[0], point[1], panel.width(maxHeight), panel.height(maxHeight), screenWidth, screenHeight);
            pin.setLayout(panel, position[0], position[1], maxHeight);
        }
        Pin pinUnderMouse = cursor ? PinManager.pinAt(mouseX, mouseY) : null;

        ItemStack hoveredStack = ItemStack.EMPTY;
        for (Pin pin : PinManager.pins()) {
            if (!pin.isVisible()) {
                continue;
            }
            graphics.nextStratum();
            boolean underMouse = pin == pinUnderMouse;
            ItemStack stack = pin.panel().render(graphics, font, pin.left(), pin.top(), maxHeight, pin.scroll(),
                    underMouse ? mouseX : Double.NaN, underMouse ? mouseY : Double.NaN);
            if (!stack.isEmpty()) {
                hoveredStack = stack;
            }
        }

        hovered = null;
        if (cursor && pinUnderMouse == null) {
            hovered = CursorPicker.pick(projection, minecraft.mouseHandler.xpos(), minecraft.mouseHandler.ypos(), partialTick);
            if (hovered != null && !PinManager.hasTargetPin(hovered.ref())) {
                InfoPanel panel = InfoPanel.layout(hoverInfo(hovered), minecraft.hasShiftDown(), font);
                int[] position = place(mouseX, mouseY, panel.width(maxHeight), panel.height(maxHeight), screenWidth, screenHeight);
                graphics.nextStratum();
                panel.render(graphics, font, position[0], position[1], maxHeight, 0, Double.NaN, Double.NaN);
            }
        }

        if (!hoveredStack.isEmpty()) {
            graphics.setTooltipForNextFrame(font, hoveredStack, (int) mouseX, (int) mouseY);
            graphics.renderDeferredElements();
        }
    }

    private static DisplayInfo hoverInfo(LookUpAccessor accessor) {
        if (hoveredInfo == null || !accessor.ref().equals(hoveredInfoRef) || hoveredInfoTick != tick) {
            hoveredInfo = DisplayInfo.build(accessor);
            hoveredInfoRef = accessor.ref();
            hoveredInfoTick = tick;
        }
        return hoveredInfo;
    }

    private static Vec3 anchorOf(LookUpAccessor accessor, float partialTick) {
        return switch (accessor) {
            case BlockAccessor block -> block.anchor();
            case EntityAccessor entityAccessor -> {
                Entity entity = entityAccessor.entity();
                yield entity.getPosition(partialTick).add(0, entity.getBbHeight() / 2.0, 0);
            }
        };
    }

    /** Places a box next to a point like vanilla tooltips, keeping it inside the screen. */
    private static int[] place(double x, double y, int width, int height, int screenWidth, int screenHeight) {
        int left = (int) x + OFFSET;
        int top = (int) y - OFFSET;
        if (left + width + SCREEN_MARGIN > screenWidth) {
            left = Math.max(SCREEN_MARGIN, (int) x - OFFSET - width - SCREEN_MARGIN);
        }
        if (top + height + SCREEN_MARGIN > screenHeight) {
            top = screenHeight - height - SCREEN_MARGIN;
        }
        top = Math.max(SCREEN_MARGIN, top);
        return new int[]{left, top};
    }
}
