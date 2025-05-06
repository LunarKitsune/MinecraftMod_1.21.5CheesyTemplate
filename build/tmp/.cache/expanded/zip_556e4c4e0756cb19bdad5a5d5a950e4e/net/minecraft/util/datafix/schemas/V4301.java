package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4301 extends NamespacedSchema {
    public V4301(int p_397475_, Schema p_394460_) {
        super(p_397475_, p_394460_);
    }

    @Override
    public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
        super.registerTypes(pSchema, pEntityTypes, pBlockEntityTypes);
        pSchema.registerType(
            true,
            References.ENTITY_EQUIPMENT,
            () -> DSL.optional(
                DSL.field(
                    "equipment",
                    DSL.optionalFields(
                        Pair.of("mainhand", References.ITEM_STACK.in(pSchema)),
                        Pair.of("offhand", References.ITEM_STACK.in(pSchema)),
                        Pair.of("feet", References.ITEM_STACK.in(pSchema)),
                        Pair.of("legs", References.ITEM_STACK.in(pSchema)),
                        Pair.of("chest", References.ITEM_STACK.in(pSchema)),
                        Pair.of("head", References.ITEM_STACK.in(pSchema)),
                        Pair.of("body", References.ITEM_STACK.in(pSchema)),
                        Pair.of("saddle", References.ITEM_STACK.in(pSchema))
                    )
                )
            )
        );
    }
}