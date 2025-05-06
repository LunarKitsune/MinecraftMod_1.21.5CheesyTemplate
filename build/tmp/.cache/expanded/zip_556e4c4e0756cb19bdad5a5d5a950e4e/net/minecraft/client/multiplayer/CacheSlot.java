package net.minecraft.client.multiplayer;

import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CacheSlot<C extends CacheSlot.Cleaner<C>, D> {
    private final Function<C, D> operation;
    @Nullable
    private C context;
    @Nullable
    private D value;

    public CacheSlot(Function<C, D> pOperation) {
        this.operation = pOperation;
    }

    public D compute(C pContext) {
        if (pContext == this.context && this.value != null) {
            return this.value;
        } else {
            D d = this.operation.apply(pContext);
            this.value = d;
            this.context = pContext;
            pContext.registerForCleaning(this);
            return d;
        }
    }

    public void clear() {
        this.value = null;
        this.context = null;
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface Cleaner<C extends CacheSlot.Cleaner<C>> {
        void registerForCleaning(CacheSlot<C, ?> pCacheSlot);
    }
}