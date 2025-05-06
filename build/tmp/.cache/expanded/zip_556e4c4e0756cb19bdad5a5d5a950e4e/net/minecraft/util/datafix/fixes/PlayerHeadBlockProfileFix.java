package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class PlayerHeadBlockProfileFix extends NamedEntityFix {
    public PlayerHeadBlockProfileFix(Schema pOutputSchema) {
        super(pOutputSchema, false, "PlayerHeadBlockProfileFix", References.BLOCK_ENTITY, "minecraft:skull");
    }

    @Override
    protected Typed<?> fix(Typed<?> p_332910_) {
        return p_332910_.update(DSL.remainderFinder(), this::fix);
    }

    private <T> Dynamic<T> fix(Dynamic<T> pTag) {
        Optional<Dynamic<T>> optional = pTag.get("SkullOwner").result();
        Optional<Dynamic<T>> optional1 = pTag.get("ExtraType").result();
        Optional<Dynamic<T>> optional2 = optional.or(() -> optional1);
        if (optional2.isEmpty()) {
            return pTag;
        } else {
            pTag = pTag.remove("SkullOwner").remove("ExtraType");
            return pTag.set("profile", ItemStackComponentizationFix.fixProfile(optional2.get()));
        }
    }
}