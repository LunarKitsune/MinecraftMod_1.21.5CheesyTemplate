package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.Arrays;
import java.util.function.Function;

public class EntityProjectileOwnerFix extends DataFix {
    public EntityProjectileOwnerFix(Schema pOutputSchema) {
        super(pOutputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Schema schema = this.getInputSchema();
        return this.fixTypeEverywhereTyped("EntityProjectileOwner", schema.getType(References.ENTITY), this::updateProjectiles);
    }

    private Typed<?> updateProjectiles(Typed<?> pTyped) {
        pTyped = this.updateEntity(pTyped, "minecraft:egg", this::updateOwnerThrowable);
        pTyped = this.updateEntity(pTyped, "minecraft:ender_pearl", this::updateOwnerThrowable);
        pTyped = this.updateEntity(pTyped, "minecraft:experience_bottle", this::updateOwnerThrowable);
        pTyped = this.updateEntity(pTyped, "minecraft:snowball", this::updateOwnerThrowable);
        pTyped = this.updateEntity(pTyped, "minecraft:potion", this::updateOwnerThrowable);
        pTyped = this.updateEntity(pTyped, "minecraft:llama_spit", this::updateOwnerLlamaSpit);
        pTyped = this.updateEntity(pTyped, "minecraft:arrow", this::updateOwnerArrow);
        pTyped = this.updateEntity(pTyped, "minecraft:spectral_arrow", this::updateOwnerArrow);
        return this.updateEntity(pTyped, "minecraft:trident", this::updateOwnerArrow);
    }

    private Dynamic<?> updateOwnerArrow(Dynamic<?> pArrowTag) {
        long i = pArrowTag.get("OwnerUUIDMost").asLong(0L);
        long j = pArrowTag.get("OwnerUUIDLeast").asLong(0L);
        return this.setUUID(pArrowTag, i, j).remove("OwnerUUIDMost").remove("OwnerUUIDLeast");
    }

    private Dynamic<?> updateOwnerLlamaSpit(Dynamic<?> pLlamaSpitTag) {
        OptionalDynamic<?> optionaldynamic = pLlamaSpitTag.get("Owner");
        long i = optionaldynamic.get("OwnerUUIDMost").asLong(0L);
        long j = optionaldynamic.get("OwnerUUIDLeast").asLong(0L);
        return this.setUUID(pLlamaSpitTag, i, j).remove("Owner");
    }

    private Dynamic<?> updateOwnerThrowable(Dynamic<?> pThrowableTag) {
        String s = "owner";
        OptionalDynamic<?> optionaldynamic = pThrowableTag.get("owner");
        long i = optionaldynamic.get("M").asLong(0L);
        long j = optionaldynamic.get("L").asLong(0L);
        return this.setUUID(pThrowableTag, i, j).remove("owner");
    }

    private Dynamic<?> setUUID(Dynamic<?> pDynamic, long pUuidMost, long pUuidLeast) {
        String s = "OwnerUUID";
        return pUuidMost != 0L && pUuidLeast != 0L ? pDynamic.set("OwnerUUID", pDynamic.createIntList(Arrays.stream(createUUIDArray(pUuidMost, pUuidLeast)))) : pDynamic;
    }

    private static int[] createUUIDArray(long pUuidMost, long pUuidLeast) {
        return new int[]{(int)(pUuidMost >> 32), (int)pUuidMost, (int)(pUuidLeast >> 32), (int)pUuidLeast};
    }

    private Typed<?> updateEntity(Typed<?> pTyped, String pChoiceName, Function<Dynamic<?>, Dynamic<?>> pUpdater) {
        Type<?> type = this.getInputSchema().getChoiceType(References.ENTITY, pChoiceName);
        Type<?> type1 = this.getOutputSchema().getChoiceType(References.ENTITY, pChoiceName);
        return pTyped.updateTyped(DSL.namedChoice(pChoiceName, type), type1, p_15576_ -> p_15576_.update(DSL.remainderFinder(), pUpdater));
    }
}