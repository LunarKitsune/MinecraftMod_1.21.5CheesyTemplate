package net.minecraft.gametest.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public abstract class TestFunctionLoader {
    private static final List<TestFunctionLoader> loaders = new ArrayList<>();

    public static void registerLoader(TestFunctionLoader pLoader) {
        loaders.add(pLoader);
    }

    public static void runLoaders(Registry<Consumer<GameTestHelper>> pRegistry) {
        for (TestFunctionLoader testfunctionloader : loaders) {
            testfunctionloader.load((p_396342_, p_395649_) -> Registry.register(pRegistry, p_396342_, p_395649_));
        }
    }

    public abstract void load(BiConsumer<ResourceKey<Consumer<GameTestHelper>>, Consumer<GameTestHelper>> pLoader);
}