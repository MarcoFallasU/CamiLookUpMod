package io.github.marcofallasu.camilookup.client;

import io.github.marcofallasu.camilookup.api.target.LookUpAccessor;
import io.github.marcofallasu.camilookup.client.pin.Pin;
import io.github.marcofallasu.camilookup.client.pin.PinManager;
import io.github.marcofallasu.camilookup.client.render.LookUpOverlay;
import io.github.marcofallasu.camilookup.config.ClientConfig;
import io.github.marcofallasu.camilookup.config.ClientConfig.PinAnchor;
import io.github.marcofallasu.camilookup.core.LookUpRegistry;
import io.github.marcofallasu.camilookup.core.Targets;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Mouse handling while cursor mode is active. */
final class PinInteractions {
    private static final double SCROLL_STEP = 10.0;

    private PinInteractions() {
    }

    static void onClick(int button, boolean controlDown) {
        Minecraft minecraft = Minecraft.getInstance();
        double mouseX = minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        double mouseY = minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
        Pin pin = PinManager.pinAt(mouseX, mouseY);

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (pin != null) {
                PinManager.remove(pin);
                return;
            }
            LookUpAccessor target = LookUpOverlay.hovered();
            if (target == null) {
                return;
            }
            Pin existing = PinManager.find(target.ref());
            if (existing != null) {
                PinManager.remove(existing);
                return;
            }
            PinAnchor anchor = ClientConfig.pinAnchor();
            if (controlDown) {
                anchor = anchor == PinAnchor.TARGET ? PinAnchor.SCREEN : PinAnchor.TARGET;
            }
            PinManager.add(target, anchor, mouseX, mouseY);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && pin != null) {
            runPinAction(pin);
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

    /** Scrolls the pinned box under the mouse; returns whether the scroll was used. */
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
