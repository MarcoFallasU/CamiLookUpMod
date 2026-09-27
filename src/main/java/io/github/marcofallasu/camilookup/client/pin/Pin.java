package io.github.marcofallasu.camilookup.client.pin;

import io.github.marcofallasu.camilookup.api.target.LookUpAccessor;
import io.github.marcofallasu.camilookup.api.target.TargetRef;
import io.github.marcofallasu.camilookup.client.DisplayInfo;
import io.github.marcofallasu.camilookup.client.render.InfoPanel;
import io.github.marcofallasu.camilookup.client.render.PanelButton;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/** A box that stays on screen: kept open next to its target, or pinned as a movable window. */
public final class Pin {
    public enum Mode {
        /** Floats next to its block or entity, follows it and shrinks with distance. */
        OPEN,
        /** A window on the screen that can be dragged around. Needs Cami LookUp on the server. */
        WINDOW
    }

    private final TargetRef ref;
    private final Mode mode;
    private final @Nullable Block block;
    private LookUpAccessor accessor;
    private DisplayInfo info;
    private boolean available = true;
    private int scroll;
    private double windowX;
    private double windowY;

    // Layout of the last frame, used for mouse interaction.
    private boolean visible;
    private @Nullable InfoPanel panel;
    private double left;
    private double top;
    private float scale = 1.0F;
    private int maxHeight;

    Pin(LookUpAccessor accessor, Mode mode, @Nullable Block block, DisplayInfo info, double windowX, double windowY) {
        this.ref = accessor.ref();
        this.accessor = accessor;
        this.mode = mode;
        this.block = block;
        this.info = info;
        this.windowX = windowX;
        this.windowY = windowY;
    }

    public TargetRef ref() {
        return ref;
    }

    public Mode mode() {
        return mode;
    }

    @Nullable Block block() {
        return block;
    }

    /** The target as last resolved; a window's target may no longer exist. */
    public LookUpAccessor accessor() {
        return accessor;
    }

    public DisplayInfo info() {
        return info;
    }

    void update(LookUpAccessor accessor, DisplayInfo info) {
        this.accessor = accessor;
        this.info = info;
        this.available = true;
    }

    /** Keeps the last information, marked as no longer up to date. */
    void markUnavailable() {
        if (available) {
            available = false;
            info = info.unavailable();
        }
    }

    public double windowX() {
        return windowX;
    }

    public double windowY() {
        return windowY;
    }

    public void moveWindow(double x, double y) {
        windowX = x;
        windowY = y;
    }

    public int scroll() {
        return scroll;
    }

    public void scroll(double amount) {
        if (panel != null) {
            scroll = (int) Math.max(0, Math.min(panel.maxScroll(maxHeight), scroll - amount / scale));
        }
    }

    public void setLayout(InfoPanel panel, double left, double top, float scale, int maxHeight) {
        this.visible = true;
        this.panel = panel;
        this.left = left;
        this.top = top;
        this.scale = scale;
        this.maxHeight = maxHeight;
        this.scroll = Math.min(scroll, panel.maxScroll(maxHeight));
    }

    public void hide() {
        visible = false;
    }

    public boolean isVisible() {
        return visible && panel != null;
    }

    public InfoPanel panel() {
        return panel;
    }

    public double left() {
        return left;
    }

    public double top() {
        return top;
    }

    public float scale() {
        return scale;
    }

    public int maxHeight() {
        return maxHeight;
    }

    /** Converts a screen point to the box's own coordinates, where its content starts at (0, 0). */
    public double localX(double screenX) {
        return (screenX - left) / scale;
    }

    public double localY(double screenY) {
        return (screenY - top) / scale;
    }

    public boolean contains(double mouseX, double mouseY) {
        if (!isVisible()) {
            return false;
        }
        double x = localX(mouseX);
        double y = localY(mouseY);
        return x >= -InfoPanel.BORDER && x < panel.width(maxHeight) + InfoPanel.BORDER
                && y >= -InfoPanel.BORDER && y < panel.height(maxHeight) + InfoPanel.BORDER;
    }

    public boolean isOnHeader(double mouseX, double mouseY) {
        return contains(mouseX, mouseY) && localY(mouseY) < panel.headerHeight() + 1;
    }

    public @Nullable PanelButton buttonAt(double mouseX, double mouseY) {
        return isVisible() ? panel.buttonAt(localX(mouseX), localY(mouseY), 0, 0, maxHeight) : null;
    }
}
