package net.minecraft.util.datafix.fixes;

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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class TextComponentHoverAndClickEventFix extends DataFix {
    public TextComponentHoverAndClickEventFix(Schema pOutputSchema) {
        super(pOutputSchema, true);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<? extends Pair<String, ?>> type = (Type<? extends Pair<String, ?>>)this.getInputSchema().getType(References.TEXT_COMPONENT).findFieldType("hoverEvent");
        return this.createFixer(this.getInputSchema().getTypeRaw(References.TEXT_COMPONENT), this.getOutputSchema().getType(References.TEXT_COMPONENT), type);
    }

    private <C1, C2, H extends Pair<String, ?>> TypeRewriteRule createFixer(Type<C1> pInputComponentType, Type<C2> pOutputComponentType, Type<H> pHoverEventType) {
        Type<Pair<String, Either<Either<String, List<C1>>, Pair<Either<List<C1>, Unit>, Pair<Either<C1, Unit>, Pair<Either<H, Unit>, Dynamic<?>>>>>>> type = DSL.named(
            References.TEXT_COMPONENT.typeName(),
            DSL.or(
                DSL.or(DSL.string(), DSL.list(pInputComponentType)),
                DSL.and(
                    DSL.optional(DSL.field("extra", DSL.list(pInputComponentType))),
                    DSL.optional(DSL.field("separator", pInputComponentType)),
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
            Type<?> type1 = ExtraDataFixUtils.patchSubType(type, type, pOutputComponentType);
            return this.fixTypeEverywhere(
                "TextComponentHoverAndClickEventFix",
                type,
                pOutputComponentType,
                p_392390_ -> p_396871_ -> {
                    boolean flag = p_396871_.getSecond().map(p_396868_ -> false, p_392755_ -> {
                        Pair<Either<H, Unit>, Dynamic<?>> pair = p_392755_.getSecond().getSecond();
                        boolean flag1 = pair.getFirst().left().isPresent();
                        boolean flag2 = pair.getSecond().get("clickEvent").result().isPresent();
                        return flag1 || flag2;
                    });
                    return (C2)(!flag
                        ? p_396871_
                        : Util.writeAndReadTypedOrThrow(ExtraDataFixUtils.cast(type1, p_396871_, p_392390_), pOutputComponentType, TextComponentHoverAndClickEventFix::fixTextComponent)
                            .getValue());
                }
            );
        }
    }

    private static Dynamic<?> fixTextComponent(Dynamic<?> pData) {
        return pData.renameAndFixField("hoverEvent", "hover_event", TextComponentHoverAndClickEventFix::fixHoverEvent)
            .renameAndFixField("clickEvent", "click_event", TextComponentHoverAndClickEventFix::fixClickEvent);
    }

    private static Dynamic<?> copyFields(Dynamic<?> pNewData, Dynamic<?> pOldData, String... pFields) {
        for (String s : pFields) {
            pNewData = Dynamic.copyField(pOldData, s, pNewData, s);
        }

        return pNewData;
    }

    private static Dynamic<?> fixHoverEvent(Dynamic<?> pData) {
        String s = pData.get("action").asString("");

        return switch (s) {
            case "show_text" -> pData.renameField("contents", "value");
            case "show_item" -> {
                Dynamic<?> dynamic1 = pData.get("contents").orElseEmptyMap();
                Optional<String> optional = dynamic1.asString().result();
                yield optional.isPresent()
                    ? pData.renameField("contents", "id")
                    : copyFields(pData.remove("contents"), dynamic1, "id", "count", "components");
            }
            case "show_entity" -> {
                Dynamic<?> dynamic = pData.get("contents").orElseEmptyMap();
                yield copyFields(pData.remove("contents"), dynamic, "id", "type", "name").renameField("id", "uuid").renameField("type", "id");
            }
            default -> pData;
        };
    }

    @Nullable
    private static <T> Dynamic<T> fixClickEvent(Dynamic<T> pData) {
        String s = pData.get("action").asString("");
        String s1 = pData.get("value").asString("");

        return switch (s) {
            case "open_url" -> !validateUri(s1) ? null : pData.renameField("value", "url");
            case "open_file" -> pData.renameField("value", "path");
            case "run_command", "suggest_command" -> !validateChat(s1) ? null : pData.renameField("value", "command");
            case "change_page" -> {
                Integer integer = pData.get("value").result().map(TextComponentHoverAndClickEventFix::parseOldPage).orElse(null);
                if (integer == null) {
                    yield null;
                } else {
                    int i = Math.max(integer, 1);
                    yield pData.remove("value").set("page", pData.createInt(i));
                }
            }
            default -> pData;
        };
    }

    @Nullable
    private static Integer parseOldPage(Dynamic<?> pData) {
        Optional<Number> optional = pData.asNumber().result();
        if (optional.isPresent()) {
            return optional.get().intValue();
        } else {
            try {
                return Integer.parseInt(pData.asString(""));
            } catch (Exception exception) {
                return null;
            }
        }
    }

    private static boolean validateUri(String pUri) {
        try {
            URI uri = new URI(pUri);
            String s = uri.getScheme();
            if (s == null) {
                return false;
            } else {
                String s1 = s.toLowerCase(Locale.ROOT);
                return "http".equals(s1) || "https".equals(s1);
            }
        } catch (URISyntaxException urisyntaxexception) {
            return false;
        }
    }

    private static boolean validateChat(String pChat) {
        for (int i = 0; i < pChat.length(); i++) {
            char c0 = pChat.charAt(i);
            if (c0 == 167 || c0 < ' ' || c0 == 127) {
                return false;
            }
        }

        return true;
    }
}