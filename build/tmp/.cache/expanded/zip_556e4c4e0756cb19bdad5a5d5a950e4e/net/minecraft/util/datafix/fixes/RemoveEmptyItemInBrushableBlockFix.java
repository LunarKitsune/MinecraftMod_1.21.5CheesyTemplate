package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class RemoveEmptyItemInBrushableBlockFix extends NamedEntityWriteReadFix {
    public RemoveEmptyItemInBrushableBlockFix(Schema pOutputSchema) {
        super(pOutputSchema, false, "RemoveEmptyItemInSuspiciousBlockFix", References.BLOCK_ENTITY, "minecraft:brushable_block");
    }

    @Override
    protected <T> Dynamic<T> fix(Dynamic<T> p_330310_) {
        Optional<Dynamic<T>> optional = p_330310_.get("item").result();
        return optional.isPresent() && isEmptyStack(optional.get()) ? p_330310_.remove("item") : p_330310_;
    }

    private static boolean isEmptyStack(Dynamic<?> pTag) {
        String s = NamespacedSchema.ensureNamespaced(pTag.get("id").asString("minecraft:air"));
        int i = pTag.get("count").asInt(0);
        return s.equals("minecraft:air") || i == 0;
    }
}