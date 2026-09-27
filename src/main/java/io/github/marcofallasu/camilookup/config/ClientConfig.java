package io.github.marcofallasu.camilookup.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public enum ActivationMode {
        /** Cursor mode is active while the key is held. */
        HOLD,
        /** Each key press turns cursor mode on or off. */
        TOGGLE
    }

    public enum PinAnchor {
        /** The pinned box follows its target and updates live. */
        TARGET,
        /** The pinned box stays where it was pinned, as a snapshot. */
        SCREEN
    }

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.EnumValue<ActivationMode> ACTIVATION_MODE;
    private static final ForgeConfigSpec.DoubleValue INSPECT_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue SHOW_TECHNICAL;
    private static final ForgeConfigSpec.EnumValue<PinAnchor> PIN_ANCHOR;

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
        PIN_ANCHOR = builder
                .comment("Default anchor of pinned boxes. Hold CTRL while pinning to use the other one.",
                        "TARGET: follows the block or entity and updates live. SCREEN: stays in place as a snapshot.")
                .defineEnum("pinAnchor", PinAnchor.TARGET);
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

    public static PinAnchor pinAnchor() {
        return SPEC.isLoaded() ? PIN_ANCHOR.get() : PIN_ANCHOR.getDefault();
    }
}
