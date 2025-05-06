package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V1483 extends NamespacedSchema {
    public V1483(int p_17717_, Schema p_17718_) {
        super(p_17717_, p_17718_);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(pSchema);
        map.put("minecraft:pufferfish", map.remove("minecraft:puffer_fish"));
        return map;
    }
}