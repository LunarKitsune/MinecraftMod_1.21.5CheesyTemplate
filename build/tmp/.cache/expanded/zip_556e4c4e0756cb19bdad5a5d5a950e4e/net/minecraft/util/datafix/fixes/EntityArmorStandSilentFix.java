package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class EntityArmorStandSilentFix extends NamedEntityFix {
    public EntityArmorStandSilentFix(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType, "EntityArmorStandSilentFix", References.ENTITY, "ArmorStand");
    }

    public Dynamic<?> fixTag(Dynamic<?> pTag) {
        return pTag.get("Silent").asBoolean(false) && !pTag.get("Marker").asBoolean(false) ? pTag.remove("Silent") : pTag;
    }

    @Override
    protected Typed<?> fix(Typed<?> p_15327_) {
        return p_15327_.update(DSL.remainderFinder(), this::fixTag);
    }
}