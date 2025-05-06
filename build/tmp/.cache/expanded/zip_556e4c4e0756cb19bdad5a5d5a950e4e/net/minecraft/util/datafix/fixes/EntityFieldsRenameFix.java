package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Map.Entry;

public class EntityFieldsRenameFix extends NamedEntityFix {
    private final Map<String, String> renames;

    public EntityFieldsRenameFix(Schema pOutputSchema, String pName, String pEntityName, Map<String, String> pRenames) {
        super(pOutputSchema, false, pName, References.ENTITY, pEntityName);
        this.renames = pRenames;
    }

    public Dynamic<?> fixTag(Dynamic<?> pTag) {
        for (Entry<String, String> entry : this.renames.entrySet()) {
            pTag = pTag.renameField(entry.getKey(), entry.getValue());
        }

        return pTag;
    }

    @Override
    protected Typed<?> fix(Typed<?> p_378322_) {
        return p_378322_.update(DSL.remainderFinder(), this::fixTag);
    }
}