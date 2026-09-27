package io.github.marcofallasu.camilookup.client.render;

import io.github.marcofallasu.camilookup.api.info.InfoElement;
import io.github.marcofallasu.camilookup.client.DisplayInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lays out and draws one Cami LookUp box with the vanilla tooltip style. The header (icon and name) and the footer
 * (mod name) stay fixed; the body scrolls when it does not fit.
 */
public final class InfoPanel {
    /** Padding the vanilla tooltip background draws around the content. */
    public static final int BORDER = 3;
    private static final int CELL = 18;
    private static final int SUMMARY_GRID_LIMIT = 18;
    private static final int MAX_HEARTS = 20;
    private static final int SCROLLBAR = 4;
    private static final int BUTTON_SPACING = PanelButton.SIZE + 2;

    private static final Identifier HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");
    private static final Identifier HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.withDefaultNamespace("hud/heart/half");
    private static final Identifier BURN_PROGRESS = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final Identifier LIT_PROGRESS = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    /** Tint used to draw the empty part of the arrow and flame. */
    private static final int EMPTY_TINT = 0xFF404040;
    private static final int SLOT_COLOR = 0x30FFFFFF;

    private final ItemStack icon;
    private final Component title;
    private final List<Row> body;
    private final List<Row> footer;
    private final List<PanelButton> buttons;
    private final int contentWidth;
    private final int headerHeight;
    private final int bodyHeight;
    private final int footerHeight;

    private InfoPanel(ItemStack icon, Component title, List<Row> body, List<Row> footer, List<PanelButton> buttons, Font font) {
        this.icon = icon;
        this.title = title;
        this.body = body;
        this.footer = footer;
        this.buttons = List.copyOf(buttons);
        this.headerHeight = icon.isEmpty() ? 10 : 16;
        int width = font.width(title) + (icon.isEmpty() ? 0 : CELL + 2)
                + (buttons.isEmpty() ? 0 : 4 + buttons.size() * BUTTON_SPACING);
        int bodySum = 0;
        for (Row row : body) {
            width = Math.max(width, row.width());
            bodySum += row.height();
        }
        int footerSum = 0;
        for (Row row : footer) {
            width = Math.max(width, row.width());
            footerSum += row.height();
        }
        this.contentWidth = width;
        this.bodyHeight = bodySum;
        this.footerHeight = footerSum;
    }

    public static InfoPanel layout(DisplayInfo info, boolean detailed, Font font) {
        return layout(info, detailed, font, List.of());
    }

    public static InfoPanel layout(DisplayInfo info, boolean detailed, Font font, List<PanelButton> buttons) {
        List<Row> body = new ArrayList<>();
        for (InfoElement element : info.elements()) {
            if (element.visibility().isShown(detailed)) {
                addRows(element, detailed, font, body);
            }
        }
        List<Row> footer = new ArrayList<>();
        if (info.notice() != null) {
            footer.add(new TextRow(info.notice(), font));
        }
        footer.add(new TextRow(info.modName(), font));
        return new InfoPanel(info.icon(), info.title(), body, footer, buttons, font);
    }

    private static void addRows(InfoElement element, boolean detailed, Font font, List<Row> rows) {
        switch (element) {
            case InfoElement.Text text -> rows.add(new TextRow(text.text(), font));
            case InfoElement.IconText iconText -> rows.add(new IconTextRow(iconText.icon(), iconText.text(), font));
            case InfoElement.ItemGrid grid -> {
                if (detailed) {
                    rows.add(new GridRow(grid.slots(), grid.columns(), true));
                    return;
                }
                List<ItemStack> merged = merge(grid.slots());
                if (merged.isEmpty()) {
                    rows.add(new TextRow(Component.translatable("camilookup.container.empty").withStyle(ChatFormatting.GRAY), font));
                } else if (merged.size() > SUMMARY_GRID_LIMIT) {
                    rows.add(new GridRow(merged.subList(0, SUMMARY_GRID_LIMIT), 9, false));
                    rows.add(new TextRow(Component.translatable("camilookup.container.more",
                            merged.size() - SUMMARY_GRID_LIMIT).withStyle(ChatFormatting.GRAY), font));
                } else {
                    rows.add(new GridRow(merged, Math.min(9, merged.size()), false));
                }
            }
            case InfoElement.ItemRow row -> {
                List<ItemStack> items = row.items().stream().filter(stack -> !stack.isEmpty()).toList();
                if (!items.isEmpty()) {
                    rows.add(new GridRow(items, items.size(), false));
                }
            }
            case InfoElement.Process process -> rows.add(new ProcessRow(process));
            case InfoElement.Health health -> {
                int hearts = Mth.ceil(health.maxHealth() / 2.0F);
                if (hearts <= MAX_HEARTS && hearts > 0) {
                    rows.add(new HeartsRow(health.health(), hearts));
                } else {
                    rows.add(new TextRow(Component.literal(number(health.health()) + "/" + number(health.maxHealth()) + " ❤")
                            .withStyle(ChatFormatting.RED), font));
                }
            }
        }
    }

