package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.List;

public class DropChancesFormatFix extends DataFix {
    private static final List<String> ARMOR_SLOT_NAMES = List.of("feet", "legs", "chest", "head");
    private static final List<String> HAND_SLOT_NAMES = List.of("mainhand", "offhand");
    private static final float DEFAULT_CHANCE = 0.085F;

    public DropChancesFormatFix(Schema pOutputSchema) {
        super(pOutputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "DropChancesFormatFix", this.getInputSchema().getType(References.ENTITY), p_392460_ -> p_392460_.update(DSL.remainderFinder(), p_395033_ -> {
                List<Float> list = parseDropChances(p_395033_.get("ArmorDropChances"));
                List<Float> list1 = parseDropChances(p_395033_.get("HandDropChances"));
                float f = p_395033_.get("body_armor_drop_chance").asNumber().result().map(Number::floatValue).orElse(0.085F);
                p_395033_ = p_395033_.remove("ArmorDropChances").remove("HandDropChances").remove("body_armor_drop_chance");
                Dynamic<?> dynamic = p_395033_.emptyMap();
                dynamic = addSlotChances(dynamic, list, ARMOR_SLOT_NAMES);
                dynamic = addSlotChances(dynamic, list1, HAND_SLOT_NAMES);
                if (f != 0.085F) {
                    dynamic = dynamic.set("body", p_395033_.createFloat(f));
                }

                return !dynamic.equals(p_395033_.emptyMap()) ? p_395033_.set("drop_chances", dynamic) : p_395033_;
            })
        );
    }

    private static Dynamic<?> addSlotChances(Dynamic<?> pTag, List<Float> pChances, List<String> pNames) {
        for (int i = 0; i < pNames.size() && i < pChances.size(); i++) {
            String s = pNames.get(i);
            float f = pChances.get(i);
            if (f != 0.085F) {
                pTag = pTag.set(s, pTag.createFloat(f));
            }
        }

        return pTag;
    }

    private static List<Float> parseDropChances(OptionalDynamic<?> pData) {
        return pData.asStream().map(p_397948_ -> p_397948_.asFloat(0.085F)).toList();
    }
}