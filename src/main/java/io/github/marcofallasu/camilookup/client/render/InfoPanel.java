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
        return visibleBodyHeight(maxHeight) < bodyHeight;
    }

    public int maxScroll(int maxHeight) {
        return Math.max(0, bodyHeight - visibleBodyHeight(maxHeight));
    }

    public int headerHeight() {
        return headerHeight;
    }

    /** The header button at a point, in the same coordinates the box was rendered with. */
    public @Nullable PanelButton buttonAt(double mouseX, double mouseY, int left, int top, int maxHeight) {
        for (int i = 0; i < buttons.size(); i++) {
            int x = buttonX(i, left, maxHeight);
            int y = buttonY(top);
            if (mouseX >= x - 1 && mouseX < x + PanelButton.SIZE + 1 && mouseY >= y - 1 && mouseY < y + PanelButton.SIZE + 1) {
                return buttons.get(i);
            }
        }
        return null;
    }

    private int buttonX(int index, int left, int maxHeight) {
        return left + width(maxHeight) - (buttons.size() - index) * BUTTON_SPACING + 2;
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

    private int visibleBodyHeight(int maxHeight) {
        int fixed = headerHeight + gapAfterHeader() + gapBeforeFooter() + footerHeight;
        return Math.max(Math.min(bodyHeight, CELL), Math.min(bodyHeight, maxHeight - fixed));
    }

    /**
     * Draws the box with its content's top-left corner at ({@code left}, {@code top}).
     *
     * @return the item under the mouse, or an empty stack
     */
    public ItemStack render(GuiGraphics graphics, Font font, int left, int top, int maxHeight, int scroll,
                            double mouseX, double mouseY) {
        int width = width(maxHeight);
        int height = height(maxHeight);
        TooltipRenderUtil.renderTooltipBackground(graphics, left, top, width, height, null);

        int y = top;
        if (icon.isEmpty()) {
            graphics.drawString(font, title, left, y, -1, true);
        } else {
            graphics.renderItem(icon, left, y);
            graphics.drawString(font, title, left + CELL + 2, y + 4, -1, true);
        }
        PanelButton hoveredButton = buttonAt(mouseX, mouseY, left, top, maxHeight);
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).render(graphics, buttonX(i, left, maxHeight), buttonY(top), buttons.get(i) == hoveredButton);
        }
        y += headerHeight + gapAfterHeader();

        ItemStack hovered = ItemStack.EMPTY;
        int visibleBody = visibleBodyHeight(maxHeight);
        if (!body.isEmpty()) {
            int clampedScroll = Mth.clamp(scroll, 0, maxScroll(maxHeight));
            boolean mouseInBody = mouseX >= left && mouseX < left + contentWidth && mouseY >= y && mouseY < y + visibleBody;
            graphics.enableScissor(left, y, left + width, y + visibleBody);
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
            if (isScrollable(maxHeight)) {
                int trackX = left + width - 2;
                int thumbHeight = Math.max(8, visibleBody * visibleBody / bodyHeight);
                int thumbY = y + (visibleBody - thumbHeight) * clampedScroll / maxScroll(maxHeight);
                graphics.fill(trackX, y, trackX + 2, y + visibleBody, 0x40FFFFFF);
                graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, 0xC0FFFFFF);
            }
            y += visibleBody + gapBeforeFooter();
        }

        for (Row row : footer) {
            row.render(graphics, font, left, y, Double.NaN, Double.NaN);
            y += row.height();
        }
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
                    graphics.fill(cellX, cellY, cellX + CELL - 1, cellY + CELL - 1, 0x30FFFFFF);
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
}
