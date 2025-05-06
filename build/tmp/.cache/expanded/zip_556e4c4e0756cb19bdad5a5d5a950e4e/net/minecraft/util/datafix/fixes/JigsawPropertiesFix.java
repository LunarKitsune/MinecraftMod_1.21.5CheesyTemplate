package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class JigsawPropertiesFix extends NamedEntityFix {
    public JigsawPropertiesFix(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType, "JigsawPropertiesFix", References.BLOCK_ENTITY, "minecraft:jigsaw");
    }

    private static Dynamic<?> fixTag(Dynamic<?> pTag) {
        String s = pTag.get("attachement_type").asString("minecraft:empty");
        String s1 = pTag.get("target_pool").asString("minecraft:empty");
        return pTag.set("name", pTag.createString(s))
            .set("target", pTag.createString(s))
            .remove("attachement_type")
            .set("pool", pTag.createString(s1))
            .remove("target_pool");
    }

    @Override
    protected Typed<?> fix(Typed<?> p_16185_) {
        return p_16185_.update(DSL.remainderFinder(), JigsawPropertiesFix::fixTag);
    }
}