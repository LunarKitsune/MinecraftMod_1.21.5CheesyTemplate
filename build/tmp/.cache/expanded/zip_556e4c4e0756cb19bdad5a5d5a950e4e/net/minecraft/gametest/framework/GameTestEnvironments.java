package net.minecraft.gametest.framework;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public interface GameTestEnvironments {
    String DEFAULT = "default";
    ResourceKey<TestEnvironmentDefinition> DEFAULT_KEY = create("default");

    private static ResourceKey<TestEnvironmentDefinition> create(String pName) {
        return ResourceKey.create(Registries.TEST_ENVIRONMENT, ResourceLocation.withDefaultNamespace(pName));
    }

    static void bootstrap(BootstrapContext<TestEnvironmentDefinition> pContext) {
        pContext.register(DEFAULT_KEY, new TestEnvironmentDefinition.AllOf(List.of()));
    }
}