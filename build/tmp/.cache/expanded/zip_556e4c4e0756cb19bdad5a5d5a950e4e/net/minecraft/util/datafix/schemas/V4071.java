package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V4071 extends NamespacedSchema {
    public V4071(int p_361008_, Schema p_362992_) {
        super(p_361008_, p_362992_);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(pSchema);
        pSchema.registerSimple(map, "minecraft:creaking");
        pSchema.registerSimple(map, "minecraft:creaking_transient");
        return map;
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(pSchema);
        this.registerSimple(map, "minecraft:creaking_heart");
        return map;
    }
}