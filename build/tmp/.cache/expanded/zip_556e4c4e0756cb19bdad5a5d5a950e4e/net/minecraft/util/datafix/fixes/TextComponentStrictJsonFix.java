package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class TextComponentStrictJsonFix extends DataFix {
    public TextComponentStrictJsonFix(Schema pOutputSchema) {
        super(pOutputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<Pair<String, String>> type = (Type<Pair<String, String>>)this.getInputSchema().getType(References.TEXT_COMPONENT);
        return this.fixTypeEverywhere("TextComponentStrictJsonFix", type, p_397503_ -> p_393832_ -> p_393832_.mapSecond(LegacyComponentDataFixUtils::rewriteFromLenient));
    }
}