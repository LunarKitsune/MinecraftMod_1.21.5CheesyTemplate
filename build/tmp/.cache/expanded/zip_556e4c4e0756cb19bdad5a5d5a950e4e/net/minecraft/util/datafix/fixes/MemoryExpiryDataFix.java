package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;

public class MemoryExpiryDataFix extends NamedEntityFix {
    public MemoryExpiryDataFix(Schema pOutputSchema, String pEntityName) {
        super(pOutputSchema, false, "Memory expiry data fix (" + pEntityName + ")", References.ENTITY, pEntityName);
    }

    @Override
    protected Typed<?> fix(Typed<?> p_16408_) {
        return p_16408_.update(DSL.remainderFinder(), this::fixTag);
    }

    public Dynamic<?> fixTag(Dynamic<?> pTag) {
        return pTag.update("Brain", this::updateBrain);
    }

    private Dynamic<?> updateBrain(Dynamic<?> pBrainTag) {
        return pBrainTag.update("memories", this::updateMemories);
    }

    private Dynamic<?> updateMemories(Dynamic<?> pMemoriesTag) {
        return pMemoriesTag.updateMapValues(this::updateMemoryEntry);
    }

    private Pair<Dynamic<?>, Dynamic<?>> updateMemoryEntry(Pair<Dynamic<?>, Dynamic<?>> pMemory) {
        return pMemory.mapSecond(this::wrapMemoryValue);
    }

    private Dynamic<?> wrapMemoryValue(Dynamic<?> pMemory) {
        return pMemory.createMap(ImmutableMap.of(pMemory.createString("value"), pMemory));
    }
}