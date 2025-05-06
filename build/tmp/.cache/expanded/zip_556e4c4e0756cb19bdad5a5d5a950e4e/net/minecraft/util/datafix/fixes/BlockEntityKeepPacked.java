package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class BlockEntityKeepPacked extends NamedEntityFix {
    public BlockEntityKeepPacked(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType, "BlockEntityKeepPacked", References.BLOCK_ENTITY, "DUMMY");
    }

    private static Dynamic<?> fixTag(Dynamic<?> pTag) {
        return pTag.set("keepPacked", pTag.createBoolean(true));
    }

    @Override
    protected Typed<?> fix(Typed<?> p_14851_) {
        return p_14851_.update(DSL.remainderFinder(), BlockEntityKeepPacked::fixTag);
    }
}