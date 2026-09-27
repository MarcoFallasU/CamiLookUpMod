package io.github.marcofallasu.camilookup.client.render;

import net.minecraft.client.gui.GuiGraphics;

/** Small icon buttons drawn in the header of a box. */
public enum PanelButton {
    /** Turns a box kept open near its target into a movable window. */
    PIN("camilookup.button.pin",
            "..####..",
            "..####..",
            "..####..",
            ".######.",
            "########",
            "...##...",
            "...##...",
            "...#...."),
    /** Closes a window. */
    CLOSE("camilookup.button.close",
            "##....##",
            "###..###",
            ".######.",
            "..####..",
            "..####..",
            ".######.",
            "###..###",
            "##....##");

    public static final int SIZE = 8;

    private static final int COLOR = 0xFFAAAAAA;
    private static final int HOVER_COLOR = 0xFFFFFF55;

    private final String tooltipKey;
    private final String[] pixels;

    PanelButton(String tooltipKey, String... pixels) {
        this.tooltipKey = tooltipKey;
        this.pixels = pixels;
    }

    public String tooltipKey() {
        return tooltipKey;
    }

    void render(GuiGraphics graphics, int x, int y, boolean hovered) {
        int color = hovered ? HOVER_COLOR : COLOR;
        for (int row = 0; row < pixels.length; row++) {
            String line = pixels[row];
            for (int column = 0; column < line.length(); column++) {
                if (line.charAt(column) == '#') {
                    graphics.fill(x + column, y + row, x + column + 1, y + row + 1, color);
                }
            }
        }
    }
}
