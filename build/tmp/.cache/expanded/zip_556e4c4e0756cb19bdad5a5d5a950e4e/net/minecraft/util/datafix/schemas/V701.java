package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V701 extends Schema {
    public V701(int pVersionKey, Schema pParent) {
        super(pVersionKey, pParent);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(pSchema);
        pSchema.registerSimple(map, "WitherSkeleton");
        pSchema.registerSimple(map, "Stray");
        return map;
    }
}