package net.minecraft.util.datafix.fixes;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.Util;
import org.slf4j.Logger;

public class UnflattenTextComponentFix extends DataFix {
    private static final Logger LOGGER = LogUtils.getLogger();

    public UnflattenTextComponentFix(Schema pOutputSchema) {
        super(pOutputSchema, true);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<Pair<String, String>> type = (Type<Pair<String, String>>)this.getInputSchema().getType(References.TEXT_COMPONENT);
        Type<?> type1 = this.getOutputSchema().getType(References.TEXT_COMPONENT);
        return this.createFixer(type, type1);
    }

    private <T> TypeRewriteRule createFixer(Type<Pair<String, String>> pInputType, Type<T> pOutputType) {
        return this.fixTypeEverywhere(
            "UnflattenTextComponentFix",
            pInputType,
            pOutputType,
            p_394708_ -> p_394788_ -> Util.readTypedOrThrow(pOutputType, unflattenJson(p_394708_, p_394788_.getSecond()), true).getValue()
        );
    }

    private static <T> Dynamic<T> unflattenJson(DynamicOps<T> pOps, String pJson) {
        try {
            JsonElement jsonelement = JsonParser.parseString(pJson);
            if (!jsonelement.isJsonNull()) {
                return new Dynamic<>(pOps, JsonOps.INSTANCE.convertTo(pOps, jsonelement));
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to unflatten text component json: {}", pJson, exception);
        }

        return new Dynamic<>(pOps, pOps.createString(pJson));
    }
}