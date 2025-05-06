package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3818_3 extends NamespacedSchema {
    public V3818_3(int p_333453_, Schema p_330765_) {
        super(p_333453_, p_330765_);
    }

    public static SequencedMap<String, Supplier<TypeTemplate>> components(Schema pSchema) {
        SequencedMap<String, Supplier<TypeTemplate>> sequencedmap = new LinkedHashMap<>();
        sequencedmap.put("minecraft:bees", () -> DSL.list(DSL.optionalFields("entity_data", References.ENTITY_TREE.in(pSchema))));
        sequencedmap.put("minecraft:block_entity_data", () -> References.BLOCK_ENTITY.in(pSchema));
        sequencedmap.put("minecraft:bundle_contents", () -> DSL.list(References.ITEM_STACK.in(pSchema)));
        sequencedmap.put(
            "minecraft:can_break",
            () -> DSL.optionalFields(
                "predicates", DSL.list(DSL.optionalFields("blocks", DSL.or(References.BLOCK_NAME.in(pSchema), DSL.list(References.BLOCK_NAME.in(pSchema)))))
            )
        );
        sequencedmap.put(
            "minecraft:can_place_on",
            () -> DSL.optionalFields(
                "predicates", DSL.list(DSL.optionalFields("blocks", DSL.or(References.BLOCK_NAME.in(pSchema), DSL.list(References.BLOCK_NAME.in(pSchema)))))
            )
        );
        sequencedmap.put("minecraft:charged_projectiles", () -> DSL.list(References.ITEM_STACK.in(pSchema)));
        sequencedmap.put("minecraft:container", () -> DSL.list(DSL.optionalFields("item", References.ITEM_STACK.in(pSchema))));
        sequencedmap.put("minecraft:entity_data", () -> References.ENTITY_TREE.in(pSchema));
        sequencedmap.put("minecraft:pot_decorations", () -> DSL.list(References.ITEM_NAME.in(pSchema)));
        sequencedmap.put("minecraft:food", () -> DSL.optionalFields("using_converts_to", References.ITEM_STACK.in(pSchema)));
        sequencedmap.put("minecraft:custom_name", () -> References.TEXT_COMPONENT.in(pSchema));
        sequencedmap.put("minecraft:item_name", () -> References.TEXT_COMPONENT.in(pSchema));
        sequencedmap.put("minecraft:lore", () -> DSL.list(References.TEXT_COMPONENT.in(pSchema)));
        sequencedmap.put(
            "minecraft:written_book_content",
            () -> DSL.optionalFields(
                "pages",
                DSL.list(
                    DSL.or(
                        DSL.optionalFields("raw", References.TEXT_COMPONENT.in(pSchema), "filtered", References.TEXT_COMPONENT.in(pSchema)),
                        References.TEXT_COMPONENT.in(pSchema)
                    )
                )
            )
        );
        return sequencedmap;
    }

    @Override
    public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
        super.registerTypes(pSchema, pEntityTypes, pBlockEntityTypes);
        pSchema.registerType(true, References.DATA_COMPONENTS, () -> DSL.optionalFieldsLazy(components(pSchema)));
    }
}