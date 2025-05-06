package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.DoubleUnaryOperator;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntityAttributeBaseValueFix extends NamedEntityFix {
    private final String attributeId;
    private final DoubleUnaryOperator valueFixer;

    public EntityAttributeBaseValueFix(Schema pOutputSchema, String pName, String pEntityName, String pAttributeId, DoubleUnaryOperator pValueFixer) {
        super(pOutputSchema, false, pName, References.ENTITY, pEntityName);
        this.attributeId = pAttributeId;
        this.valueFixer = pValueFixer;
    }

    @Override
    protected Typed<?> fix(Typed<?> p_378195_) {
        return p_378195_.update(DSL.remainderFinder(), this::fixValue);
    }

    private Dynamic<?> fixValue(Dynamic<?> pTag) {
        return pTag.update("attributes", p_376705_ -> pTag.createList(p_376705_.asStream().map(p_378716_ -> {
            String s = NamespacedSchema.ensureNamespaced(p_378716_.get("id").asString(""));
            if (!s.equals(this.attributeId)) {
                return p_378716_;
            } else {
                double d0 = p_378716_.get("base").asDouble(0.0);
                return p_378716_.set("base", p_378716_.createDouble(this.valueFixer.applyAsDouble(d0)));
            }
        })));
    }
}