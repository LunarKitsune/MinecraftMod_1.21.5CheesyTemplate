package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1458 extends NamespacedSchema {
    public V1458(int p_394988_, Schema p_397437_) {
        super(p_394988_, p_397437_);
    }

    @Override
    public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
        super.registerTypes(pSchema, pEntityTypes, pBlockEntityTypes);
        pSchema.registerType(
            true,
            References.ENTITY,
            () -> DSL.and(
                References.ENTITY_EQUIPMENT.in(pSchema),
                DSL.optionalFields("CustomName", References.TEXT_COMPONENT.in(pSchema), DSL.taggedChoiceLazy("id", namespacedString(), pEntityTypes))
            )
        );
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(pSchema);
        pSchema.register(map, "minecraft:beacon", () -> nameable(pSchema));
        pSchema.register(map, "minecraft:banner", () -> nameable(pSchema));
        pSchema.register(map, "minecraft:brewing_stand", () -> nameableInventory(pSchema));
        pSchema.register(map, "minecraft:chest", () -> nameableInventory(pSchema));
        pSchema.register(map, "minecraft:trapped_chest", () -> nameableInventory(pSchema));
        pSchema.register(map, "minecraft:dispenser", () -> nameableInventory(pSchema));
        pSchema.register(map, "minecraft:dropper", () -> nameableInventory(pSchema));
        pSchema.register(map, "minecraft:enchanting_table", () -> nameable(pSchema));
        pSchema.register(map, "minecraft:furnace", () -> nameableInventory(pSchema));
        pSchema.register(map, "minecraft:hopper", () -> nameableInventory(pSchema));
        pSchema.register(map, "minecraft:shulker_box", () -> nameableInventory(pSchema));
        return map;
    }

    public static TypeTemplate nameableInventory(Schema pSchema) {
        return DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(pSchema)), "CustomName", References.TEXT_COMPONENT.in(pSchema));
    }

    public static TypeTemplate nameable(Schema pSchema) {
        return DSL.optionalFields("CustomName", References.TEXT_COMPONENT.in(pSchema));
    }
}