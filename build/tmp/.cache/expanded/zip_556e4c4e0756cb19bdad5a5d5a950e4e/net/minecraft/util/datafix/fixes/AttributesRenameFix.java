package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class AttributesRenameFix extends DataFix {
    private final String name;
    private final UnaryOperator<String> renames;

    public AttributesRenameFix(Schema pOutputSchema, String pName, UnaryOperator<String> pRenames) {
        super(pOutputSchema, false);
        this.name = pName;
        this.renames = pRenames;
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return TypeRewriteRule.seq(
            this.fixTypeEverywhereTyped(this.name + " (Components)", this.getInputSchema().getType(References.DATA_COMPONENTS), this::fixDataComponents),
            this.fixTypeEverywhereTyped(this.name + " (Entity)", this.getInputSchema().getType(References.ENTITY), this::fixEntity),
            this.fixTypeEverywhereTyped(this.name + " (Player)", this.getInputSchema().getType(References.PLAYER), this::fixEntity)
        );
    }

    private Typed<?> fixDataComponents(Typed<?> pDataComponents) {
        return pDataComponents.update(
            DSL.remainderFinder(),
            p_366382_ -> p_366382_.update(
                "minecraft:attribute_modifiers",
                p_369365_ -> p_369365_.update(
                    "modifiers",
                    p_366575_ -> DataFixUtils.orElse(
                        p_366575_.asStreamOpt().result().map(p_363756_ -> p_363756_.map(this::fixTypeField)).map(p_366575_::createList), p_366575_
                    )
                )
            )
        );
    }

    private Typed<?> fixEntity(Typed<?> pData) {
        return pData.update(
            DSL.remainderFinder(),
            p_362710_ -> p_362710_.update(
                "attributes",
                p_366472_ -> DataFixUtils.orElse(
                    p_366472_.asStreamOpt().result().map(p_362930_ -> p_362930_.map(this::fixIdField)).map(p_366472_::createList), p_366472_
                )
            )
        );
    }

    private Dynamic<?> fixIdField(Dynamic<?> pData) {
        return ExtraDataFixUtils.fixStringField(pData, "id", this.renames);
    }

    private Dynamic<?> fixTypeField(Dynamic<?> pData) {
        return ExtraDataFixUtils.fixStringField(pData, "type", this.renames);
    }
}