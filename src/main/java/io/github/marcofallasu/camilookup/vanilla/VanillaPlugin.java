package io.github.marcofallasu.camilookup.vanilla;

import io.github.marcofallasu.camilookup.api.InfoCategories;
import io.github.marcofallasu.camilookup.api.LookUpRegistrar;
import io.github.marcofallasu.camilookup.api.RegisterLookUpEvent;
import io.github.marcofallasu.camilookup.api.provider.ProviderSide;
import net.minecraft.resources.Identifier;

/** Registers Cami LookUp's own information through the public API. */
public final class VanillaPlugin {
    private VanillaPlugin() {
    }

    public static void init() {
        RegisterLookUpEvent.BUS.addListener(event -> register(event.registrar()));
    }

    private static void register(LookUpRegistrar registrar) {
        registrar.registerBlockProvider(ProviderSide.CLIENT, id("block_basic"), InfoCategories.BASIC, 0, new BlockBasicProvider());
        registrar.registerBlockProvider(ProviderSide.SERVER, id("container"), InfoCategories.CONTAINER, 400, ContainerProviders::appendBlock);
        registrar.registerBlockProvider(ProviderSide.SERVER, id("furnace"), InfoCategories.PROCESSING, 400, new FurnaceProvider());
        registrar.registerBlockProvider(ProviderSide.CLIENT, id("block_technical"), InfoCategories.TECHNICAL, 1000, new BlockTechnicalProvider());

        registrar.registerEntityProvider(ProviderSide.CLIENT, id("entity_basic"), InfoCategories.BASIC, 0, EntityProviders::appendBasic);
        registrar.registerEntityProvider(ProviderSide.CLIENT, id("item_entity"), InfoCategories.BASIC, 1, EntityProviders::appendItemEntity);
        registrar.registerEntityProvider(ProviderSide.CLIENT, id("living"), InfoCategories.ENTITY, 100, EntityProviders::appendLiving);
        registrar.registerEntityProvider(ProviderSide.CLIENT, id("item_frame"), InfoCategories.ENTITY, 100, EntityProviders::appendItemFrame);
        registrar.registerEntityProvider(ProviderSide.SERVER, id("effects"), InfoCategories.EFFECTS, 200, EntityProviders::appendEffects);
        registrar.registerEntityProvider(ProviderSide.SERVER, id("player_effects"), InfoCategories.PLAYER_DETAILS, 200, EntityProviders::appendPlayerEffects);
        registrar.registerEntityProvider(ProviderSide.SERVER, id("age"), InfoCategories.ENTITY, 300, EntityProviders::appendAge);
        registrar.registerEntityProvider(ProviderSide.SERVER, id("item_despawn"), InfoCategories.ENTITY, 300, EntityProviders::appendItemDespawn);
        registrar.registerEntityProvider(ProviderSide.SERVER, id("entity_container"), InfoCategories.CONTAINER, 400, ContainerProviders::appendEntity);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("camilookup", path);
    }
}
