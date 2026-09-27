package io.github.marcofallasu.camilookup.core;

import net.minecraftforge.fml.ModList;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Resolves a registry namespace to the display name of the mod that owns it. */
public final class ModNames {
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private ModNames() {
    }

    public static String of(String namespace) {
        return CACHE.computeIfAbsent(namespace, ns -> {
            if (ns.equals("minecraft")) {
                return "Minecraft";
            }
            return ModList.get().getModContainerById(ns)
                    .map(container -> container.getModInfo().getDisplayName())
                    .orElse(ns);
        });
    }
}
