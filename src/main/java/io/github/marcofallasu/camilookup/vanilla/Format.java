package io.github.marcofallasu.camilookup.vanilla;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/** Small formatting helpers for the built-in providers. */
final class Format {
    private Format() {
    }

    /** "Label: value" with a gray label, as used across all boxes. */
    static MutableComponent labeled(String labelKey, Component value) {
        return Component.translatable(labelKey).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(value.copy().withStyle(ChatFormatting.WHITE));
    }

    static MutableComponent labeled(String labelKey, String value) {
        return labeled(labelKey, Component.literal(value));
    }

    static Component yesNo(boolean value) {
        return Component.translatable(value ? "camilookup.yes" : "camilookup.no")
                .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    static String number(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    /** Formats game ticks as m:ss (or h:mm:ss). */
    static String duration(long ticks) {
        long totalSeconds = Math.max(0, ticks) / 20;
        long hours = totalSeconds / 3600;
        long minutes = totalSeconds / 60 % 60;
        long seconds = totalSeconds % 60;
        return hours > 0
                ? String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
                : String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }
}
