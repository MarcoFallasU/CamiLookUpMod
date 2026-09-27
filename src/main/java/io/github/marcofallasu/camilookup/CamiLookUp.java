package io.github.marcofallasu.camilookup;

import io.github.marcofallasu.camilookup.client.CamiLookUpClient;
import io.github.marcofallasu.camilookup.config.ServerConfig;
import io.github.marcofallasu.camilookup.core.LookUpRegistry;
import io.github.marcofallasu.camilookup.network.CamiNetwork;
import io.github.marcofallasu.camilookup.server.ServerInfoHandler;
import io.github.marcofallasu.camilookup.vanilla.VanillaPlugin;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(CamiLookUp.MODID)
public final class CamiLookUp {
    public static final String MODID = "camilookup";

    public CamiLookUp(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);

        CamiNetwork.init();
        ServerInfoHandler.init();
        VanillaPlugin.init();
        FMLCommonSetupEvent.getBus(context.getModBusGroup())
                .addListener(event -> event.enqueueWork(LookUpRegistry::load));

        if (FMLEnvironment.dist.isClient()) {
            CamiLookUpClient.init(context);
        }
    }
}
