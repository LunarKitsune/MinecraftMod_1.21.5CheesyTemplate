package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;

public class InvalidLockComponentFix extends DataComponentRemainderFix {
    private static final Optional<String> INVALID_LOCK_CUSTOM_NAME = Optional.of("\"\"");

    public InvalidLockComponentFix(Schema pOutputSchema) {
        super(pOutputSchema, "InvalidLockComponentPredicateFix", "minecraft:lock");
    }

    @Nullable
    @Override
    protected <T> Dynamic<T> fixComponent(Dynamic<T> p_377274_) {
        return fixLock(p_377274_);
    }

    @Nullable
    public static <T> Dynamic<T> fixLock(Dynamic<T> pTag) {
        return isBrokenLock(pTag) ? null : pTag;
    }

    private static <T> boolean isBrokenLock(Dynamic<T> pTag) {
        return isMapWithOneField(
            pTag, "components", p_378206_ -> isMapWithOneField(p_378206_, "minecraft:custom_name", p_377439_ -> p_377439_.asString().result().equals(INVALID_LOCK_CUSTOM_NAME))
        );
    }

    private static <T> boolean isMapWithOneField(Dynamic<T> pTag, String pKey, Predicate<Dynamic<T>> pPredicate) {
        Optional<Map<Dynamic<T>, Dynamic<T>>> optional = pTag.getMapValues().result();
        return !optional.isEmpty() && optional.get().size() == 1 ? pTag.get(pKey).result().filter(pPredicate).isPresent() : false;
    }
}