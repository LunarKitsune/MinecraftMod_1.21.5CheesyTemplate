package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V100 extends Schema {
    public V100(int pVersionKey, Schema pParent) {
        super(pVersionKey, pParent);
    }

    @Override
    public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
        super.registerTypes(pSchema, pEntityTypes, pBlockEntityTypes);
        pSchema.registerType(
            true,
            References.ENTITY_EQUIPMENT,
            () -> DSL.and(
                DSL.optional(DSL.field("ArmorItems", DSL.list(References.ITEM_STACK.in(pSchema)))),
                DSL.optional(DSL.field("HandItems", DSL.list(References.ITEM_STACK.in(pSchema)))),
                DSL.optional(DSL.field("body_armor_item", References.ITEM_STACK.in(pSchema))),
                DSL.optional(DSL.field("saddle", References.ITEM_STACK.in(pSchema)))
            )
        );
    }
}