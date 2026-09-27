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
        registrar.registerBlockProvider(ProviderSide.CLIENT, id("redstone"), InfoCategories.REDSTONE, 150, RedstoneProvider::appendClient);
        registrar.registerBlockProvider(ProviderSide.SERVER, id("redstone_server"), InfoCategories.REDSTONE, 151, RedstoneProvider::appendServer);
        registrar.registerBlockProvider(ProviderSide.CLIENT, id("farm"), InfoCategories.FARM, 150, FarmProvider::appendClient);
        registrar.registerBlockProvider(ProviderSide.SERVER, id("farm_server"), InfoCategories.FARM, 151, FarmProvider::appendServer);
        registrar.registerBlockProvider(ProviderSide.CLIENT, id("utility"), InfoCategories.UTILITY, 150, UtilityProvider::appendClient);
        registrar.registerBlockProvider(ProviderSide.SERVER, id("utility_server"), InfoCategories.UTILITY, 151, UtilityProvider::appendServer);
        registrar.registerBlockProvider(ProviderSide.SERVER, id("processing"), InfoCategories.PROCESSING, 400, ProcessingProvider::appendServer);
        registrar.registerBlockProvider(ProviderSide.CLIENT, id("advanced"), InfoCategories.ADVANCED, 150, AdvancedProvider::appendClient);
        registrar.registerBlockProvider(ProviderSide.SERVER, id("advanced_server"), InfoCategories.ADVANCED, 151, AdvancedProvider::appendServer);
        registrar.registerBlockProvider(ProviderSide.CLIENT, id("sculk"), InfoCategories.REDSTONE, 150, OtherProvider::appendClient);
        registrar.registerBlockProvider(ProviderSide.SERVER, id("sculk_server"), InfoCategories.REDSTONE, 151, OtherProvider::appendSculk);
        registrar.registerBlockProvider(ProviderSide.SERVER, id("suspicious_block"), InfoCategories.SUSPICIOUS_BLOCK, 150, OtherProvider::appendSuspicious);
        registrar.registerBlockProvider(ProviderSide.SERVER, id("container"), InfoCategories.CONTAINER, 400, ContainerProviders::appendBlock);
        registrar.registerBlockProvider(ProviderSide.SERVER, id("furnace"), InfoCategories.PROCESSING, 400, new FurnaceProvider());
        registrar.registerBlockProvider(ProviderSide.CLIENT, id("block_technical"), InfoCategories.TECHNICAL, 1000, new BlockTechnicalProvider());

        registrar.registerEntityProvider(ProviderSide.CLIENT, id("entity_basic"), InfoCategories.BASIC, 0, EntityProviders::appendBasic);
        registrar.registerEntityProvider(ProviderSide.CLIENT, id("item_entity"), InfoCategories.BASIC, 1, EntityProviders::appendItemEntity);
        registrar.registerEntityProvider(ProviderSide.CLIENT, id("living"), InfoCategories.ENTITY, 100, EntityProviders::appendLiving);
        registrar.registerEntityProvider(ProviderSide.CLIENT, id("item_frame"), InfoCategories.ENTITY, 100, EntityProviders::appendItemFrame);
        registrar.registerEntityProvider(ProviderSide.CLIENT, id("special_entity"), InfoCategories.ENTITY, 150, SpecialEntityProvider::appendClient);
        registrar.registerEntityProvider(ProviderSide.SERVER, id("special_entity_server"), InfoCategories.ENTITY, 250, SpecialEntityProvider::appendServer);
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
