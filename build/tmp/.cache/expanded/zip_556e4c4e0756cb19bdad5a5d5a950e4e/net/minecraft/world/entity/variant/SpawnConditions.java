package net.minecraft.world.entity.variant;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;

public class SpawnConditions {
    public static MapCodec<? extends SpawnCondition> bootstrap(Registry<MapCodec<? extends SpawnCondition>> pRegistry) {
        Registry.register(pRegistry, "structure", StructureCheck.MAP_CODEC);
        Registry.register(pRegistry, "moon_brightness", MoonBrightnessCheck.MAP_CODEC);
        return Registry.register(pRegistry, "biome", BiomeCheck.MAP_CODEC);
    }
}