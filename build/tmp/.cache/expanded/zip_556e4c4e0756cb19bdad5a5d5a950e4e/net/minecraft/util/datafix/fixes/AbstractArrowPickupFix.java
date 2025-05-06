package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.function.Function;

public class AbstractArrowPickupFix extends DataFix {
    public AbstractArrowPickupFix(Schema pOutputSchema) {
        super(pOutputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Schema schema = this.getInputSchema();
        return this.fixTypeEverywhereTyped("AbstractArrowPickupFix", schema.getType(References.ENTITY), this::updateProjectiles);
    }

    private Typed<?> updateProjectiles(Typed<?> pTyped) {
        pTyped = this.updateEntity(pTyped, "minecraft:arrow", AbstractArrowPickupFix::updatePickup);
        pTyped = this.updateEntity(pTyped, "minecraft:spectral_arrow", AbstractArrowPickupFix::updatePickup);
        return this.updateEntity(pTyped, "minecraft:trident", AbstractArrowPickupFix::updatePickup);
    }

    private static Dynamic<?> updatePickup(Dynamic<?> pDynamic) {
        if (pDynamic.get("pickup").result().isPresent()) {
            return pDynamic;
        } else {
            boolean flag = pDynamic.get("player").asBoolean(true);
            return pDynamic.set("pickup", pDynamic.createByte((byte)(flag ? 1 : 0))).remove("player");
        }
    }

    private Typed<?> updateEntity(Typed<?> pTyped, String pChoiceName, Function<Dynamic<?>, Dynamic<?>> pUpdater) {
        Type<?> type = this.getInputSchema().getChoiceType(References.ENTITY, pChoiceName);
        Type<?> type1 = this.getOutputSchema().getChoiceType(References.ENTITY, pChoiceName);
        return pTyped.updateTyped(DSL.namedChoice(pChoiceName, type), type1, p_145057_ -> p_145057_.update(DSL.remainderFinder(), pUpdater));
    }
}