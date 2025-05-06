package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.Util;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntityCustomNameToComponentFix extends DataFix {
    public EntityCustomNameToComponentFix(Schema pOutputSchema) {
        super(pOutputSchema, true);
    }

    @Override
    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.ENTITY);
        Type<?> type1 = this.getOutputSchema().getType(References.ENTITY);
        OpticFinder<String> opticfinder = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
        OpticFinder<String> opticfinder1 = (OpticFinder<String>)type.findField("CustomName");
        Type<?> type2 = type1.findFieldType("CustomName");
        return this.fixTypeEverywhereTyped("EntityCustomNameToComponentFix", type, type1, p_390241_ -> fixEntity(p_390241_, opticfinder, opticfinder1, type2));
    }

    private static <T> Typed<?> fixEntity(Typed<?> pData, OpticFinder<String> pCustomNameOptic, OpticFinder<String> pIdOptic, Type<T> pNewType) {
        return pData.update(pIdOptic, pNewType, p_390237_ -> {
            String s = pData.getOptional(pCustomNameOptic).orElse("");
            Dynamic<?> dynamic = fixCustomName(pData.getOps(), p_390237_, s);
            return Util.readTypedOrThrow(pNewType, dynamic).getValue();
        });
    }

    private static <T> Dynamic<T> fixCustomName(DynamicOps<T> pOps, String pCustomName, String pId) {
        return "minecraft:commandblock_minecart".equals(pId)
            ? new Dynamic<>(pOps, pOps.createString(pCustomName))
            : LegacyComponentDataFixUtils.createPlainTextComponent(pOps, pCustomName);
    }
}