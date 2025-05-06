package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3689 extends NamespacedSchema {
    public V3689(int p_312091_, Schema p_310599_) {
        super(p_312091_, p_310599_);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(pSchema);
        pSchema.registerSimple(map, "minecraft:breeze");
        pSchema.registerSimple(map, "minecraft:wind_charge");
        pSchema.registerSimple(map, "minecraft:breeze_wind_charge");
        return map;
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(pSchema);
        pSchema.register(
            map,
            "minecraft:trial_spawner",
            () -> DSL.optionalFields(
                "spawn_potentials",
                DSL.list(DSL.fields("data", DSL.fields("entity", References.ENTITY_TREE.in(pSchema)))),
                "spawn_data",
                DSL.fields("entity", References.ENTITY_TREE.in(pSchema))
            )
        );
        return map;
    }
}