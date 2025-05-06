package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1470 extends NamespacedSchema {
    public V1470(int p_17698_, Schema p_17699_) {
        super(p_17698_, p_17699_);
    }

    protected static void registerMob(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
        pSchema.registerSimple(pMap, pName);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(pSchema);
        registerMob(pSchema, map, "minecraft:turtle");
        registerMob(pSchema, map, "minecraft:cod_mob");
        registerMob(pSchema, map, "minecraft:tropical_fish");
        registerMob(pSchema, map, "minecraft:salmon_mob");
        registerMob(pSchema, map, "minecraft:puffer_fish");
        registerMob(pSchema, map, "minecraft:phantom");
        registerMob(pSchema, map, "minecraft:dolphin");
        registerMob(pSchema, map, "minecraft:drowned");
        pSchema.register(
            map,
            "minecraft:trident",
            p_309012_ -> DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(pSchema), "Trident", References.ITEM_STACK.in(pSchema))
        );
        return map;
    }
}