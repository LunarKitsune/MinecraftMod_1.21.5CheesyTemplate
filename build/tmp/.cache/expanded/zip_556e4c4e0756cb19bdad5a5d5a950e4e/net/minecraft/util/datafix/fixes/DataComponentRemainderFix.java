package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import javax.annotation.Nullable;

public abstract class DataComponentRemainderFix extends DataFix {
    private final String name;
    private final String componentId;
    private final String newComponentId;

    public DataComponentRemainderFix(Schema pOutputSchema, String pName, String pComponentId) {
        this(pOutputSchema, pName, pComponentId, pComponentId);
    }

    public DataComponentRemainderFix(Schema pOutputSchema, String pName, String pComponentId, String pNewComponentId) {
        super(pOutputSchema, false);
        this.name = pName;
        this.componentId = pComponentId;
        this.newComponentId = pNewComponentId;
    }

    @Override
    public final TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.DATA_COMPONENTS);
        return this.fixTypeEverywhereTyped(this.name, type, p_375807_ -> p_375807_.update(DSL.remainderFinder(), p_377360_ -> {
            Optional<? extends Dynamic<?>> optional = p_377360_.get(this.componentId).result();
            if (optional.isEmpty()) {
                return p_377360_;
            } else {
                Dynamic<?> dynamic = this.fixComponent((Dynamic<?>)optional.get());
                return p_377360_.remove(this.componentId).setFieldIfPresent(this.newComponentId, Optional.ofNullable(dynamic));
            }
        }));
    }

    @Nullable
    protected abstract <T> Dynamic<T> fixComponent(Dynamic<T> pComponent);
}