package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Sets;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.Set;

public class EntityHealthFix extends DataFix {
    private static final Set<String> ENTITIES = Sets.newHashSet(
        "ArmorStand",
        "Bat",
        "Blaze",
        "CaveSpider",
        "Chicken",
        "Cow",
        "Creeper",
        "EnderDragon",
        "Enderman",
        "Endermite",
        "EntityHorse",
        "Ghast",
        "Giant",
        "Guardian",
        "LavaSlime",
        "MushroomCow",
        "Ozelot",
        "Pig",
        "PigZombie",
        "Rabbit",
        "Sheep",
        "Shulker",
        "Silverfish",
        "Skeleton",
        "Slime",
        "SnowMan",
        "Spider",
        "Squid",
        "Villager",
        "VillagerGolem",
        "Witch",
        "WitherBoss",
        "Wolf",
        "Zombie"
    );

    public EntityHealthFix(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType);
    }

    public Dynamic<?> fixTag(Dynamic<?> pTag) {
        Optional<Number> optional = pTag.get("HealF").asNumber().result();
        Optional<Number> optional1 = pTag.get("Health").asNumber().result();
        float f;
        if (optional.isPresent()) {
            f = optional.get().floatValue();
            pTag = pTag.remove("HealF");
        } else {
            if (!optional1.isPresent()) {
                return pTag;
            }

            f = optional1.get().floatValue();
        }

        return pTag.set("Health", pTag.createFloat(f));
    }

    @Override
    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "EntityHealthFix", this.getInputSchema().getType(References.ENTITY), p_15437_ -> p_15437_.update(DSL.remainderFinder(), this::fixTag)
        );
    }
}