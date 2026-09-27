package io.github.marcofallasu.camilookup.client.pin;

import io.github.marcofallasu.camilookup.api.target.BlockAccessor;
import io.github.marcofallasu.camilookup.api.target.LookUpAccessor;
import io.github.marcofallasu.camilookup.api.target.TargetRef;
import io.github.marcofallasu.camilookup.client.ClientState;
import io.github.marcofallasu.camilookup.client.DisplayInfo;
import io.github.marcofallasu.camilookup.client.ServerInfoCache;
import io.github.marcofallasu.camilookup.config.ClientConfig.PinAnchor;
import io.github.marcofallasu.camilookup.core.Targets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Keeps the pinned boxes, updating target-anchored ones and closing them when their target is gone. */
public final class PinManager {
    /** Ticks between server refreshes of a target-anchored pin. */
    private static final int REFRESH_TICKS = 10;
    /** Extra distance before a pin closes, so it does not flicker at the edge of the range. */
    private static final double RANGE_MARGIN = 2.0;

    private static final List<Pin> PINS = new ArrayList<>();
    private static @Nullable ClientLevel level;

    private PinManager() {
    }

    public static List<Pin> pins() {
        return Collections.unmodifiableList(PINS);
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != level) {
            PINS.clear();
            level = minecraft.level;
        }
        if (level == null || minecraft.player == null) {
            return;
        }
        Iterator<Pin> iterator = PINS.iterator();
        while (iterator.hasNext()) {
            Pin pin = iterator.next();
            if (pin.anchor() != PinAnchor.TARGET) {
                continue;
            }
            LookUpAccessor accessor = Targets.resolve(level, minecraft.player, pin.ref(), false);
            if (accessor == null
                    || accessor instanceof BlockAccessor block && block.state().getBlock() != pin.block()
                    || Targets.distance(minecraft.player.getEyePosition(), accessor) > ClientState.maxDistance() + RANGE_MARGIN) {
                iterator.remove();
                continue;
            }
            ServerInfoCache.want(pin.ref(), REFRESH_TICKS);
            pin.update(accessor, DisplayInfo.build(accessor));
        }
    }

    public static Pin add(LookUpAccessor accessor, PinAnchor anchor, double screenX, double screenY) {
        ServerInfoCache.want(accessor.ref(), REFRESH_TICKS);
        Pin pin = new Pin(accessor, anchor, accessor instanceof BlockAccessor block ? block.state().getBlock() : null,
                DisplayInfo.build(accessor), screenX, screenY);
        PINS.add(pin);
        return pin;
    }

    public static void remove(Pin pin) {
        PINS.remove(pin);
    }

    public static @Nullable Pin find(TargetRef ref) {
        for (Pin pin : PINS) {
            if (pin.ref().equals(ref)) {
                return pin;
            }
        }
        return null;
    }

    public static boolean hasTargetPin(TargetRef ref) {
        Pin pin = find(ref);
        return pin != null && pin.anchor() == PinAnchor.TARGET;
    }

    /** The topmost visible pin under the mouse. */
    public static @Nullable Pin pinAt(double mouseX, double mouseY) {
        for (int i = PINS.size() - 1; i >= 0; i--) {
            if (PINS.get(i).contains(mouseX, mouseY)) {
                return PINS.get(i);
            }
        }
        return null;
    }

    public static void clear() {
        PINS.clear();
    }
}
