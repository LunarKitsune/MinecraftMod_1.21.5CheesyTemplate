package net.minecraft.util.datafix.fixes;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import javax.annotation.Nullable;

public class LockComponentPredicateFix extends DataComponentRemainderFix {
    public static final Escaper ESCAPER = Escapers.builder().addEscape('"', "\\\"").addEscape('\\', "\\\\").build();

    public LockComponentPredicateFix(Schema pOutputSchema) {
        super(pOutputSchema, "LockComponentPredicateFix", "minecraft:lock");
    }

    @Nullable
    @Override
    protected <T> Dynamic<T> fixComponent(Dynamic<T> p_360989_) {
        return fixLock(p_360989_);
    }

    @Nullable
    public static <T> Dynamic<T> fixLock(Dynamic<T> pTag) {
        Optional<String> optional = pTag.asString().result();
        if (optional.isEmpty()) {
            return null;
        } else if (optional.get().isEmpty()) {
            return null;
        } else {
            Dynamic<T> dynamic = pTag.createString("\"" + ESCAPER.escape(optional.get()) + "\"");
            Dynamic<T> dynamic1 = pTag.emptyMap().set("minecraft:custom_name", dynamic);
            return pTag.emptyMap().set("components", dynamic1);
        }
    }
}