package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ReorganizePoi extends DataFix {
    public ReorganizePoi(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<Pair<String, Dynamic<?>>> type = DSL.named(References.POI_CHUNK.typeName(), DSL.remainderType());
        if (!Objects.equals(type, this.getInputSchema().getType(References.POI_CHUNK))) {
            throw new IllegalStateException("Poi type is not what was expected.");
        } else {
            return this.fixTypeEverywhere("POI reorganization", type, p_16860_ -> p_145640_ -> p_145640_.mapSecond(ReorganizePoi::cap));
        }
    }

    private static <T> Dynamic<T> cap(Dynamic<T> pDynamic) {
        Map<Dynamic<T>, Dynamic<T>> map = Maps.newHashMap();

        for (int i = 0; i < 16; i++) {
            String s = String.valueOf(i);
            Optional<Dynamic<T>> optional = pDynamic.get(s).result();
            if (optional.isPresent()) {
                Dynamic<T> dynamic = optional.get();
                Dynamic<T> dynamic1 = pDynamic.createMap(ImmutableMap.of(pDynamic.createString("Records"), dynamic));
                map.put(pDynamic.createString(Integer.toString(i)), dynamic1);
                pDynamic = pDynamic.remove(s);
            }
        }

        return pDynamic.set("Sections", pDynamic.createMap(map));
    }
}