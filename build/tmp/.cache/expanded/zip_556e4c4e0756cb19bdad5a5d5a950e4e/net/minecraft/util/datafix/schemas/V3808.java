package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3808 extends NamespacedSchema {
    public V3808(int p_332469_, Schema p_333434_) {
        super(p_332469_, p_333434_);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(pSchema);
        pSchema.register(map, "minecraft:horse", p_390394_ -> DSL.optionalFields("SaddleItem", References.ITEM_STACK.in(pSchema)));
        return map;
    }
}