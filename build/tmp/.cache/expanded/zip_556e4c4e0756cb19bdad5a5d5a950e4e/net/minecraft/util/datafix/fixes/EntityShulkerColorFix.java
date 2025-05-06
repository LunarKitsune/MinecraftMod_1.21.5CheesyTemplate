package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class EntityShulkerColorFix extends NamedEntityFix {
    public EntityShulkerColorFix(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType, "EntityShulkerColorFix", References.ENTITY, "minecraft:shulker");
    }

    public Dynamic<?> fixTag(Dynamic<?> pTag) {
        return pTag.get("Color").map(Dynamic::asNumber).result().isEmpty() ? pTag.set("Color", pTag.createByte((byte)10)) : pTag;
    }

    @Override
    protected Typed<?> fix(Typed<?> p_15676_) {
        return p_15676_.update(DSL.remainderFinder(), this::fixTag);
    }
}