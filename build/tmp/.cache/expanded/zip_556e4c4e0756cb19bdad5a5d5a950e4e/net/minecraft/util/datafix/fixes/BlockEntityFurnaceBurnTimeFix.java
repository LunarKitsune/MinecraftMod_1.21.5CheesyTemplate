package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class BlockEntityFurnaceBurnTimeFix extends NamedEntityFix {
    public BlockEntityFurnaceBurnTimeFix(Schema pOutputSchema, String pEntityName) {
        super(pOutputSchema, false, "BlockEntityFurnaceBurnTimeFix" + pEntityName, References.BLOCK_ENTITY, pEntityName);
    }

    public Dynamic<?> fixBurnTime(Dynamic<?> pTag) {
        pTag = pTag.renameField("CookTime", "cooking_time_spent");
        pTag = pTag.renameField("CookTimeTotal", "cooking_total_time");
        pTag = pTag.renameField("BurnTime", "lit_time_remaining");
        return pTag.setFieldIfPresent("lit_total_time", pTag.get("lit_time_remaining").result());
    }

    @Override
    protected Typed<?> fix(Typed<?> p_376974_) {
        return p_376974_.update(DSL.remainderFinder(), this::fixBurnTime);
    }
}