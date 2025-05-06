package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.stream.Stream;

public class IglooMetadataRemovalFix extends DataFix {
    public IglooMetadataRemovalFix(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.STRUCTURE_FEATURE);
        return this.fixTypeEverywhereTyped(
            "IglooMetadataRemovalFix", type, p_274928_ -> p_274928_.update(DSL.remainderFinder(), IglooMetadataRemovalFix::fixTag)
        );
    }

    private static <T> Dynamic<T> fixTag(Dynamic<T> pTag) {
        boolean flag = pTag.get("Children").asStreamOpt().map(p_15911_ -> p_15911_.allMatch(IglooMetadataRemovalFix::isIglooPiece)).result().orElse(false);
        return flag ? pTag.set("id", pTag.createString("Igloo")).remove("Children") : pTag.update("Children", IglooMetadataRemovalFix::removeIglooPieces);
    }

    private static <T> Dynamic<T> removeIglooPieces(Dynamic<T> pDynamic) {
        return pDynamic.asStreamOpt()
            .map(p_15907_ -> p_15907_.filter(p_145382_ -> !isIglooPiece((Dynamic<?>)p_145382_)))
            .map(pDynamic::createList)
            .result()
            .orElse(pDynamic);
    }

    private static boolean isIglooPiece(Dynamic<?> pDynamic) {
        return pDynamic.get("id").asString("").equals("Iglu");
    }
}