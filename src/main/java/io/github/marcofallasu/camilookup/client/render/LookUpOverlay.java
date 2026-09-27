package io.github.marcofallasu.camilookup.client.render;

import com.mojang.blaze3d.platform.Window;
import io.github.marcofallasu.camilookup.api.target.BlockAccessor;
import io.github.marcofallasu.camilookup.api.target.EntityAccessor;
import io.github.marcofallasu.camilookup.api.target.LookUpAccessor;
import io.github.marcofallasu.camilookup.api.target.TargetRef;
import io.github.marcofallasu.camilookup.client.CameraProjection;
import io.github.marcofallasu.camilookup.client.ClientState;
import io.github.marcofallasu.camilookup.client.CursorMode;
import io.github.marcofallasu.camilookup.client.CursorPicker;
import io.github.marcofallasu.camilookup.client.DisplayInfo;
import io.github.marcofallasu.camilookup.client.pin.Pin;
import io.github.marcofallasu.camilookup.client.pin.PinManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** HUD layer drawing the open boxes, the pinned windows and the box under the cursor. */
public final class LookUpOverlay {
    /** Offset of a box from the point it belongs to, like vanilla tooltips. */
    private static final int OFFSET = 12;
    private static final int SCREEN_MARGIN = 4;
    /** Open boxes are drawn at full size up to this distance, and shrink beyond it. */
    private static final double FULL_SIZE_DISTANCE = 6.0;
    private static final float MIN_OPEN_SCALE = 0.5F;
    /** Height above an entity's head where its open box floats. */
    private static final double ENTITY_LABEL_HEIGHT = 0.4;

    private static @Nullable LookUpAccessor hovered;
    private static @Nullable Pin lastPinUnderMouse;
    private static @Nullable DisplayInfo hoveredInfo;
    private static @Nullable TargetRef hoveredInfoRef;
    private static long hoveredInfoTick = -1;
    private static long tick;

    private LookUpOverlay() {
    }

    /** The target under the cursor in the last frame, if cursor mode is active and no box covers it. */
    public static @Nullable LookUpAccessor hovered() {
        return hovered;
    }

