package io.github.marcofallasu.camilookup.client;

import io.github.marcofallasu.camilookup.config.ClientConfig;
import io.github.marcofallasu.camilookup.config.ClientConfig.ActivationMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/** In-game client settings, opened from the Config button in the mod list. */
public final class CamiConfigScreen extends Screen {
    private static final int WIDGET_WIDTH = 240;
    private static final int WIDGET_HEIGHT = 20;
    private static final int SPACING = 24;
    private static final double MIN_DISTANCE = 1.0;
    private static final double MAX_DISTANCE = 256.0;

    private final @Nullable Screen parent;

    public CamiConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("camilookup.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = (width - WIDGET_WIDTH) / 2;
        int y = height / 4;

        addRenderableWidget(CycleButton.builder(
                        (ActivationMode mode) -> Component.translatable("camilookup.config.activation_mode." + mode.name().toLowerCase(Locale.ROOT)),
                        ClientConfig.activationMode())
                .withValues(List.of(ActivationMode.values()))
                .create(x, y, WIDGET_WIDTH, WIDGET_HEIGHT, Component.translatable("camilookup.config.activation_mode"),
                        (button, mode) -> ClientConfig.setActivationMode(mode)));

        addRenderableWidget(new DistanceSlider(x, y + SPACING, ClientConfig.inspectDistance()));

        addRenderableWidget(CycleButton.onOffBuilder(ClientConfig.showTechnical())
                .create(x, y + SPACING * 2, WIDGET_WIDTH, WIDGET_HEIGHT, Component.translatable("camilookup.config.show_technical"),
                        (button, show) -> ClientConfig.setShowTechnical(show)));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(x, y + SPACING * 4, WIDGET_WIDTH, WIDGET_HEIGHT)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 4 - 24, -1);
    }

    @Override
    public void onClose() {
        ClientConfig.save();
        minecraft.setScreen(parent);
    }

    private static final class DistanceSlider extends AbstractSliderButton {
        DistanceSlider(int x, int y, double distance) {
            super(x, y, WIDGET_WIDTH, WIDGET_HEIGHT, Component.empty(), toSlider(distance));
            updateMessage();
        }

        private static double toSlider(double distance) {
            return (Mth.clamp(distance, MIN_DISTANCE, MAX_DISTANCE) - MIN_DISTANCE) / (MAX_DISTANCE - MIN_DISTANCE);
        }

        private double distance() {
            return Math.round(MIN_DISTANCE + value * (MAX_DISTANCE - MIN_DISTANCE));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("camilookup.config.inspect_distance", (int) distance()));
        }

        @Override
        protected void applyValue() {
            ClientConfig.setInspectDistance(distance());
        }
    }
}
