package net.minecraft.util.datafix;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.util.GsonHelper;

public class LegacyComponentDataFixUtils {
    private static final String EMPTY_CONTENTS = createTextComponentJson("");

    public static <T> Dynamic<T> createPlainTextComponent(DynamicOps<T> pOps, String pData) {
        String s = createTextComponentJson(pData);
        return new Dynamic<>(pOps, pOps.createString(s));
    }

    public static <T> Dynamic<T> createEmptyComponent(DynamicOps<T> pOps) {
        return new Dynamic<>(pOps, pOps.createString(EMPTY_CONTENTS));
    }

    public static String createTextComponentJson(String pJson) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("text", pJson);
        return GsonHelper.toStableString(jsonobject);
    }

    public static String createTranslatableComponentJson(String pJson) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("translate", pJson);
        return GsonHelper.toStableString(jsonobject);
    }

    public static <T> Dynamic<T> createTranslatableComponent(DynamicOps<T> pOps, String pData) {
        String s = createTranslatableComponentJson(pData);
        return new Dynamic<>(pOps, pOps.createString(s));
    }

    public static String rewriteFromLenient(String pData) {
        if (!pData.isEmpty() && !pData.equals("null")) {
            char c0 = pData.charAt(0);
            char c1 = pData.charAt(pData.length() - 1);
            if (c0 == '"' && c1 == '"' || c0 == '{' && c1 == '}' || c0 == '[' && c1 == ']') {
                try {
                    JsonElement jsonelement = JsonParser.parseString(pData);
                    if (jsonelement.isJsonPrimitive()) {
                        return createTextComponentJson(jsonelement.getAsString());
                    }

                    return GsonHelper.toStableString(jsonelement);
                } catch (JsonParseException jsonparseexception) {
                }
            }

            return createTextComponentJson(pData);
        } else {
            return EMPTY_CONTENTS;
        }
    }

    public static Optional<String> extractTranslationString(String pData) {
        try {
            JsonElement jsonelement = JsonParser.parseString(pData);
            if (jsonelement.isJsonObject()) {
                JsonObject jsonobject = jsonelement.getAsJsonObject();
                JsonElement jsonelement1 = jsonobject.get("translate");
                if (jsonelement1 != null && jsonelement1.isJsonPrimitive()) {
                    return Optional.of(jsonelement1.getAsString());
                }
            }
        } catch (JsonParseException jsonparseexception) {
        }

        return Optional.empty();
    }
}