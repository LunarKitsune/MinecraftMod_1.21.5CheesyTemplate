package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4070 extends NamespacedSchema {
    public V4070(int p_367059_, Schema p_368410_) {
        super(p_367059_, p_368410_);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(pSchema);
        pSchema.registerSimple(map, "minecraft:pale_oak_boat");
        pSchema.register(map, "minecraft:pale_oak_chest_boat", p_361560_ -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(pSchema))));
        return map;
    }
}