package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntitySpawnerItemVariantComponentFix extends DataFix {
    public EntitySpawnerItemVariantComponentFix(Schema pOutputSchema) {
        super(pOutputSchema, false);
    }

    @Override
    public final TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder<Pair<String, String>> opticfinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        OpticFinder<?> opticfinder1 = type.findField("components");
        return this.fixTypeEverywhereTyped(
            "ItemStack bucket_entity_data variants to separate components",
            type,
            p_392674_ -> {
                String s = p_392674_.getOptional(opticfinder).map(Pair::getSecond).orElse("");

                return switch (s) {
                    case "minecraft:salmon_bucket" -> p_392674_.updateTyped(opticfinder1, (Fixer)EntitySpawnerItemVariantComponentFix::fixSalmonBucket);
                    case "minecraft:axolotl_bucket" -> p_392674_.updateTyped(opticfinder1, (Fixer)EntitySpawnerItemVariantComponentFix::fixAxolotlBucket);
                    case "minecraft:tropical_fish_bucket" -> p_392674_.updateTyped(opticfinder1, (Fixer)EntitySpawnerItemVariantComponentFix::fixTropicalFishBucket);
                    case "minecraft:painting" -> p_392674_.updateTyped(
                        opticfinder1, p_395765_ -> Util.writeAndReadTypedOrThrow(p_395765_, p_395765_.getType(), EntitySpawnerItemVariantComponentFix::fixPainting)
                    );
                    default -> p_392674_;
                };
            }
        );
    }

    private static String getBaseColor(int pVariant) {
        return ExtraDataFixUtils.dyeColorIdToName(pVariant >> 16 & 0xFF);
    }

    private static String getPatternColor(int pVariant) {
        return ExtraDataFixUtils.dyeColorIdToName(pVariant >> 24 & 0xFF);
    }

    private static String getPattern(int pVariant) {
        return switch (pVariant & 65535) {
            case 1 -> "flopper";
            case 256 -> "sunstreak";
            case 257 -> "stripey";
            case 512 -> "snooper";
            case 513 -> "glitter";
            case 768 -> "dasher";
            case 769 -> "blockfish";
            case 1024 -> "brinely";
            case 1025 -> "betty";
            case 1280 -> "spotty";
            case 1281 -> "clayfish";
            default -> "kob";
        };
    }

    private static <T> Dynamic<T> fixTropicalFishBucket(Dynamic<T> pData, Dynamic<T> pEntityData) {
        Optional<Number> optional = pEntityData.get("BucketVariantTag").asNumber().result();
        if (optional.isEmpty()) {
            return pData;
        } else {
            int i = optional.get().intValue();
            String s = getPattern(i);
            String s1 = getBaseColor(i);
            String s2 = getPatternColor(i);
            return pData.update("minecraft:bucket_entity_data", p_397862_ -> p_397862_.remove("BucketVariantTag"))
                .set("minecraft:tropical_fish/pattern", pData.createString(s))
                .set("minecraft:tropical_fish/base_color", pData.createString(s1))
                .set("minecraft:tropical_fish/pattern_color", pData.createString(s2));
        }
    }

    private static <T> Dynamic<T> fixAxolotlBucket(Dynamic<T> pData, Dynamic<T> pEntityData) {
        Optional<Number> optional = pEntityData.get("Variant").asNumber().result();
        if (optional.isEmpty()) {
            return pData;
        } else {
            String s = switch (optional.get().intValue()) {
                case 1 -> "wild";
                case 2 -> "gold";
                case 3 -> "cyan";
                case 4 -> "blue";
                default -> "lucy";
            };
            return pData.update("minecraft:bucket_entity_data", p_395620_ -> p_395620_.remove("Variant"))
                .set("minecraft:axolotl/variant", pData.createString(s));
        }
    }

    private static <T> Dynamic<T> fixSalmonBucket(Dynamic<T> pData, Dynamic<T> pEntityData) {
        Optional<Dynamic<T>> optional = pEntityData.get("type").result();
        return optional.isEmpty()
            ? pData
            : pData.update("minecraft:bucket_entity_data", p_394947_ -> p_394947_.remove("type")).set("minecraft:salmon/size", optional.get());
    }

    private static <T> Dynamic<T> fixPainting(Dynamic<T> pData) {
        Optional<Dynamic<T>> optional = pData.get("minecraft:entity_data").result();
        if (optional.isEmpty()) {
            return pData;
        } else if (optional.get().get("id").asString().result().filter(p_391705_ -> p_391705_.equals("minecraft:painting")).isEmpty()) {
            return pData;
        } else {
            Optional<Dynamic<T>> optional1 = optional.get().get("variant").result();
            Dynamic<T> dynamic = optional.get().remove("variant");
            if (dynamic.remove("id").equals(dynamic.emptyMap())) {
                pData = pData.remove("minecraft:entity_data");
            } else {
                pData = pData.set("minecraft:entity_data", dynamic);
            }

            if (optional1.isPresent()) {
                pData = pData.set("minecraft:painting/variant", optional1.get());
            }

            return pData;
        }
    }

    @FunctionalInterface
    interface Fixer extends Function<Typed<?>, Typed<?>> {
        default Typed<?> apply(Typed<?> p_393786_) {
            return p_393786_.update(DSL.remainderFinder(), this::fixRemainder);
        }

        default <T> Dynamic<T> fixRemainder(Dynamic<T> pData) {
            return pData.get("minecraft:bucket_entity_data").result().map(p_397629_ -> this.fixRemainder(pData, (Dynamic<T>)p_397629_)).orElse(pData);
        }

        <T> Dynamic<T> fixRemainder(Dynamic<T> pData, Dynamic<T> pEntityData);
    }
}