    /** Non-empty stacks with equal items combined, for the compact summary grid. */
    private static List<ItemStack> merge(List<ItemStack> slots) {
        List<ItemStack> merged = new ArrayList<>();
        outer:
        for (ItemStack stack : slots) {
            if (stack.isEmpty()) {
                continue;
            }
            for (ItemStack existing : merged) {
                if (ItemStack.isSameItemSameComponents(existing, stack)) {
                    existing.setCount(existing.getCount() + stack.getCount());
                    continue outer;
                }
            }
            merged.add(stack.copy());
        }
        return merged;
    }

    private static String number(float value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.format(Locale.ROOT, "%.1f", value);
    }

    private static String countText(int count) {
        if (count <= 1) {
            return null;
        }
        if (count >= 10_000) {
            return count / 1000 + "k";
        }
        if (count >= 1000) {
            return String.format(Locale.ROOT, "%.1fk", count / 1000.0);
        }
        return String.valueOf(count);
    }

    /** Height of the whole box when the body is limited to {@code maxHeight} in total. */
    public int height(int maxHeight) {
        return headerHeight + gapAfterHeader() + visibleBodyHeight(maxHeight) + gapBeforeFooter() + footerHeight;
    }

    public int width(int maxHeight) {
        return contentWidth + (isScrollable(maxHeight) ? SCROLLBAR : 0);
    }

    public boolean isScrollable(int maxHeight) {
        return isScrollableIn(height(maxHeight));
    }

    public int maxScroll(int maxHeight) {
        return maxScrollIn(height(maxHeight));
    }

    /** Whether the body must scroll when the box is {@code boxHeight} tall. */
    public boolean isScrollableIn(int boxHeight) {
        return bodyIn(boxHeight) < bodyHeight;
    }

    public int maxScrollIn(int boxHeight) {
        return Math.max(0, bodyHeight - bodyIn(boxHeight));
    }

    /** Smallest box that still shows the header, part of the body and the footer. */
    public int minHeight() {
        return fixedHeight() + Math.min(bodyHeight, CELL);
    }

    public int headerHeight() {
        return headerHeight;
    }

    /** The header button at a point, in the same coordinates the box was rendered with. */
    public @Nullable PanelButton buttonAt(double mouseX, double mouseY, int left, int top, int boxWidth) {
        for (int i = 0; i < buttons.size(); i++) {
            int x = buttonX(i, left, boxWidth);
            int y = buttonY(top);
            if (mouseX >= x - 1 && mouseX < x + PanelButton.SIZE + 1 && mouseY >= y - 1 && mouseY < y + PanelButton.SIZE + 1) {
                return buttons.get(i);
            }
        }
        return null;
    }

    private int buttonX(int index, int left, int boxWidth) {
        return left + boxWidth - (buttons.size() - index) * BUTTON_SPACING + 2;
    }

    private int buttonsWidth() {
        return buttons.isEmpty() ? 0 : 4 + buttons.size() * BUTTON_SPACING;
    }

    private int buttonY(int top) {
        return top + (headerHeight - PanelButton.SIZE) / 2;
    }

    private int gapAfterHeader() {
        return 2;
    }

    private int gapBeforeFooter() {
        return body.isEmpty() ? 0 : 2;
    }

    private int fixedHeight() {
        return headerHeight + gapAfterHeader() + gapBeforeFooter() + footerHeight;
    }

    private int visibleBodyHeight(int maxHeight) {
        return Math.max(Math.min(bodyHeight, CELL), Math.min(bodyHeight, maxHeight - fixedHeight()));
    }

    /** Visible body height inside a box of the given height. */
    private int bodyIn(int boxHeight) {
        return Math.max(0, boxHeight - fixedHeight());
    }

    /** Draws the box at its natural size, with the body limited so the whole box fits in {@code maxHeight}. */
    public ItemStack render(GuiGraphics graphics, Font font, int left, int top, int maxHeight, int scroll,
                            double mouseX, double mouseY) {
        return render(graphics, font, left, top, width(maxHeight), height(maxHeight), scroll, mouseX, mouseY);
    }

