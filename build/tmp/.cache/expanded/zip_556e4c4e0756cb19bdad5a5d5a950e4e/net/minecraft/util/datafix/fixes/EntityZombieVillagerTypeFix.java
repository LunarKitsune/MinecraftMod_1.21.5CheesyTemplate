package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.RandomSource;

public class EntityZombieVillagerTypeFix extends NamedEntityFix {
    private static final int PROFESSION_MAX = 6;

    public EntityZombieVillagerTypeFix(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType, "EntityZombieVillagerTypeFix", References.ENTITY, "Zombie");
    }

    public Dynamic<?> fixTag(Dynamic<?> pTag) {
        if (pTag.get("IsVillager").asBoolean(false)) {
            if (pTag.get("ZombieType").result().isEmpty()) {
                int i = this.getVillagerProfession(pTag.get("VillagerProfession").asInt(-1));
                if (i == -1) {
                    i = this.getVillagerProfession(RandomSource.create().nextInt(6));
                }

                pTag = pTag.set("ZombieType", pTag.createInt(i));
            }

            pTag = pTag.remove("IsVillager");
        }

        return pTag;
    }

    private int getVillagerProfession(int pVillagerProfession) {
        return pVillagerProfession >= 0 && pVillagerProfession < 6 ? pVillagerProfession : -1;
    }

    @Override
    protected Typed<?> fix(Typed<?> p_15811_) {
        return p_15811_.update(DSL.remainderFinder(), this::fixTag);
    }
}