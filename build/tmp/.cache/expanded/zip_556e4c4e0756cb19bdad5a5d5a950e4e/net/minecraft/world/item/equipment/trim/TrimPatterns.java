package net.minecraft.world.item.equipment.trim;

import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class TrimPatterns {
    public static final ResourceKey<TrimPattern> SENTRY = registryKey("sentry");
    public static final ResourceKey<TrimPattern> DUNE = registryKey("dune");
    public static final ResourceKey<TrimPattern> COAST = registryKey("coast");
    public static final ResourceKey<TrimPattern> WILD = registryKey("wild");
    public static final ResourceKey<TrimPattern> WARD = registryKey("ward");
    public static final ResourceKey<TrimPattern> EYE = registryKey("eye");
    public static final ResourceKey<TrimPattern> VEX = registryKey("vex");
    public static final ResourceKey<TrimPattern> TIDE = registryKey("tide");
    public static final ResourceKey<TrimPattern> SNOUT = registryKey("snout");
    public static final ResourceKey<TrimPattern> RIB = registryKey("rib");
    public static final ResourceKey<TrimPattern> SPIRE = registryKey("spire");
    public static final ResourceKey<TrimPattern> WAYFINDER = registryKey("wayfinder");
    public static final ResourceKey<TrimPattern> SHAPER = registryKey("shaper");
    public static final ResourceKey<TrimPattern> SILENCE = registryKey("silence");
    public static final ResourceKey<TrimPattern> RAISER = registryKey("raiser");
    public static final ResourceKey<TrimPattern> HOST = registryKey("host");
    public static final ResourceKey<TrimPattern> FLOW = registryKey("flow");
    public static final ResourceKey<TrimPattern> BOLT = registryKey("bolt");

    public static void bootstrap(BootstrapContext<TrimPattern> pContext) {
        register(pContext, SENTRY);
        register(pContext, DUNE);
        register(pContext, COAST);
        register(pContext, WILD);
        register(pContext, WARD);
        register(pContext, EYE);
        register(pContext, VEX);
        register(pContext, TIDE);
        register(pContext, SNOUT);
        register(pContext, RIB);
        register(pContext, SPIRE);
        register(pContext, WAYFINDER);
        register(pContext, SHAPER);
        register(pContext, SILENCE);
        register(pContext, RAISER);
        register(pContext, HOST);
        register(pContext, FLOW);
        register(pContext, BOLT);
    }

    public static void register(BootstrapContext<TrimPattern> pContext, ResourceKey<TrimPattern> pKey) {
        TrimPattern trimpattern = new TrimPattern(defaultAssetId(pKey), Component.translatable(Util.makeDescriptionId("trim_pattern", pKey.location())), false);
        pContext.register(pKey, trimpattern);
    }

    private static ResourceKey<TrimPattern> registryKey(String pName) {
        return ResourceKey.create(Registries.TRIM_PATTERN, ResourceLocation.withDefaultNamespace(pName));
    }

    public static ResourceLocation defaultAssetId(ResourceKey<TrimPattern> pKey) {
        return pKey.location();
    }
}