package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Streams;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class DropInvalidSignDataFix extends DataFix {
    private final String entityName;

    public DropInvalidSignDataFix(Schema pOutputSchema, String pEntityName) {
        super(pOutputSchema, false);
        this.entityName = pEntityName;
    }

    private <T> Dynamic<T> fix(Dynamic<T> pData) {
        pData = pData.update("front_text", DropInvalidSignDataFix::fixText);
        pData = pData.update("back_text", DropInvalidSignDataFix::fixText);

        for (String s : BlockEntitySignDoubleSidedEditableTextFix.FIELDS_TO_DROP) {
            pData = pData.remove(s);
        }

        return pData;
    }

    private static <T> Dynamic<T> fixText(Dynamic<T> pTextDynamic) {
        Optional<Stream<Dynamic<T>>> optional = pTextDynamic.get("filtered_messages").asStreamOpt().result();
        if (optional.isEmpty()) {
            return pTextDynamic;
        } else {
            Dynamic<T> dynamic = LegacyComponentDataFixUtils.createEmptyComponent(pTextDynamic.getOps());
            List<Dynamic<T>> list = pTextDynamic.get("messages").asStreamOpt().result().orElse(Stream.of()).toList();
            List<Dynamic<T>> list1 = Streams.mapWithIndex(optional.get(), (p_298117_, p_298041_) -> {
                Dynamic<T> dynamic1 = p_298041_ < list.size() ? list.get((int)p_298041_) : dynamic;
                return p_298117_.equals(dynamic) ? dynamic1 : p_298117_;
            }).toList();
            return list1.equals(list) ? pTextDynamic.remove("filtered_messages") : pTextDynamic.set("filtered_messages", pTextDynamic.createList(list1.stream()));
        }
    }

    @Override
    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.BLOCK_ENTITY);
        Type<?> type1 = this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, this.entityName);
        OpticFinder<?> opticfinder = DSL.namedChoice(this.entityName, type1);
        return this.fixTypeEverywhereTyped(
            "DropInvalidSignDataFix for " + this.entityName,
            type,
            p_390233_ -> p_390233_.updateTyped(
                opticfinder,
                type1,
                p_390230_ -> {
                    boolean flag = p_390230_.get(DSL.remainderFinder()).get("_filtered_correct").asBoolean(false);
                    return flag
                        ? p_390230_.update(DSL.remainderFinder(), p_390228_ -> p_390228_.remove("_filtered_correct"))
                        : Util.writeAndReadTypedOrThrow(p_390230_, type1, this::fix);
                }
            )
        );
    }
}