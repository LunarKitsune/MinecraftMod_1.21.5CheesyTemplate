package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4300 extends NamespacedSchema {
    public V4300(int p_391864_, Schema p_395992_) {
        super(p_391864_, p_395992_);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(pSchema);
        pSchema.register(map, "minecraft:llama", p_394953_ -> entityWithInventory(pSchema));
        pSchema.register(map, "minecraft:trader_llama", p_397421_ -> entityWithInventory(pSchema));
        pSchema.register(map, "minecraft:donkey", p_391407_ -> entityWithInventory(pSchema));
        pSchema.register(map, "minecraft:mule", p_392509_ -> entityWithInventory(pSchema));
        pSchema.registerSimple(map, "minecraft:horse");
        pSchema.registerSimple(map, "minecraft:skeleton_horse");
        pSchema.registerSimple(map, "minecraft:zombie_horse");
        return map;
    }

    private static TypeTemplate entityWithInventory(Schema pSchema) {
        return DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(pSchema)));
    }
}