    /**
     * Draws the box with its content's top-left corner at ({@code left}, {@code top}) and the given content size.
     * Content that does not fit is clipped; the body scrolls vertically.
     *
     * @return the item under the mouse, or an empty stack
     */
    public ItemStack render(GuiGraphics graphics, Font font, int left, int top, int boxWidth, int boxHeight, int scroll,
                            double mouseX, double mouseY) {
        TooltipRenderUtil.renderTooltipBackground(graphics, left, top, boxWidth, boxHeight, null);
        boolean scrollable = isScrollableIn(boxHeight);
        int contentRight = left + boxWidth - (scrollable ? SCROLLBAR : 0);

        graphics.enableScissor(left, top, left + boxWidth - buttonsWidth(), top + headerHeight);
        if (icon.isEmpty()) {
            graphics.drawString(font, title, left, top, -1, true);
        } else {
            graphics.renderItem(icon, left, top);
            graphics.drawString(font, title, left + CELL + 2, top + 4, -1, true);
        }
        graphics.disableScissor();
        PanelButton hoveredButton = buttonAt(mouseX, mouseY, left, top, boxWidth);
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).render(graphics, buttonX(i, left, boxWidth), buttonY(top), buttons.get(i) == hoveredButton);
        }

        ItemStack hovered = ItemStack.EMPTY;
        int y = top + headerHeight + gapAfterHeader();
        int visibleBody = bodyIn(boxHeight);
        if (!body.isEmpty() && visibleBody > 0) {
            int maxScroll = maxScrollIn(boxHeight);
            int clampedScroll = Mth.clamp(scroll, 0, maxScroll);
            boolean mouseInBody = mouseX >= left && mouseX < contentRight && mouseY >= y && mouseY < y + visibleBody;
            graphics.enableScissor(left, y, contentRight, y + visibleBody);
            int rowY = y - clampedScroll;
            for (Row row : body) {
                if (rowY + row.height() > y && rowY < y + visibleBody) {
                    ItemStack rowHovered = row.render(graphics, font, left, rowY,
                            mouseInBody ? mouseX : Double.NaN, mouseInBody ? mouseY : Double.NaN);
                    if (!rowHovered.isEmpty()) {
                        hovered = rowHovered;
                    }
                }
                rowY += row.height();
            }
            graphics.disableScissor();
            if (scrollable) {
                int trackX = left + boxWidth - 2;
                int thumbHeight = Math.max(8, visibleBody * visibleBody / bodyHeight);
                int thumbY = y + (visibleBody - thumbHeight) * clampedScroll / Math.max(1, maxScroll);
                graphics.fill(trackX, y, trackX + 2, y + visibleBody, 0x40FFFFFF);
                graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, 0xC0FFFFFF);
            }
        }

        int footerY = top + boxHeight - footerHeight;
        graphics.enableScissor(left, footerY, contentRight, footerY + footerHeight);
        for (Row row : footer) {
            row.render(graphics, font, left, footerY, Double.NaN, Double.NaN);
            footerY += row.height();
        }
        graphics.disableScissor();
        return hovered;
    }

    private interface Row {
        int width();

        int height();

        ItemStack render(GuiGraphics graphics, Font font, int x, int y, double mouseX, double mouseY);
    }

    private record TextRow(Component text, int width) implements Row {
        TextRow(Component text, Font font) {
            this(text, font.width(text));
        }

        @Override
        public int height() {
            return 10;
        }

        @Override
        public ItemStack render(GuiGraphics graphics, Font font, int x, int y, double mouseX, double mouseY) {
            graphics.drawString(font, text, x, y, -1, true);
            return ItemStack.EMPTY;
        }
    }

    private record IconTextRow(ItemStack icon, Component text, int width) implements Row {
        IconTextRow(ItemStack icon, Component text, Font font) {
            this(icon, text, CELL + 2 + font.width(text));
        }

        @Override
        public int height() {
            return CELL;
        }

        @Override
        public ItemStack render(GuiGraphics graphics, Font font, int x, int y, double mouseX, double mouseY) {
            graphics.renderItem(icon, x, y + 1);
            graphics.drawString(font, text, x + CELL + 2, y + 5, -1, true);
            boolean hovered = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
            return hovered ? icon : ItemStack.EMPTY;
        }
    }

    private record GridRow(List<ItemStack> stacks, int columns, boolean showSlots) implements Row {
        @Override
        public int width() {
            return columns * CELL;
        }

        @Override
        public int height() {
            return Mth.positiveCeilDiv(stacks.size(), columns) * CELL;
        }

        @Override
        public ItemStack render(GuiGraphics graphics, Font font, int x, int y, double mouseX, double mouseY) {
            ItemStack hovered = ItemStack.EMPTY;
            for (int i = 0; i < stacks.size(); i++) {
                int cellX = x + i % columns * CELL;
                int cellY = y + i / columns * CELL;
                if (showSlots) {
                    graphics.fill(cellX, cellY, cellX + CELL - 1, cellY + CELL - 1, SLOT_COLOR);
                }
                ItemStack stack = stacks.get(i);
                if (stack.isEmpty()) {
                    continue;
                }
                graphics.renderItem(stack, cellX, cellY);
                graphics.renderItemDecorations(font, stack, cellX, cellY, countText(stack.getCount()));
                if (mouseX >= cellX && mouseX < cellX + CELL && mouseY >= cellY && mouseY < cellY + CELL) {
                    hovered = stack;
                }
            }
            return hovered;
        }
    }

    private record HeartsRow(float health, int hearts) implements Row {
        private static final int PER_ROW = 10;

        @Override
        public int width() {
            return Math.min(hearts, PER_ROW) * 8 + 1;
        }

        @Override
        public int height() {
            return Mth.positiveCeilDiv(hearts, PER_ROW) * 10;
        }

        @Override
        public ItemStack render(GuiGraphics graphics, Font font, int x, int y, double mouseX, double mouseY) {
            for (int i = 0; i < hearts; i++) {
                int heartX = x + i % PER_ROW * 8;
                int heartY = y + i / PER_ROW * 10;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER, heartX, heartY, 9, 9);
                if (health >= (i + 1) * 2) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_FULL, heartX, heartY, 9, 9);
                } else if (health > i * 2) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_HALF, heartX, heartY, 9, 9);
                }
            }
            return ItemStack.EMPTY;
        }
    }

    /** Draws an item slot with its background and returns the stack if the mouse is over it. */
    private static ItemStack renderSlot(GuiGraphics graphics, Font font, ItemStack stack, int x, int y, double mouseX, double mouseY) {
        graphics.fill(x, y, x + CELL - 1, y + CELL - 1, SLOT_COLOR);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y, countText(stack.getCount()));
        return mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL ? stack : ItemStack.EMPTY;
    }

    /** A furnace-like layout: inputs over the flame and fuel, then the progress arrow and the outputs. */
    private record ProcessRow(InfoElement.Process process) implements Row {
        private static final int ARROW_WIDTH = 24;
        private static final int ARROW_HEIGHT = 16;
        private static final int FLAME_SIZE = 14;

        private int arrowX() {
            return Math.max(1, process.inputs().size()) * CELL + 4;
        }

        private int arrowY() {
            return process.hasFuel() ? CELL - 1 : 1;
        }

        private int outputsX() {
            return arrowX() + ARROW_WIDTH + 4;
        }

        @Override
        public int width() {
            return outputsX() + Math.max(1, process.outputs().size()) * CELL;
        }

        @Override
        public int height() {
            return process.hasFuel() ? CELL * 3 - 2 : CELL;
        }

        @Override
        public ItemStack render(GuiGraphics graphics, Font font, int x, int y, double mouseX, double mouseY) {
            ItemStack hovered = ItemStack.EMPTY;
            for (int i = 0; i < process.inputs().size(); i++) {
                hovered = orElse(hovered, renderSlot(graphics, font, process.inputs().get(i), x + i * CELL, y, mouseX, mouseY));
            }
            if (process.hasFuel()) {
                int flameX = x + 2;
                int flameY = y + CELL;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT_PROGRESS, flameX, flameY, FLAME_SIZE, FLAME_SIZE, EMPTY_TINT);
                if (process.fuelLevel() > 0) {
                    int lit = Mth.ceil(process.fuelLevel() * (FLAME_SIZE - 1)) + 1;
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT_PROGRESS, FLAME_SIZE, FLAME_SIZE, 0, FLAME_SIZE - lit,
                            flameX, flameY + FLAME_SIZE - lit, FLAME_SIZE, lit);
                }
                hovered = orElse(hovered, renderSlot(graphics, font, process.fuel(), x, y + CELL * 2 - 2, mouseX, mouseY));
            }

            int arrowX = x + arrowX();
            int arrowY = y + arrowY();
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BURN_PROGRESS, arrowX, arrowY, ARROW_WIDTH, ARROW_HEIGHT, EMPTY_TINT);
            int filled = Mth.ceil(process.progress() * ARROW_WIDTH);
            if (filled > 0) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BURN_PROGRESS, ARROW_WIDTH, ARROW_HEIGHT, 0, 0,
                        arrowX, arrowY, filled, ARROW_HEIGHT);
            }

            for (int i = 0; i < process.outputs().size(); i++) {
                hovered = orElse(hovered, renderSlot(graphics, font, process.outputs().get(i),
                        x + outputsX() + i * CELL, arrowY - 1, mouseX, mouseY));
            }
            return hovered;
        }

        private static ItemStack orElse(ItemStack current, ItemStack candidate) {
            return candidate.isEmpty() ? current : candidate;
        }
    }
}
