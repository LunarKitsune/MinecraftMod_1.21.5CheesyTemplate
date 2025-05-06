package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class CatTypeFix extends NamedEntityFix {
    public CatTypeFix(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType, "CatTypeFix", References.ENTITY, "minecraft:cat");
    }

    public Dynamic<?> fixTag(Dynamic<?> pDynamic) {
        return pDynamic.get("CatType").asInt(0) == 9 ? pDynamic.set("CatType", pDynamic.createInt(10)) : pDynamic;
    }

    @Override
    protected Typed<?> fix(Typed<?> p_15010_) {
        return p_15010_.update(DSL.remainderFinder(), this::fixTag);
    }
}