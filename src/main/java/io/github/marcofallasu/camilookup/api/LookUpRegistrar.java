package io.github.marcofallasu.camilookup.api;

import io.github.marcofallasu.camilookup.api.provider.IBlockInfoProvider;
import io.github.marcofallasu.camilookup.api.provider.IEntityInfoProvider;
import io.github.marcofallasu.camilookup.api.provider.ProviderSide;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;

/**
 * Registration entry point handed out by {@link RegisterLookUpEvent}.
 * <p>
 * Lower priorities are shown first. The built-in providers use 0 (name and icon) to 1000; the default is 500.
 * Client providers and pin actions are ignored on a dedicated server; guard client-only code with {@link #dist()}.
 */
public interface LookUpRegistrar {
    int DEFAULT_PRIORITY = 500;

    /** The physical side the game is running on. */
    Dist dist();

    void registerBlockProvider(ProviderSide side, Identifier id, Identifier category, int priority, IBlockInfoProvider provider);

    void registerEntityProvider(ProviderSide side, Identifier id, Identifier category, int priority, IEntityInfoProvider provider);

    void registerPinAction(Identifier id, int priority, IPinAction action);

    void registerRestrictionRule(Identifier id, IRestrictionRule rule);

    default void registerBlockProvider(ProviderSide side, Identifier id, Identifier category, IBlockInfoProvider provider) {
        registerBlockProvider(side, id, category, DEFAULT_PRIORITY, provider);
    }

    default void registerEntityProvider(ProviderSide side, Identifier id, Identifier category, IEntityInfoProvider provider) {
        registerEntityProvider(side, id, category, DEFAULT_PRIORITY, provider);
    }
}
