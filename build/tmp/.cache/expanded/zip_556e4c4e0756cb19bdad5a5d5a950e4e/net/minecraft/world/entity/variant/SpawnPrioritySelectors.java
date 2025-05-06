package net.minecraft.world.entity.variant;

import com.mojang.serialization.Codec;
import java.util.List;

public record SpawnPrioritySelectors(List<PriorityProvider.Selector<SpawnContext, SpawnCondition>> selectors) {
    public static final SpawnPrioritySelectors EMPTY = new SpawnPrioritySelectors(List.of());
    public static final Codec<SpawnPrioritySelectors> CODEC = PriorityProvider.Selector.<SpawnContext, SpawnCondition>codec(SpawnCondition.CODEC)
        .listOf()
        .xmap(SpawnPrioritySelectors::new, SpawnPrioritySelectors::selectors);

    public static SpawnPrioritySelectors single(SpawnCondition pCondition, int pPriority) {
        return new SpawnPrioritySelectors(PriorityProvider.single(pCondition, pPriority));
    }

    public static SpawnPrioritySelectors fallback(int pPriority) {
        return new SpawnPrioritySelectors(PriorityProvider.alwaysTrue(pPriority));
    }
}