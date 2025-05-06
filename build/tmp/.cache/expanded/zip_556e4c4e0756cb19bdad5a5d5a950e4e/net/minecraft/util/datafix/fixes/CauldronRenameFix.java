package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class CauldronRenameFix extends DataFix {
    public CauldronRenameFix(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType);
    }

    private static Dynamic<?> fix(Dynamic<?> pDynamic) {
        Optional<String> optional = pDynamic.get("Name").asString().result();
        if (optional.equals(Optional.of("minecraft:cauldron"))) {
            Dynamic<?> dynamic = pDynamic.get("Properties").orElseEmptyMap();
            return dynamic.get("level").asString("0").equals("0")
                ? pDynamic.remove("Properties")
                : pDynamic.set("Name", pDynamic.createString("minecraft:water_cauldron"));
        } else {
            return pDynamic;
        }
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "cauldron_rename_fix",
            this.getInputSchema().getType(References.BLOCK_STATE),
            p_145199_ -> p_145199_.update(DSL.remainderFinder(), CauldronRenameFix::fix)
        );
    }
}