package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class VillagerSetCanPickUpLootFix extends NamedEntityFix {
    private static final String CAN_PICK_UP_LOOT = "CanPickUpLoot";

    public VillagerSetCanPickUpLootFix(Schema pOutputSchema) {
        super(pOutputSchema, true, "Villager CanPickUpLoot default value", References.ENTITY, "Villager");
    }

    @Override
    protected Typed<?> fix(Typed<?> p_360984_) {
        return p_360984_.update(DSL.remainderFinder(), VillagerSetCanPickUpLootFix::fixValue);
    }

    private static Dynamic<?> fixValue(Dynamic<?> pTag) {
        return pTag.set("CanPickUpLoot", pTag.createBoolean(true));
    }
}