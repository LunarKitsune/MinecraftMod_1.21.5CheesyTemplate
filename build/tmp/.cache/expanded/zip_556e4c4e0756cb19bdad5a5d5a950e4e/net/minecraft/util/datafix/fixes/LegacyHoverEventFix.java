package net.minecraft.util.datafix.fixes;

import com.google.gson.JsonElement;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.util.GsonHelper;

public class LegacyHoverEventFix extends DataFix {
    public LegacyHoverEventFix(Schema pOutputSchema) {
        super(pOutputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<? extends Pair<String, ?>> type = (Type<? extends Pair<String, ?>>)this.getInputSchema().getType(References.TEXT_COMPONENT).findFieldType("hoverEvent");
        return this.createFixer(this.getInputSchema().getTypeRaw(References.TEXT_COMPONENT), type);
    }

    private <C, H extends Pair<String, ?>> TypeRewriteRule createFixer(Type<C> pComponentType, Type<H> pHoverEventType) {
        Type<Pair<String, Either<Either<String, List<C>>, Pair<Either<List<C>, Unit>, Pair<Either<C, Unit>, Pair<Either<H, Unit>, Dynamic<?>>>>>>> type = DSL.named(
            References.TEXT_COMPONENT.typeName(),
            DSL.or(
                DSL.or(DSL.string(), DSL.list(pComponentType)),
                DSL.and(
                    DSL.optional(DSL.field("extra", DSL.list(pComponentType))),
                    DSL.optional(DSL.field("separator", pComponentType)),
                    DSL.optional(DSL.field("hoverEvent", pHoverEventType)),
                    DSL.remainderType()
                )
            )
        );
        if (!type.equals(this.getInputSchema().getType(References.TEXT_COMPONENT))) {
            throw new IllegalStateException(
                "Text component type did not match, expected " + type + " but got " + this.getInputSchema().getType(References.TEXT_COMPONENT)
            );
        } else {
            return this.fixTypeEverywhere(
                "LegacyHoverEventFix",
                type,
                p_394382_ -> p_395778_ -> p_395778_.mapSecond(
                    p_391228_ -> p_391228_.mapRight(p_395158_ -> p_395158_.mapSecond(p_395579_ -> p_395579_.mapSecond(p_395788_ -> {
                        Dynamic<?> dynamic = p_395788_.getSecond();
                        Optional<? extends Dynamic<?>> optional = dynamic.get("hoverEvent").result();
                        if (optional.isEmpty()) {
                            return p_395788_;
                        } else {
                            Optional<? extends Dynamic<?>> optional1 = optional.get().get("value").result();
                            if (optional1.isEmpty()) {
                                return p_395788_;
                            } else {
                                String s = p_395788_.getFirst().left().map(Pair::getFirst).orElse("");
                                H h = this.fixHoverEvent(pHoverEventType, s, (Dynamic<?>)optional.get());
                                return p_395788_.mapFirst(p_391455_ -> Either.left(h));
                            }
                        }
                    })))
                )
            );
        }
    }

    private <H> H fixHoverEvent(Type<H> pType, String pAction, Dynamic<?> pData) {
        return "show_text".equals(pAction) ? fixShowTextHover(pType, pData) : createPlaceholderHover(pType, pData);
    }

    private static <H> H fixShowTextHover(Type<H> pType, Dynamic<?> pData) {
        Dynamic<?> dynamic = pData.renameField("value", "contents");
        return Util.readTypedOrThrow(pType, dynamic).getValue();
    }

    private static <H> H createPlaceholderHover(Type<H> pType, Dynamic<?> pData) {
        JsonElement jsonelement = pData.convert(JsonOps.INSTANCE).getValue();
        Dynamic<?> dynamic = new Dynamic<>(
            JavaOps.INSTANCE,
            Map.of("action", "show_text", "contents", Map.<String, String>of("text", "Legacy hoverEvent: " + GsonHelper.toStableString(jsonelement)))
        );
        return Util.readTypedOrThrow(pType, dynamic).getValue();
    }
}