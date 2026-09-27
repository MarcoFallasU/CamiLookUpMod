package io.github.marcofallasu.camilookup.client;

import io.github.marcofallasu.camilookup.CamiLookUp;
import io.github.marcofallasu.camilookup.api.target.LookUpAccessor;
import io.github.marcofallasu.camilookup.client.pin.PinManager;
import io.github.marcofallasu.camilookup.client.render.LookUpOverlay;
import io.github.marcofallasu.camilookup.client.render.TargetHighlight;
import io.github.marcofallasu.camilookup.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

import java.util.function.Predicate;

/** Client-side setup. Only loaded on the physical client. */
public final class CamiLookUpClient {
    /** Ticks between server refreshes of the target under the cursor. */
    private static final int HOVER_REFRESH_TICKS = 5;

    private CamiLookUpClient() {
    }

    public static void init(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(CamiConfigScreen::new));

        RegisterKeyMappingsEvent.BUS.addListener(event -> event.register(CursorMode.KEY));
        AddGuiOverlayLayersEvent.BUS.addListener(event -> event.getLayeredDraw()
                .add(Identifier.fromNamespaceAndPath(CamiLookUp.MODID, "boxes"), LookUpOverlay::render)
                .addConditionTo(ForgeLayeredDraw.PRE_SLEEP_STACK, ForgeLayeredDraw.CROSSHAIR, () -> !CursorMode.isActive()));
        ViewportEvent.ComputeFov.BUS.addListener(Priority.LOWEST, CameraProjection::onComputeFov);
        RenderHighlightEvent.Block.BUS.addListener((Predicate<RenderHighlightEvent.Block>) TargetHighlight::onHighlight);
        RenderHighlightEvent.Entity.BUS.addListener((Predicate<RenderHighlightEvent.Entity>) TargetHighlight::onHighlight);

        TickEvent.RenderTickEvent.Pre.BUS.addListener(event -> CursorMode.update());
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> onClientTick());

        InputEvent.MouseButton.Pre.BUS.addListener(event -> {
            if (!CursorMode.isActive() || Minecraft.getInstance().screen != null) {
                return false;
            }
            if (event.getAction() == GLFW.GLFW_PRESS) {
                PinInteractions.onPress(event.getInfo().button(), event.getInfo().hasControlDown());
            } else if (event.getAction() == GLFW.GLFW_RELEASE) {
                PinInteractions.onRelease(event.getInfo().button());
            }
            // Cursor mode never lets clicks reach the world.
            return true;
        });
        InputEvent.MouseScrollingEvent.BUS.addListener(event ->
                CursorMode.isActive() && PinInteractions.onScroll(event.getDeltaY()));

        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> {
            ClientPacketHandler.onDisconnect();
            PinManager.clear();
        });
    }

    private static void onClientTick() {
        LookUpOverlay.tick();
        LookUpAccessor hovered = LookUpOverlay.hovered();
        if (hovered != null && CursorMode.isActive()) {
            ServerInfoCache.want(hovered.ref(), HOVER_REFRESH_TICKS);
        }
        ServerInfoCache.tick();
        PinManager.tick();
    }
}
