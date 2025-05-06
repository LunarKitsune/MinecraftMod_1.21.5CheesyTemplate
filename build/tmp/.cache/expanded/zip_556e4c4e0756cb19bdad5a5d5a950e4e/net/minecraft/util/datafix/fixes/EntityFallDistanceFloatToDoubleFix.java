package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class EntityFallDistanceFloatToDoubleFix extends DataFix {
    private TypeReference type;

    public EntityFallDistanceFloatToDoubleFix(Schema pOutputSchema, TypeReference pType) {
        super(pOutputSchema, false);
        this.type = pType;
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "EntityFallDistanceFloatToDoubleFixFor" + this.type.typeName(),
            this.getOutputSchema().getType(this.type),
            EntityFallDistanceFloatToDoubleFix::fixEntity
        );
    }

    private static Typed<?> fixEntity(Typed<?> pData) {
        return pData.update(
            DSL.remainderFinder(),
            p_392978_ -> p_392978_.renameAndFixField("FallDistance", "fall_distance", p_393317_ -> p_393317_.createDouble(p_393317_.asFloat(0.0F)))
        );
    }
}