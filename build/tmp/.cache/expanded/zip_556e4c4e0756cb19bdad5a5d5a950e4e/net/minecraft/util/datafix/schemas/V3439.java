package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3439 extends NamespacedSchema {
    public V3439(int p_396938_, Schema p_397912_) {
        super(p_396938_, p_397912_);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(pSchema);
        this.register(map, "minecraft:sign", () -> sign(pSchema));
        this.register(map, "minecraft:hanging_sign", () -> sign(pSchema));
        return map;
    }

    private static TypeTemplate sign(Schema pSchema) {
        return DSL.optionalFields(
            "front_text",
            DSL.optionalFields("messages", DSL.list(References.TEXT_COMPONENT.in(pSchema)), "filtered_messages", DSL.list(References.TEXT_COMPONENT.in(pSchema))),
            "back_text",
            DSL.optionalFields("messages", DSL.list(References.TEXT_COMPONENT.in(pSchema)), "filtered_messages", DSL.list(References.TEXT_COMPONENT.in(pSchema)))
        );
    }
}