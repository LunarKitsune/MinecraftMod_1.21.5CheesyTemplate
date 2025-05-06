package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class StriderGravityFix extends NamedEntityFix {
    public StriderGravityFix(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType, "StriderGravityFix", References.ENTITY, "minecraft:strider");
    }

    public Dynamic<?> fixTag(Dynamic<?> pTag) {
        return pTag.get("NoGravity").asBoolean(false) ? pTag.set("NoGravity", pTag.createBoolean(false)) : pTag;
    }

    @Override
    protected Typed<?> fix(Typed<?> p_16957_) {
        return p_16957_.update(DSL.remainderFinder(), this::fixTag);
    }
}