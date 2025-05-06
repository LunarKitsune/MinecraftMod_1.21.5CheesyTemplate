package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.UnaryOperator;

public class BlockPropertyRenameAndFix extends AbstractBlockPropertyFix {
    private final String blockId;
    private final String oldPropertyName;
    private final String newPropertyName;
    private final UnaryOperator<String> valueFixer;

    public BlockPropertyRenameAndFix(Schema pOutputSchema, String pName, String pBlockId, String pOldPropertyName, String pNewPropertyName, UnaryOperator<String> pValueFixer) {
        super(pOutputSchema, pName);
        this.blockId = pBlockId;
        this.oldPropertyName = pOldPropertyName;
        this.newPropertyName = pNewPropertyName;
        this.valueFixer = pValueFixer;
    }

    @Override
    protected boolean shouldFix(String p_397317_) {
        return p_397317_.equals(this.blockId);
    }

    @Override
    protected <T> Dynamic<T> fixProperties(String p_396643_, Dynamic<T> p_397950_) {
        return p_397950_.renameAndFixField(this.oldPropertyName, this.newPropertyName, p_392508_ -> p_392508_.createString(this.valueFixer.apply(p_392508_.asString(""))));
    }
}