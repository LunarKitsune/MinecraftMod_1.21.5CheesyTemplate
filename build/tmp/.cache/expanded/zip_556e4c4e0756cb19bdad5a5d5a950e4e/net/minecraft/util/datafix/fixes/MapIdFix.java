package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;

public class MapIdFix extends DataFix {
    public MapIdFix(Schema pOutputSchema) {
        super(pOutputSchema, true);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.writeFixAndRead(
            "Map id fix",
            this.getInputSchema().getType(References.SAVED_DATA_MAP_DATA),
            this.getOutputSchema().getType(References.SAVED_DATA_MAP_DATA),
            p_390310_ -> p_390310_.createMap(Map.of(p_390310_.createString("data"), p_390310_))
        );
    }
}