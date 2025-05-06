package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class NamedEntityConvertUncheckedFix extends NamedEntityFix {
    public NamedEntityConvertUncheckedFix(Schema pOutputSchema, String pName, TypeReference pType, String pEntityName) {
        super(pOutputSchema, true, pName, pType, pEntityName);
    }

    @Override
    protected Typed<?> fix(Typed<?> p_391723_) {
        Type<?> type = this.getOutputSchema().getChoiceType(this.type, this.entityName);
        return ExtraDataFixUtils.cast(type, p_391723_);
    }
}