package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Streams;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class BlockEntitySignDoubleSidedEditableTextFix extends NamedEntityWriteReadFix {
    public static final List<String> FIELDS_TO_DROP = List.of(
        "Text1", "Text2", "Text3", "Text4", "FilteredText1", "FilteredText2", "FilteredText3", "FilteredText4", "Color", "GlowingText"
    );
    public static final String FILTERED_CORRECT = "_filtered_correct";
    private static final String DEFAULT_COLOR = "black";

    public BlockEntitySignDoubleSidedEditableTextFix(Schema pOutputSchema, String pName, String pEntityName) {
        super(pOutputSchema, true, pName, References.BLOCK_ENTITY, pEntityName);
    }

    @Override
    protected <T> Dynamic<T> fix(Dynamic<T> p_393570_) {
        p_393570_ = p_393570_.set("front_text", fixFrontTextTag(p_393570_))
            .set("back_text", createDefaultText(p_393570_))
            .set("is_waxed", p_393570_.createBoolean(false))
            .set("_filtered_correct", p_393570_.createBoolean(true));

        for (String s : FIELDS_TO_DROP) {
            p_393570_ = p_393570_.remove(s);
        }

        return p_393570_;
    }

    private static <T> Dynamic<T> fixFrontTextTag(Dynamic<T> pTag) {
        Dynamic<T> dynamic = LegacyComponentDataFixUtils.createEmptyComponent(pTag.getOps());
        List<Dynamic<T>> list = getLines(pTag, "Text").map(p_297945_ -> p_297945_.orElse(dynamic)).toList();
        Dynamic<T> dynamic1 = pTag.emptyMap()
            .set("messages", pTag.createList(list.stream()))
            .set("color", pTag.get("Color").result().orElse(pTag.createString("black")))
            .set("has_glowing_text", pTag.get("GlowingText").result().orElse(pTag.createBoolean(false)));
        List<Optional<Dynamic<T>>> list1 = getLines(pTag, "FilteredText").toList();
        if (list1.stream().anyMatch(Optional::isPresent)) {
            dynamic1 = dynamic1.set("filtered_messages", pTag.createList(Streams.mapWithIndex(list1.stream(), (p_299542_, p_300269_) -> {
                Dynamic<T> dynamic2 = list.get((int)p_300269_);
                return p_299542_.orElse(dynamic2);
            })));
        }

        return dynamic1;
    }

    private static <T> Stream<Optional<Dynamic<T>>> getLines(Dynamic<T> pDynamic, String pPrefix) {
        return Stream.of(
            pDynamic.get(pPrefix + "1").result(),
            pDynamic.get(pPrefix + "2").result(),
            pDynamic.get(pPrefix + "3").result(),
            pDynamic.get(pPrefix + "4").result()
        );
    }

    private static <T> Dynamic<T> createDefaultText(Dynamic<T> pDynamic) {
        return pDynamic.emptyMap()
            .set("messages", createEmptyLines(pDynamic))
            .set("color", pDynamic.createString("black"))
            .set("has_glowing_text", pDynamic.createBoolean(false));
    }

    private static <T> Dynamic<T> createEmptyLines(Dynamic<T> pDynamic) {
        Dynamic<T> dynamic = LegacyComponentDataFixUtils.createEmptyComponent(pDynamic.getOps());
        return pDynamic.createList(Stream.of(dynamic, dynamic, dynamic, dynamic));
    }
}