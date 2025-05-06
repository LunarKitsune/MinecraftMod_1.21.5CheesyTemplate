package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4292 extends NamespacedSchema {
    public V4292(int p_393326_, Schema p_391298_) {
        super(p_393326_, p_391298_);
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
                    "hover_event",
                    DSL.taggedChoice(
                        "action",
                        DSL.string(),
                        Map.of(
                            "show_text",
                            DSL.optionalFields("value", References.TEXT_COMPONENT.in(pSchema)),
                            "show_item",
                            References.ITEM_STACK.in(pSchema),
                            "show_entity",
                            DSL.optionalFields("id", References.ENTITY_NAME.in(pSchema), "name", References.TEXT_COMPONENT.in(pSchema))
                        )
                    )
                )
            )
        );
    }
}