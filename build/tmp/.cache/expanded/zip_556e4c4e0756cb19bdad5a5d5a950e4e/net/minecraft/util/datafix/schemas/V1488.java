package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1488 extends NamespacedSchema {
    public V1488(int p_395175_, Schema p_397841_) {
        super(p_395175_, p_397841_);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(pSchema);
        pSchema.register(
            map,
            "minecraft:command_block",
            () -> DSL.optionalFields("CustomName", References.TEXT_COMPONENT.in(pSchema), "LastOutput", References.TEXT_COMPONENT.in(pSchema))
        );
        return map;
    }
}