package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4312 extends NamespacedSchema {
    public V4312(int p_393232_, Schema p_397205_) {
        super(p_393232_, p_397205_);
    }

    @Override
    public void registerTypes(Schema pOutputSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
        super.registerTypes(pOutputSchema, pEntityTypes, pBlockEntityTypes);
        pOutputSchema.registerType(
            false,
            References.PLAYER,
            () -> DSL.and(
                References.ENTITY_EQUIPMENT.in(pOutputSchema),
                DSL.optionalFields(
                    Pair.of("RootVehicle", DSL.optionalFields("Entity", References.ENTITY_TREE.in(pOutputSchema))),
                    Pair.of("ender_pearls", DSL.list(References.ENTITY_TREE.in(pOutputSchema))),
                    Pair.of("Inventory", DSL.list(References.ITEM_STACK.in(pOutputSchema))),
                    Pair.of("EnderItems", DSL.list(References.ITEM_STACK.in(pOutputSchema))),
                    Pair.of("ShoulderEntityLeft", References.ENTITY_TREE.in(pOutputSchema)),
                    Pair.of("ShoulderEntityRight", References.ENTITY_TREE.in(pOutputSchema)),
                    Pair.of(
                        "recipeBook",
                        DSL.optionalFields("recipes", DSL.list(References.RECIPE.in(pOutputSchema)), "toBeDisplayed", DSL.list(References.RECIPE.in(pOutputSchema)))
                    )
                )
            )
        );
    }
}