package net.minecraft.util;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;
import java.util.function.UnaryOperator;

abstract class AbstractListBuilder<T, B> implements ListBuilder<T> {
    private final DynamicOps<T> ops;
    protected DataResult<B> builder = DataResult.success(this.initBuilder(), Lifecycle.stable());

    protected AbstractListBuilder(DynamicOps<T> pOps) {
        this.ops = pOps;
    }

    @Override
    public DynamicOps<T> ops() {
        return this.ops;
    }

    protected abstract B initBuilder();

    protected abstract B append(B pBuilder, T pValue);

    protected abstract DataResult<T> build(B pBuilder, T pValue);

    @Override
    public ListBuilder<T> add(T pValue) {
        this.builder = this.builder.map(p_397872_ -> this.append((B)p_397872_, pValue));
        return this;
    }

    @Override
    public ListBuilder<T> add(DataResult<T> pValue) {
        this.builder = this.builder.apply2stable(this::append, pValue);
        return this;
    }

    @Override
    public ListBuilder<T> withErrorsFrom(DataResult<?> pResult) {
        this.builder = this.builder.flatMap(p_394538_ -> pResult.map(p_395945_ -> p_394538_));
        return this;
    }

    @Override
    public ListBuilder<T> mapError(UnaryOperator<String> pOnError) {
        this.builder = this.builder.mapError(pOnError);
        return this;
    }

    @Override
    public DataResult<T> build(T pPrefix) {
        DataResult<T> dataresult = this.builder.flatMap(p_397770_ -> this.build((B)p_397770_, pPrefix));
        this.builder = DataResult.success(this.initBuilder(), Lifecycle.stable());
        return dataresult;
    }
}