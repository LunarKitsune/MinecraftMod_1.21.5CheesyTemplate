package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public abstract class AbstractBlockPropertyFix extends DataFix {
    private final String name;

    public AbstractBlockPropertyFix(Schema pOutputSchema, String pName) {
        super(pOutputSchema, false);
        this.name = pName;
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            this.name, this.getInputSchema().getType(References.BLOCK_STATE), p_393544_ -> p_393544_.update(DSL.remainderFinder(), this::fixBlockState)
        );
    }

    private Dynamic<?> fixBlockState(Dynamic<?> pTag) {
        Optional<String> optional = pTag.get("Name").asString().result().map(NamespacedSchema::ensureNamespaced);
        return optional.isPresent() && this.shouldFix(optional.get())
            ? pTag.update("Properties", p_395220_ -> this.fixProperties(optional.get(), p_395220_))
            : pTag;
    }

    protected abstract boolean shouldFix(String pName);

    protected abstract <T> Dynamic<T> fixProperties(String pName, Dynamic<T> pProperties);
}