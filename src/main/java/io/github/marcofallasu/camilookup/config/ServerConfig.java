package io.github.marcofallasu.camilookup.config;

import io.github.marcofallasu.camilookup.api.InfoCategories;
import net.minecraft.resources.Identifier;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Objects;

public final class ServerConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.DoubleValue MAX_DISTANCE;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_CATEGORIES;
    private static final ForgeConfigSpec.BooleanValue PLAYER_DETAILS;
    private static final ForgeConfigSpec.IntValue MAX_REQUESTS_PER_SECOND;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        MAX_DISTANCE = builder
                .comment("Maximum inspection distance, in blocks, measured from the player's eyes.")
                .defineInRange("maxDistance", 32.0, 1.0, 256.0);
        DISABLED_CATEGORIES = builder
                .comment("Information categories the server does not share, e.g. [\"camilookup:container\"].",
                        "Built-in categories: " + String.join(", ", List.of(
                                InfoCategories.BASIC.toString(), InfoCategories.TECHNICAL.toString(),
                                InfoCategories.CONTAINER.toString(), InfoCategories.PROCESSING.toString(),
                                InfoCategories.ENTITY.toString(),
                                InfoCategories.EFFECTS.toString(), InfoCategories.PLAYER_DETAILS.toString())))
                .defineListAllowEmpty("disabledCategories", List.<String>of(),
                        value -> value instanceof String s && Identifier.tryParse(s) != null);
        PLAYER_DETAILS = builder
                .comment("Share information about other players beyond what is visible (e.g. status effects).")
                .define("playerDetails", false);
        MAX_REQUESTS_PER_SECOND = builder
                .comment("Maximum information requests a player may send per second.")
                .defineInRange("maxRequestsPerSecond", 40, 1, 1000);
        SPEC = builder.build();
    }

    private ServerConfig() {
    }

    public static double maxDistance() {
        return SPEC.isLoaded() ? MAX_DISTANCE.get() : MAX_DISTANCE.getDefault();
    }

    public static List<Identifier> disabledCategories() {
        List<? extends String> values = SPEC.isLoaded() ? DISABLED_CATEGORIES.get() : List.of();
        return values.stream().map(Identifier::tryParse).filter(Objects::nonNull).toList();
    }

    public static boolean playerDetails() {
        return SPEC.isLoaded() ? PLAYER_DETAILS.get() : PLAYER_DETAILS.getDefault();
    }

    public static int maxRequestsPerSecond() {
        return SPEC.isLoaded() ? MAX_REQUESTS_PER_SECOND.get() : MAX_REQUESTS_PER_SECOND.getDefault();
    }
}
