package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class EntityWolfColorFix extends NamedEntityFix {
    public EntityWolfColorFix(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType, "EntityWolfColorFix", References.ENTITY, "minecraft:wolf");
    }

    public Dynamic<?> fixTag(Dynamic<?> pTag) {
        return pTag.update("CollarColor", p_15796_ -> p_15796_.createByte((byte)(15 - p_15796_.asInt(0))));
    }

    @Override
    protected Typed<?> fix(Typed<?> p_15792_) {
        return p_15792_.update(DSL.remainderFinder(), this::fixTag);
    }
}