package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1925 extends NamespacedSchema {
    public V1925(int p_392803_, Schema p_391989_) {
        super(p_392803_, p_391989_);
    }

    @Override
    public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
        super.registerTypes(pSchema, pEntityTypes, pBlockEntityTypes);
        pSchema.registerType(
            false,
            References.SAVED_DATA_MAP_DATA,
            () -> DSL.optionalFields("data", DSL.optionalFields("banners", DSL.list(DSL.optionalFields("Name", References.TEXT_COMPONENT.in(pSchema)))))
        );
    }
}