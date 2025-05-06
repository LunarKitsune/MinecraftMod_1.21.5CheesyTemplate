package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V2501 extends NamespacedSchema {
    public V2501(int p_17848_, Schema p_17849_) {
        super(p_17848_, p_17849_);
    }

    private static void registerFurnace(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
        pSchema.register(
            pMap,
            pName,
            () -> DSL.optionalFields(
                "Items",
                DSL.list(References.ITEM_STACK.in(pSchema)),
                "CustomName",
                References.TEXT_COMPONENT.in(pSchema),
                "RecipesUsed",
                DSL.compoundList(References.RECIPE.in(pSchema), DSL.constType(DSL.intType()))
            )
        );
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(pSchema);
        registerFurnace(pSchema, map, "minecraft:furnace");
        registerFurnace(pSchema, map, "minecraft:smoker");
        registerFurnace(pSchema, map, "minecraft:blast_furnace");
        return map;
    }
}