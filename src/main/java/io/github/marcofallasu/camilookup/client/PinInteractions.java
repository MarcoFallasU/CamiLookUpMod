package io.github.marcofallasu.camilookup.client;

import io.github.marcofallasu.camilookup.api.target.LookUpAccessor;
import io.github.marcofallasu.camilookup.client.pin.Pin;
import io.github.marcofallasu.camilookup.client.pin.PinManager;
import io.github.marcofallasu.camilookup.client.render.LookUpOverlay;
import io.github.marcofallasu.camilookup.client.render.PanelButton;
import io.github.marcofallasu.camilookup.core.LookUpRegistry;
import io.github.marcofallasu.camilookup.core.Targets;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Mouse handling while cursor mode is active.
 * <ul>
 *     <li>Click a target: keep its box open next to it (click again, or click the box, to close it).</li>
 *     <li>Pin button of an open box, or CTRL + click a target: pin it as a window (needs the mod on the server).</li>
 *     <li>Drag a window by its header to move it and by its bottom-right corner to resize it; its close button
 *     closes it.</li>
 *     <li>Right click a box: addon action. Mouse wheel over a box: scroll it.</li>
 * </ul>
 */
final class PinInteractions {
    private static final double SCROLL_STEP = 10.0;

    private PinInteractions() {
    }

    static void onPress(int button, boolean controlDown) {
        Minecraft minecraft = Minecraft.getInstance();
        double mouseX = minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        double mouseY = minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
        Pin pin = PinManager.pinAt(mouseX, mouseY);

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (pin != null) {
                clickBox(pin, mouseX, mouseY);
                return;
            }
            LookUpAccessor target = LookUpOverlay.hovered();
            if (target == null) {
                return;
            }
            if (controlDown) {
                if (ClientState.serverHasMod()) {
                    PinManager.pinWindow(target, mouseX + 12, mouseY - 12);
                }
            } else {
                PinManager.toggleOpen(target);
            }
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && pin != null) {
            runPinAction(pin);
        }
    }

    static void onRelease(int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            PinManager.stopDrag();
        }
    }

    private static void clickBox(Pin pin, double mouseX, double mouseY) {
        PanelButton button = pin.buttonAt(mouseX, mouseY);
        if (pin.mode() == Pin.Mode.WINDOW) {
            if (button == PanelButton.CLOSE) {
                PinManager.remove(pin);
            } else if (pin.isOnResizeGrip(mouseX, mouseY)) {
                PinManager.startResize(pin, mouseX, mouseY);
            } else if (pin.isOnHeader(mouseX, mouseY)) {
                PinManager.startDrag(pin, mouseX, mouseY);
            } else {
                PinManager.bringToFront(pin);
            }
            return;
        }
        if (button == PanelButton.PIN && ClientState.serverHasMod()) {
            // The window appears where the open box was, at full size.
            PinManager.remove(pin);
            PinManager.pinWindow(pin.accessor(), pin.left(), pin.top());
        } else {
            PinManager.remove(pin);
        }
    }

    private static void runPinAction(Pin pin) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        LookUpAccessor target = Targets.resolve(minecraft.level, minecraft.player, pin.ref(), false);
        if (target == null) {
            return;
        }
        for (LookUpRegistry.PinActionEntry entry : LookUpRegistry.get().pinActions()) {
            if (entry.action().appliesTo(target)) {
                entry.action().onRightClick(target);
                return;
            }
        }
    }

    /** Scrolls the box under the mouse; returns whether the scroll was used. */
    static boolean onScroll(double deltaY) {
        Minecraft minecraft = Minecraft.getInstance();
        Pin pin = PinManager.pinAt(minecraft.mouseHandler.getScaledXPos(minecraft.getWindow()),
                minecraft.mouseHandler.getScaledYPos(minecraft.getWindow()));
        if (pin == null) {
            return false;
        }
        pin.scroll(deltaY * SCROLL_STEP);
        return true;
    }
}
