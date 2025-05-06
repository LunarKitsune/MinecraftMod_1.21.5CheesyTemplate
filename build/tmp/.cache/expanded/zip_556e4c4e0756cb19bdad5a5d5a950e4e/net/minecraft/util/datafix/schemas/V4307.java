package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4307 extends NamespacedSchema {
    public V4307(int p_394198_, Schema p_393601_) {
        super(p_394198_, p_393601_);
    }

    public static SequencedMap<String, Supplier<TypeTemplate>> components(Schema pSchema) {
        SequencedMap<String, Supplier<TypeTemplate>> sequencedmap = V4059.components(pSchema);
        sequencedmap.put("minecraft:can_place_on", () -> adventureModePredicate(pSchema));
        sequencedmap.put("minecraft:can_break", () -> adventureModePredicate(pSchema));
        return sequencedmap;
    }

    private static TypeTemplate adventureModePredicate(Schema pSchema) {
        TypeTemplate typetemplate = DSL.optionalFields("blocks", DSL.or(References.BLOCK_NAME.in(pSchema), DSL.list(References.BLOCK_NAME.in(pSchema))));
        return DSL.or(typetemplate, DSL.list(typetemplate));
    }

    @Override
    public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
        super.registerTypes(pSchema, pEntityTypes, pBlockEntityTypes);
        pSchema.registerType(true, References.DATA_COMPONENTS, () -> DSL.optionalFieldsLazy(components(pSchema)));
    }
}