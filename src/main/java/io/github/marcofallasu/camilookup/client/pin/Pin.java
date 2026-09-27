package io.github.marcofallasu.camilookup.client.pin;

import io.github.marcofallasu.camilookup.api.target.LookUpAccessor;
import io.github.marcofallasu.camilookup.api.target.TargetRef;
import io.github.marcofallasu.camilookup.client.DisplayInfo;
import io.github.marcofallasu.camilookup.client.render.InfoPanel;
import io.github.marcofallasu.camilookup.config.ClientConfig.PinAnchor;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/** A box pinned by the player. */
public final class Pin {
    private final TargetRef ref;
    private final PinAnchor anchor;
    private final @Nullable Block block;
    private final double screenX;
    private final double screenY;
    private LookUpAccessor accessor;
    private DisplayInfo info;
    private int scroll;

    // Layout of the last frame, used for mouse interaction.
    private boolean visible;
    private int left;
    private int top;
    private int width;
    private int height;
    private @Nullable InfoPanel panel;
    private int maxHeight;

    Pin(LookUpAccessor accessor, PinAnchor anchor, @Nullable Block block, DisplayInfo info, double screenX, double screenY) {
        this.ref = accessor.ref();
        this.accessor = accessor;
        this.anchor = anchor;
        this.block = block;
        this.info = info;
        this.screenX = screenX;
        this.screenY = screenY;
    }

    public TargetRef ref() {
        return ref;
    }

    public PinAnchor anchor() {
        return anchor;
    }

    @Nullable Block block() {
        return block;
    }

    /** The target as last resolved; for screen-anchored pins it may no longer exist. */
    public LookUpAccessor accessor() {
        return accessor;
    }

    void update(LookUpAccessor accessor, DisplayInfo info) {
        this.accessor = accessor;
        this.info = info;
    }

    public DisplayInfo info() {
        return info;
    }

    public double screenX() {
        return screenX;
    }

    public double screenY() {
        return screenY;
    }

    public int scroll() {
        return scroll;
    }

    public void scroll(double amount) {
        if (panel != null) {
            scroll = (int) Math.max(0, Math.min(panel.maxScroll(maxHeight), scroll - amount));
        }
    }

    public void setLayout(InfoPanel panel, int left, int top, int maxHeight) {
        this.visible = true;
        this.panel = panel;
        this.left = left;
        this.top = top;
        this.maxHeight = maxHeight;
        this.width = panel.width(maxHeight);
        this.height = panel.height(maxHeight);
        this.scroll = Math.min(scroll, panel.maxScroll(maxHeight));
    }

    public InfoPanel panel() {
        return panel;
    }

    public int left() {
        return left;
    }

    public int top() {
        return top;
    }

    public void hide() {
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean contains(double mouseX, double mouseY) {
        return visible
                && mouseX >= left - InfoPanel.BORDER && mouseX < left + width + InfoPanel.BORDER
                && mouseY >= top - InfoPanel.BORDER && mouseY < top + height + InfoPanel.BORDER;
    }
}