    public static void tick() {
        tick++;
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        CameraProjection projection = CameraProjection.current();
        if (minecraft.level == null || minecraft.player == null || projection == null) {
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

        if (cursor) {
            PinManager.drag(mouseX, mouseY);
        } else {
            PinManager.stopDrag();
        }

        // Lay out every box first so the one under the mouse is known before drawing.
        for (Pin pin : PinManager.pins()) {
            if (pin.mode() == Pin.Mode.OPEN) {
                layoutOpen(pin, projection, partialTick, font, screenWidth, screenHeight, minecraft.hasShiftDown());
            } else {
                layoutWindow(pin, font, screenWidth, screenHeight);
            }
        }
        Pin pinUnderMouse = cursor ? PinManager.pinAt(mouseX, mouseY) : null;
        lastPinUnderMouse = pinUnderMouse;

        ItemStack hoveredStack = ItemStack.EMPTY;
        PanelButton hoveredButton = null;
        for (Pin pin : PinManager.pins()) {
            if (!pin.isVisible()) {
                continue;
            }
            boolean underMouse = pin == pinUnderMouse;
            graphics.nextStratum();
            graphics.pose().pushMatrix();
            graphics.pose().translate((float) pin.left(), (float) pin.top());
            graphics.pose().scale(pin.scale(), pin.scale());
            ItemStack stack = pin.panel().render(graphics, font, 0, 0, pin.maxHeight(), pin.scroll(),
                    underMouse ? pin.localX(mouseX) : Double.NaN, underMouse ? pin.localY(mouseY) : Double.NaN);
            graphics.pose().popMatrix();
            if (underMouse) {
                hoveredStack = stack;
                hoveredButton = pin.buttonAt(mouseX, mouseY);
            }
        }

        hovered = null;
        if (cursor && pinUnderMouse == null && !PinManager.isDragging()) {
            hovered = CursorPicker.pick(projection, minecraft.mouseHandler.xpos(), minecraft.mouseHandler.ypos(), partialTick);
            if (hovered != null && PinManager.find(hovered.ref(), Pin.Mode.OPEN) == null) {
                InfoPanel panel = InfoPanel.layout(hoverInfo(hovered), minecraft.hasShiftDown(), font);
                int maxHeight = maxWindowHeight(screenHeight);
                int[] position = placeNextTo(mouseX, mouseY, panel.width(maxHeight), panel.height(maxHeight), screenWidth, screenHeight);
                graphics.nextStratum();
                panel.render(graphics, font, position[0], position[1], maxHeight, 0, Double.NaN, Double.NaN);
            }
        }

        if (hoveredButton != null) {
            graphics.setTooltipForNextFrame(font, Component.translatable(hoveredButton.tooltipKey()), (int) mouseX, (int) mouseY);
            graphics.renderDeferredElements();
        } else if (!hoveredStack.isEmpty()) {
            graphics.setTooltipForNextFrame(font, hoveredStack, (int) mouseX, (int) mouseY);
            graphics.renderDeferredElements();
        }
    }

    /** Places an open box floating above its target, shrinking it with distance. */
    private static void layoutOpen(Pin pin, CameraProjection projection, float partialTick, Font font,
                                   int screenWidth, int screenHeight, boolean shiftDown) {
        Vec3 anchor = labelAnchor(pin.accessor(), partialTick);
        double[] point = projection.toGui(anchor);
        if (point == null || point[0] < 0 || point[0] > screenWidth || point[1] < 0 || point[1] > screenHeight) {
            pin.hide();
            return;
        }
        double distance = projection.cameraPos().distanceTo(anchor);
        float scale = (float) Mth.clamp(FULL_SIZE_DISTANCE / Math.max(distance, 0.01), MIN_OPEN_SCALE, 1.0);
        boolean detailed = shiftDown && pin == lastPinUnderMouse;
        List<PanelButton> buttons = ClientState.serverHasMod() ? List.of(PanelButton.PIN) : List.of();
        InfoPanel panel = InfoPanel.layout(pin.info(), detailed, font, buttons);
        int maxHeight = maxOpenHeight(screenHeight);
        double width = panel.width(maxHeight) * scale;
        double height = panel.height(maxHeight) * scale;
        double gap = (InfoPanel.BORDER + 2) * scale;
        pin.setLayout(panel, point[0] - width / 2, point[1] - height - gap, scale, maxHeight);
    }

    /** Places a pinned window at its saved position, kept inside the screen. */
    private static void layoutWindow(Pin pin, Font font, int screenWidth, int screenHeight) {
        InfoPanel panel = InfoPanel.layout(pin.info(), true, font, List.of(PanelButton.CLOSE));
        int maxHeight = maxWindowHeight(screenHeight);
        int width = panel.width(maxHeight);
        int height = panel.height(maxHeight);
        double x = Mth.clamp(pin.windowX(), SCREEN_MARGIN, Math.max(SCREEN_MARGIN, screenWidth - width - SCREEN_MARGIN));
        double y = Mth.clamp(pin.windowY(), SCREEN_MARGIN, Math.max(SCREEN_MARGIN, screenHeight - height - SCREEN_MARGIN));
        if (!PinManager.isDragging(pin)) {
            pin.moveWindow(x, y);
        }
        pin.setLayout(panel, Math.round(x), Math.round(y), 1.0F, maxHeight);
    }

    private static int maxWindowHeight(int screenHeight) {
        return Math.max(60, (int) (screenHeight * 0.6));
    }

    private static int maxOpenHeight(int screenHeight) {
        return Math.max(60, (int) (screenHeight * 0.45));
    }

    private static DisplayInfo hoverInfo(LookUpAccessor accessor) {
        if (hoveredInfo == null || !accessor.ref().equals(hoveredInfoRef) || hoveredInfoTick != tick) {
            hoveredInfo = DisplayInfo.build(accessor);
            hoveredInfoRef = accessor.ref();
            hoveredInfoTick = tick;
        }
        return hoveredInfo;
    }

    /** The point an open box floats above: the top of a block, or just above an entity's head. */
    private static Vec3 labelAnchor(LookUpAccessor accessor, float partialTick) {
        return switch (accessor) {
            case BlockAccessor block -> Vec3.atCenterOf(block.pos()).add(0, 0.5, 0);
            case EntityAccessor entityAccessor -> {
                Entity entity = entityAccessor.entity();
                yield entity.getPosition(partialTick).add(0, entity.getBbHeight() + ENTITY_LABEL_HEIGHT, 0);
            }
        };
    }

    /** Places a box next to a point like vanilla tooltips, keeping it inside the screen. */
    public static int[] placeNextTo(double x, double y, int width, int height, int screenWidth, int screenHeight) {
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
