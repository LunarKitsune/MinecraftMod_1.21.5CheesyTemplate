package net.minecraft.locale;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

public record DeprecatedTranslationsInfo(List<String> removed, Map<String, String> renamed) {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeprecatedTranslationsInfo EMPTY = new DeprecatedTranslationsInfo(List.of(), Map.of());
    public static final Codec<DeprecatedTranslationsInfo> CODEC = RecordCodecBuilder.create(
        p_368182_ -> p_368182_.group(
                Codec.STRING.listOf().fieldOf("removed").forGetter(DeprecatedTranslationsInfo::removed),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("renamed").forGetter(DeprecatedTranslationsInfo::renamed)
            )
            .apply(p_368182_, DeprecatedTranslationsInfo::new)
    );

    public static DeprecatedTranslationsInfo loadFromJson(InputStream pInputStream) {
        JsonElement jsonelement = JsonParser.parseReader(new InputStreamReader(pInputStream, StandardCharsets.UTF_8));
        return CODEC.parse(JsonOps.INSTANCE, jsonelement)
            .getOrThrow(p_370184_ -> new IllegalStateException("Failed to parse deprecated language data: " + p_370184_));
    }

    public static DeprecatedTranslationsInfo loadFromResource(String pName) {
        try (InputStream inputstream = Language.class.getResourceAsStream(pName)) {
            return inputstream != null ? loadFromJson(inputstream) : EMPTY;
        } catch (Exception exception) {
            LOGGER.error("Failed to read {}", pName, exception);
            return EMPTY;
        }
    }

    public static DeprecatedTranslationsInfo loadFromDefaultResource() {
        return loadFromResource("/assets/minecraft/lang/deprecated.json");
    }

    public void applyToMap(Map<String, String> pTranslations) {
        for (String s : this.removed) {
            pTranslations.remove(s);
        }

        this.renamed.forEach((p_363113_, p_364770_) -> {
            String s1 = pTranslations.remove(p_363113_);
            if (s1 == null) {
                LOGGER.warn("Missing translation key for rename: {}", p_363113_);
                pTranslations.remove(p_364770_);
            } else {
                pTranslations.put(p_364770_, s1);
            }
        });
    }
}