package io.github.marcofallasu.camilookup.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public enum ActivationMode {
        /** Cursor mode is active while the key is held. */
        HOLD,
        /** Each key press turns cursor mode on or off. */
        TOGGLE
    }

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.EnumValue<ActivationMode> ACTIVATION_MODE;
    private static final ForgeConfigSpec.DoubleValue INSPECT_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue SHOW_TECHNICAL;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ACTIVATION_MODE = builder
                .comment("HOLD: cursor mode while the key is held. TOGGLE: press once to enter, again to leave.")
                .defineEnum("activationMode", ActivationMode.HOLD);
        INSPECT_DISTANCE = builder
                .comment("Inspection distance in blocks. The server may impose a lower maximum.")
                .defineInRange("inspectDistance", 32.0, 1.0, 256.0);
        SHOW_TECHNICAL = builder
                .comment("Show technical block information: hardness, tool, blast resistance, light, biome and coordinates.")
                .define("showTechnical", false);
        SPEC = builder.build();
    }

    private ClientConfig() {
    }

    public static ActivationMode activationMode() {
        return SPEC.isLoaded() ? ACTIVATION_MODE.get() : ACTIVATION_MODE.getDefault();
    }

    public static double inspectDistance() {
        return SPEC.isLoaded() ? INSPECT_DISTANCE.get() : INSPECT_DISTANCE.getDefault();
    }

    public static boolean showTechnical() {
        return SPEC.isLoaded() ? SHOW_TECHNICAL.get() : SHOW_TECHNICAL.getDefault();
    }
}
