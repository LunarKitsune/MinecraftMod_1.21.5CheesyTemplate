package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.List;

public class EntityShulkerRotationFix extends NamedEntityFix {
    public EntityShulkerRotationFix(Schema pOutputSchema) {
        super(pOutputSchema, false, "EntityShulkerRotationFix", References.ENTITY, "minecraft:shulker");
    }

    public Dynamic<?> fixTag(Dynamic<?> pTag) {
        List<Double> list = pTag.get("Rotation").asList(p_15686_ -> p_15686_.asDouble(180.0));
        if (!list.isEmpty()) {
            list.set(0, list.get(0) - 180.0);
            return pTag.set("Rotation", pTag.createList(list.stream().map(pTag::createDouble)));
        } else {
            return pTag;
        }
    }

    @Override
    protected Typed<?> fix(Typed<?> p_15682_) {
        return p_15682_.update(DSL.remainderFinder(), this::fixTag);
    }
}