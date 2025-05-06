package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class SaddleEquipmentSlotFix extends DataFix {
    private static final Set<String> ENTITIES_WITH_SADDLE_ITEM = Set.of(
        "minecraft:horse",
        "minecraft:skeleton_horse",
        "minecraft:zombie_horse",
        "minecraft:donkey",
        "minecraft:mule",
        "minecraft:camel",
        "minecraft:llama",
        "minecraft:trader_llama"
    );
    private static final Set<String> ENTITIES_WITH_SADDLE_FLAG = Set.of("minecraft:pig", "minecraft:strider");
    private static final String SADDLE_FLAG = "Saddle";
    private static final String NEW_SADDLE = "saddle";

    public SaddleEquipmentSlotFix(Schema pOutputSchema) {
        super(pOutputSchema, true);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        TaggedChoiceType<String> taggedchoicetype = (TaggedChoiceType<String>)this.getInputSchema().findChoiceType(References.ENTITY);
        OpticFinder<Pair<String, ?>> opticfinder = DSL.typeFinder(taggedchoicetype);
        Type<?> type = this.getInputSchema().getType(References.ENTITY);
        Type<?> type1 = this.getOutputSchema().getType(References.ENTITY);
        Type<?> type2 = ExtraDataFixUtils.patchSubType(type, type, type1);
        return this.fixTypeEverywhereTyped("SaddleEquipmentSlotFix", type, type1, p_392493_ -> {
            String s = p_392493_.getOptional(opticfinder).map(Pair::getFirst).map(NamespacedSchema::ensureNamespaced).orElse("");
            Typed<?> typed = ExtraDataFixUtils.cast(type2, p_392493_);
            if (ENTITIES_WITH_SADDLE_ITEM.contains(s)) {
                return Util.writeAndReadTypedOrThrow(typed, type1, SaddleEquipmentSlotFix::fixEntityWithSaddleItem);
            } else {
                return ENTITIES_WITH_SADDLE_FLAG.contains(s) ? Util.writeAndReadTypedOrThrow(typed, type1, SaddleEquipmentSlotFix::fixEntityWithSaddleFlag) : ExtraDataFixUtils.cast(type1, p_392493_);
            }
        });
    }

    private static Dynamic<?> fixEntityWithSaddleItem(Dynamic<?> pData) {
        return pData.get("SaddleItem").result().isEmpty() ? pData : fixDropChances(pData.renameField("SaddleItem", "saddle"));
    }

    private static Dynamic<?> fixEntityWithSaddleFlag(Dynamic<?> pData) {
        boolean flag = pData.get("Saddle").asBoolean(false);
        pData = pData.remove("Saddle");
        if (!flag) {
            return pData;
        } else {
            Dynamic<?> dynamic = pData.emptyMap().set("id", pData.createString("minecraft:saddle")).set("count", pData.createInt(1));
            return fixDropChances(pData.set("saddle", dynamic));
        }
    }

    private static Dynamic<?> fixDropChances(Dynamic<?> pData) {
        Dynamic<?> dynamic = pData.get("drop_chances").orElseEmptyMap().set("saddle", pData.createFloat(2.0F));
        return pData.set("drop_chances", dynamic);
    }
}