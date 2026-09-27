package io.github.marcofallasu.camilookup.api;

import net.minecraft.resources.Identifier;

/** Categories used by the built-in providers. Servers can disable any category in their configuration. */
public final class InfoCategories {
    /** Name, icon, mod and block state properties. */
    public static final Identifier BASIC = id("basic");
    /** Hardness, tool, blast resistance, light, biome and coordinates. */
    public static final Identifier TECHNICAL = id("technical");
    /** Contents of containers. */
    public static final Identifier CONTAINER = id("container");
    /** Redstone components: delays, modes, signal strength. */
    public static final Identifier REDSTONE = id("redstone");
    /** Crops, saplings, beehives, composters and eggs. */
    public static final Identifier FARM = id("farm");
    /** Lecterns, signs, jukeboxes, note blocks, bookshelves, beacons, cauldrons, end portal frames. */
    public static final Identifier UTILITY = id("utility");
    /** Crafters, trial spawners, vaults, decorated pots and respawn anchors. */
    public static final Identifier ADVANCED = id("advanced");
    /** The hidden item of suspicious sand and gravel. Disabled by default on servers. */
    public static final Identifier SUSPICIOUS_BLOCK = id("suspicious_block");
    /** Furnaces and other machines that process items. */
    public static final Identifier PROCESSING = id("processing");
    /** Health, armor, equipment and other entity information. */
    public static final Identifier ENTITY = id("entity");
    /** Active status effects of entities. */
    public static final Identifier EFFECTS = id("effects");
    /** Information about other players beyond what is visible. */
    public static final Identifier PLAYER_DETAILS = id("player_details");

    private InfoCategories() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("camilookup", path);
    }
}
