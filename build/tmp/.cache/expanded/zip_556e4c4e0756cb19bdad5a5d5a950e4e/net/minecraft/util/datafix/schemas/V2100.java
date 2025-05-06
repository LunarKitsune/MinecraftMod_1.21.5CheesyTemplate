package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V2100 extends NamespacedSchema {
    public V2100(int p_17833_, Schema p_17834_) {
        super(p_17833_, p_17834_);
    }

    protected static void registerMob(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
        pSchema.registerSimple(pMap, pName);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(pSchema);
        registerMob(pSchema, map, "minecraft:bee");
        registerMob(pSchema, map, "minecraft:bee_stinger");
        return map;
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(pSchema);
        pSchema.register(
            map, "minecraft:beehive", () -> DSL.optionalFields("Bees", DSL.list(DSL.optionalFields("EntityData", References.ENTITY_TREE.in(pSchema))))
        );
        return map;
    }
}