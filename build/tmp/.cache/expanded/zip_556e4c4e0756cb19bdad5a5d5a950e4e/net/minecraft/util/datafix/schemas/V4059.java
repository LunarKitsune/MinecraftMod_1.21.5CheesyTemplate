package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4059 extends NamespacedSchema {
    public V4059(int p_365559_, Schema p_368040_) {
        super(p_365559_, p_368040_);
    }

    public static SequencedMap<String, Supplier<TypeTemplate>> components(Schema pSchema) {
        SequencedMap<String, Supplier<TypeTemplate>> sequencedmap = V3818_3.components(pSchema);
        sequencedmap.remove("minecraft:food");
        sequencedmap.put("minecraft:use_remainder", () -> References.ITEM_STACK.in(pSchema));
        sequencedmap.put(
            "minecraft:equippable",
            () -> DSL.optionalFields("allowed_entities", DSL.or(References.ENTITY_NAME.in(pSchema), DSL.list(References.ENTITY_NAME.in(pSchema))))
        );
        return sequencedmap;
    }

    @Override
    public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
        super.registerTypes(pSchema, pEntityTypes, pBlockEntityTypes);
        pSchema.registerType(true, References.DATA_COMPONENTS, () -> DSL.optionalFieldsLazy(components(pSchema)));
    }
}