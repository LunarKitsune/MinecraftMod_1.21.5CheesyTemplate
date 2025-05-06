package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class ContainerBlockEntityLockPredicateFix extends DataFix {
    public ContainerBlockEntityLockPredicateFix(Schema pOutputSchema) {
        super(pOutputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "ContainerBlockEntityLockPredicateFix", this.getInputSchema().findChoiceType(References.BLOCK_ENTITY), ContainerBlockEntityLockPredicateFix::fixBlockEntity
        );
    }

    private static Typed<?> fixBlockEntity(Typed<?> pData) {
        return pData.update(DSL.remainderFinder(), p_368586_ -> p_368586_.renameAndFixField("Lock", "lock", LockComponentPredicateFix::fixLock));
    }
}