package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V3076 extends NamespacedSchema {
    public V3076(int p_216764_, Schema p_216765_) {
        super(p_216764_, p_216765_);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(pSchema);
        pSchema.registerSimple(map, "minecraft:sculk_catalyst");
        return map;
    }
}