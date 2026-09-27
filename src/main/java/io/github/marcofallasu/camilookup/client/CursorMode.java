package io.github.marcofallasu.camilookup.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.marcofallasu.camilookup.CamiLookUp;
import io.github.marcofallasu.camilookup.config.ClientConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Cursor mode: the camera stays fixed and the mouse cursor is freed over the screen, while the player can still move.
 */
public final class CursorMode {
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(CamiLookUp.MODID, "main"));
    public static final KeyMapping KEY = new KeyMapping("key.camilookup.cursor", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY, 0);

    private static boolean active;
    private static boolean toggled;
    /** Set when the window loses focus, so a key stuck down by Alt+Tab does not re-enter cursor mode. */
    private static boolean waitForRelease;

    private CursorMode() {
    }

    public static boolean isActive() {
        return active;
    }

    /** Called every frame, before rendering. */
    static void update() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean focused = minecraft.isWindowActive();
        boolean available = focused && minecraft.screen == null && minecraft.getOverlay() == null
                && minecraft.level != null && minecraft.player != null;
        if (!available) {
            while (KEY.consumeClick()) {
                // Discard presses made while cursor mode is unavailable.
            }
            if (!focused) {
                waitForRelease = true;
                KEY.setDown(false);
            }
            toggled = false;
            setActive(false);
            return;
        }

        if (ClientConfig.activationMode() == ClientConfig.ActivationMode.TOGGLE) {
            while (KEY.consumeClick()) {
                toggled = !toggled;
            }
            setActive(toggled);
        } else {
            while (KEY.consumeClick()) {
                // Hold mode only looks at whether the key is down.
            }
            boolean down = KEY.isDown();
            if (waitForRelease) {
                if (!down) {
                    waitForRelease = false;
                }
                down = false;
            }
            setActive(down);
        }
    }

    private static void setActive(boolean value) {
        if (active == value) {
            return;
        }
        active = value;
        Minecraft minecraft = Minecraft.getInstance();
        if (value) {
            // Stop a click that was held when entering, since its release will not reach the game.
            minecraft.options.keyAttack.setDown(false);
            minecraft.options.keyUse.setDown(false);
            minecraft.options.keyPickItem.setDown(false);
            minecraft.mouseHandler.releaseMouse();
        } else if (minecraft.screen == null && minecraft.isWindowActive()) {
            minecraft.mouseHandler.grabMouse();
        }
    }
}
