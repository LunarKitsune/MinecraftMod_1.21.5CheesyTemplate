package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public abstract class ItemStackTagFix extends DataFix {
    private final String name;
    private final Predicate<String> idFilter;

    public ItemStackTagFix(Schema pOutputSchema, String pName, Predicate<String> pIdFilter) {
        super(pOutputSchema, false);
        this.name = pName;
        this.idFilter = pIdFilter;
    }

    @Override
    public final TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
        return this.fixTypeEverywhereTyped(this.name, type, createFixer(type, this.idFilter, this::fixItemStackTag));
    }

    public static UnaryOperator<Typed<?>> createFixer(Type<?> pType, Predicate<String> pFilter, UnaryOperator<Typed<?>> pFixer) {
        OpticFinder<Pair<String, String>> opticfinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        OpticFinder<?> opticfinder1 = pType.findField("tag");
        return p_390294_ -> {
            Optional<Pair<String, String>> optional = p_390294_.getOptional(opticfinder);
            return optional.isPresent() && pFilter.test(optional.get().getSecond()) ? p_390294_.updateTyped(opticfinder1, pFixer) : p_390294_;
        };
    }

    protected abstract Typed<?> fixItemStackTag(Typed<?> pData);
}