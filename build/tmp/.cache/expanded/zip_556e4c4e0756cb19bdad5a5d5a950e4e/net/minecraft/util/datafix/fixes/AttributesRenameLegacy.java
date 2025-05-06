package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class AttributesRenameLegacy extends DataFix {
    private final String name;
    private final UnaryOperator<String> renames;

    public AttributesRenameLegacy(Schema pOutputSchema, String pName, UnaryOperator<String> pRenames) {
        super(pOutputSchema, false);
        this.name = pName;
        this.renames = pRenames;
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder<?> opticfinder = type.findField("tag");
        return TypeRewriteRule.seq(
            this.fixTypeEverywhereTyped(this.name + " (ItemStack)", type, p_361468_ -> p_361468_.updateTyped(opticfinder, this::fixItemStackTag)),
            this.fixTypeEverywhereTyped(this.name + " (Entity)", this.getInputSchema().getType(References.ENTITY), this::fixEntity),
            this.fixTypeEverywhereTyped(this.name + " (Player)", this.getInputSchema().getType(References.PLAYER), this::fixEntity)
        );
    }

    private Dynamic<?> fixName(Dynamic<?> pName) {
        return DataFixUtils.orElse(pName.asString().result().map(this.renames).map(pName::createString), pName);
    }

    private Typed<?> fixItemStackTag(Typed<?> pItemStackTag) {
        return pItemStackTag.update(
            DSL.remainderFinder(),
            p_365782_ -> p_365782_.update(
                "AttributeModifiers",
                p_361291_ -> DataFixUtils.orElse(
                    p_361291_.asStreamOpt()
                        .result()
                        .map(p_368448_ -> p_368448_.map(p_363415_ -> p_363415_.update("AttributeName", this::fixName)))
                        .map(p_361291_::createList),
                    p_361291_
                )
            )
        );
    }

    private Typed<?> fixEntity(Typed<?> pEntityTag) {
        return pEntityTag.update(
            DSL.remainderFinder(),
            p_370146_ -> p_370146_.update(
                "Attributes",
                p_369341_ -> DataFixUtils.orElse(
                    p_369341_.asStreamOpt()
                        .result()
                        .map(p_361263_ -> p_361263_.map(p_362038_ -> p_362038_.update("Name", this::fixName)))
                        .map(p_369341_::createList),
                    p_369341_
                )
            )
        );
    }
}