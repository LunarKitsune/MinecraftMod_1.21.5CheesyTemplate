package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class RemoveGolemGossipFix extends NamedEntityFix {
    public RemoveGolemGossipFix(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType, "Remove Golem Gossip Fix", References.ENTITY, "minecraft:villager");
    }

    @Override
    protected Typed<?> fix(Typed<?> p_16826_) {
        return p_16826_.update(DSL.remainderFinder(), RemoveGolemGossipFix::fixValue);
    }

    private static Dynamic<?> fixValue(Dynamic<?> pDynamic) {
        return pDynamic.update(
            "Gossips", p_16831_ -> pDynamic.createList(p_16831_.asStream().filter(p_145632_ -> !p_145632_.get("Type").asString("").equals("golem")))
        );
    }
}