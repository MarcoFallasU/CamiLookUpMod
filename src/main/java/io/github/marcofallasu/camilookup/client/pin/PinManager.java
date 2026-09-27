package io.github.marcofallasu.camilookup.client.pin;

import io.github.marcofallasu.camilookup.api.target.BlockAccessor;
import io.github.marcofallasu.camilookup.api.target.LookUpAccessor;
import io.github.marcofallasu.camilookup.api.target.TargetRef;
import io.github.marcofallasu.camilookup.client.ClientState;
import io.github.marcofallasu.camilookup.client.DisplayInfo;
import io.github.marcofallasu.camilookup.client.ServerInfoCache;
import io.github.marcofallasu.camilookup.core.Targets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Keeps the boxes kept open next to their targets and the pinned windows. Open boxes close when their target is gone
 * or out of range; windows stay until closed and keep their last information if the target becomes unavailable.
 */
public final class PinManager {
    /** Ticks between server refreshes of a kept or pinned box. */
    private static final int REFRESH_TICKS = 10;
    /** Extra distance before an open box closes, so it does not flicker at the edge of the range. */
    private static final double RANGE_MARGIN = 2.0;

    /** Draw order: later entries are drawn on top. Windows are always drawn above open boxes. */
    private static final List<Pin> PINS = new ArrayList<>();
    private static @Nullable ClientLevel level;

    private static @Nullable Pin dragging;
    private static double dragOffsetX;
    private static double dragOffsetY;

    private PinManager() {
    }

    public static List<Pin> pins() {
        return Collections.unmodifiableList(PINS);
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != level) {
            clear();
            level = minecraft.level;
        }
        if (level == null || minecraft.player == null) {
            return;
        }
        Iterator<Pin> iterator = PINS.iterator();
        while (iterator.hasNext()) {
            Pin pin = iterator.next();
            LookUpAccessor accessor = Targets.resolve(level, minecraft.player, pin.ref(), false);
            boolean exists = accessor != null
                    && !(accessor instanceof BlockAccessor block && block.state().getBlock() != pin.block());
            if (pin.mode() == Pin.Mode.OPEN) {
                if (!exists || Targets.distance(minecraft.player.getEyePosition(), accessor) > ClientState.maxDistance() + RANGE_MARGIN) {
                    iterator.remove();
                    continue;
                }
            }
            ServerInfoCache.want(pin.ref(), REFRESH_TICKS);
            if (exists) {
                pin.update(accessor, DisplayInfo.build(accessor));
            } else {
                pin.markUnavailable();
            }
        }
    }

    /** Opens a box next to the target, or closes it if it was already open. */
    public static void toggleOpen(LookUpAccessor accessor) {
        Pin existing = find(accessor.ref(), Pin.Mode.OPEN);
        if (existing != null) {
            remove(existing);
        } else {
            add(accessor, Pin.Mode.OPEN, 0, 0);
        }
    }

    /** Pins the target as a window at a screen position, or brings its existing window to the front. */
    public static Pin pinWindow(LookUpAccessor accessor, double x, double y) {
        Pin existing = find(accessor.ref(), Pin.Mode.WINDOW);
        if (existing != null) {
            bringToFront(existing);
            return existing;
        }
        return add(accessor, Pin.Mode.WINDOW, x, y);
    }

    private static Pin add(LookUpAccessor accessor, Pin.Mode mode, double x, double y) {
        ServerInfoCache.want(accessor.ref(), REFRESH_TICKS);
        Pin pin = new Pin(accessor, mode, accessor instanceof BlockAccessor block ? block.state().getBlock() : null,
                DisplayInfo.build(accessor), x, y);
        PINS.add(pin);
        sort();
        return pin;
    }

    public static void remove(Pin pin) {
        PINS.remove(pin);
        if (dragging == pin) {
            dragging = null;
        }
    }

    public static void bringToFront(Pin pin) {
        if (PINS.remove(pin)) {
            PINS.add(pin);
            sort();
        }
    }

    private static void sort() {
        // Stable sort: open boxes first, windows last, keeping the order within each group.
        PINS.sort((a, b) -> Boolean.compare(a.mode() == Pin.Mode.WINDOW, b.mode() == Pin.Mode.WINDOW));
    }

    public static @Nullable Pin find(TargetRef ref, Pin.Mode mode) {
        for (Pin pin : PINS) {
            if (pin.mode() == mode && pin.ref().equals(ref)) {
                return pin;
            }
        }
        return null;
    }

    /** The topmost visible box under the mouse. */
    public static @Nullable Pin pinAt(double mouseX, double mouseY) {
        for (int i = PINS.size() - 1; i >= 0; i--) {
            if (PINS.get(i).contains(mouseX, mouseY)) {
                return PINS.get(i);
            }
        }
        return null;
    }

    public static void startDrag(Pin pin, double mouseX, double mouseY) {
        dragging = pin;
        dragOffsetX = mouseX - pin.windowX();
        dragOffsetY = mouseY - pin.windowY();
        bringToFront(pin);
    }

    public static void stopDrag() {
        dragging = null;
    }

    public static boolean isDragging(Pin pin) {
        return dragging == pin;
    }

    public static boolean isDragging() {
        return dragging != null;
    }

    public static void drag(double mouseX, double mouseY) {
        if (dragging != null) {
            dragging.moveWindow(mouseX - dragOffsetX, mouseY - dragOffsetY);
        }
    }

    public static void clear() {
        PINS.clear();
        dragging = null;
    }
}
