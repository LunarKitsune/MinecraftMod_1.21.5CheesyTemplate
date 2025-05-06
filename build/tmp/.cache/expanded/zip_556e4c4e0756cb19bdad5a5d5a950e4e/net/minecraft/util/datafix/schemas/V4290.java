package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4290 extends NamespacedSchema {
    public V4290(int p_394483_, Schema p_395200_) {
        super(p_394483_, p_395200_);
    }

    @Override
    public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
        super.registerTypes(pSchema, pEntityTypes, pBlockEntityTypes);
        pSchema.registerType(
            true,
            References.TEXT_COMPONENT,
            () -> DSL.or(
                DSL.or(DSL.constType(DSL.string()), DSL.list(References.TEXT_COMPONENT.in(pSchema))),
                DSL.optionalFields(
                    "extra",
                    DSL.list(References.TEXT_COMPONENT.in(pSchema)),
                    "separator",
                    References.TEXT_COMPONENT.in(pSchema),
                    "hoverEvent",
                    DSL.taggedChoice(
                        "action",
                        DSL.string(),
                        Map.of(
                            "show_text",
                            DSL.optionalFields("contents", References.TEXT_COMPONENT.in(pSchema)),
                            "show_item",
                            DSL.optionalFields("contents", DSL.or(References.ITEM_STACK.in(pSchema), References.ITEM_NAME.in(pSchema))),
                            "show_entity",
                            DSL.optionalFields("type", References.ENTITY_NAME.in(pSchema), "name", References.TEXT_COMPONENT.in(pSchema))
                        )
                    )
                )
            )
        );
    }
}