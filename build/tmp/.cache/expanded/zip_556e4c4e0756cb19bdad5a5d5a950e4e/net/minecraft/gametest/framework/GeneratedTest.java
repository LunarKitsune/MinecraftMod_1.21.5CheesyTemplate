package net.minecraft.gametest.framework;

import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public record GeneratedTest(
    Map<ResourceLocation, TestData<ResourceKey<TestEnvironmentDefinition>>> tests,
    ResourceKey<Consumer<GameTestHelper>> functionKey,
    Consumer<GameTestHelper> function
) {
    public GeneratedTest(
        Map<ResourceLocation, TestData<ResourceKey<TestEnvironmentDefinition>>> pTests, ResourceLocation pFunctionKey, Consumer<GameTestHelper> pFunction
    ) {
        this(pTests, ResourceKey.create(Registries.TEST_FUNCTION, pFunctionKey), pFunction);
    }

    public GeneratedTest(ResourceLocation pFunctionKey, TestData<ResourceKey<TestEnvironmentDefinition>> pTestData, Consumer<GameTestHelper> pFunction) {
        this(Map.of(pFunctionKey, pTestData), pFunctionKey, pFunction);
    }
}