package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.stream.Stream;

public class BlockEntityBannerColorFix extends NamedEntityFix {
    public BlockEntityBannerColorFix(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType, "BlockEntityBannerColorFix", References.BLOCK_ENTITY, "minecraft:banner");
    }

    public Dynamic<?> fixTag(Dynamic<?> pTag) {
        pTag = pTag.update("Base", p_14808_ -> p_14808_.createInt(15 - p_14808_.asInt(0)));
        return pTag.update(
            "Patterns",
            p_326553_ -> DataFixUtils.orElse(
                p_326553_.asStreamOpt()
                    .map(p_145125_ -> p_145125_.map(p_145127_ -> p_145127_.update("Color", p_145129_ -> p_145129_.createInt(15 - p_145129_.asInt(0)))))
                    .map(p_326553_::createList)
                    .result(),
                p_326553_
            )
        );
    }

    @Override
    protected Typed<?> fix(Typed<?> p_14796_) {
        return p_14796_.update(DSL.remainderFinder(), this::fixTag);
    }
}