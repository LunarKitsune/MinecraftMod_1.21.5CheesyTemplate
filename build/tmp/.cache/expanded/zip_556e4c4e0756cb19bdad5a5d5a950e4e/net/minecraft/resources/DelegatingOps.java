package net.minecraft.resources;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * A {@link DynamicOps} that delegates all functionality to an internal delegate. Comments and parameters here are
 * copied from {@link DynamicOps} in DataFixerUpper.
 */
public abstract class DelegatingOps<T> implements DynamicOps<T> {
    protected final DynamicOps<T> delegate;

    protected DelegatingOps(DynamicOps<T> pDelegate) {
        this.delegate = pDelegate;
    }

    @Override
    public T empty() {
        return this.delegate.empty();
    }

    @Override
    public T emptyMap() {
        return this.delegate.emptyMap();
    }

    @Override
    public T emptyList() {
        return this.delegate.emptyList();
    }

    @Override
    public <U> U convertTo(DynamicOps<U> pOutOps, T pInput) {
        return (U)(Objects.equals(pOutOps, this.delegate) ? pInput : this.delegate.convertTo(pOutOps, pInput));
    }

    @Override
    public DataResult<Number> getNumberValue(T pInput) {
        return this.delegate.getNumberValue(pInput);
    }

    @Override
    public T createNumeric(Number pI) {
        return this.delegate.createNumeric(pI);
    }

    @Override
    public T createByte(byte pValue) {
        return this.delegate.createByte(pValue);
    }

    @Override
    public T createShort(short pValue) {
        return this.delegate.createShort(pValue);
    }

    @Override
    public T createInt(int pValue) {
        return this.delegate.createInt(pValue);
    }

    @Override
    public T createLong(long pValue) {
        return this.delegate.createLong(pValue);
    }

    @Override
    public T createFloat(float pValue) {
        return this.delegate.createFloat(pValue);
    }

    @Override
    public T createDouble(double pValue) {
        return this.delegate.createDouble(pValue);
    }

    @Override
    public DataResult<Boolean> getBooleanValue(T pInput) {
        return this.delegate.getBooleanValue(pInput);
    }

    @Override
    public T createBoolean(boolean pValue) {
        return this.delegate.createBoolean(pValue);
    }

    @Override
    public DataResult<String> getStringValue(T pInput) {
        return this.delegate.getStringValue(pInput);
    }

    @Override
    public T createString(String pValue) {
        return this.delegate.createString(pValue);
    }

    @Override
    public DataResult<T> mergeToList(T pList, T pValue) {
        return this.delegate.mergeToList(pList, pValue);
    }

    @Override
    public DataResult<T> mergeToList(T pList, List<T> pValues) {
        return this.delegate.mergeToList(pList, pValues);
    }

    @Override
    public DataResult<T> mergeToMap(T pMap, T pKey, T pValue) {
        return this.delegate.mergeToMap(pMap, pKey, pValue);
    }

    @Override
    public DataResult<T> mergeToMap(T pMap, MapLike<T> pValues) {
        return this.delegate.mergeToMap(pMap, pValues);
    }

    @Override
    public DataResult<T> mergeToMap(T pMap, Map<T, T> pValues) {
        return this.delegate.mergeToMap(pMap, pValues);
    }

    @Override
    public DataResult<T> mergeToPrimitive(T pPrefix, T pValue) {
        return this.delegate.mergeToPrimitive(pPrefix, pValue);
    }

    @Override
    public DataResult<Stream<Pair<T, T>>> getMapValues(T pInput) {
        return this.delegate.getMapValues(pInput);
    }

    @Override
    public DataResult<Consumer<BiConsumer<T, T>>> getMapEntries(T pInput) {
        return this.delegate.getMapEntries(pInput);
    }

    @Override
    public T createMap(Map<T, T> pMap) {
        return this.delegate.createMap(pMap);
    }

    @Override
    public T createMap(Stream<Pair<T, T>> pMap) {
        return this.delegate.createMap(pMap);
    }

    @Override
    public DataResult<MapLike<T>> getMap(T pInput) {
        return this.delegate.getMap(pInput);
    }

    @Override
    public DataResult<Stream<T>> getStream(T pInput) {
        return this.delegate.getStream(pInput);
    }

    @Override
    public DataResult<Consumer<Consumer<T>>> getList(T pInput) {
        return this.delegate.getList(pInput);
    }

    @Override
    public T createList(Stream<T> pInput) {
        return this.delegate.createList(pInput);
    }

    @Override
    public DataResult<ByteBuffer> getByteBuffer(T pInput) {
        return this.delegate.getByteBuffer(pInput);
    }

    @Override
    public T createByteList(ByteBuffer pInput) {
        return this.delegate.createByteList(pInput);
    }

    @Override
    public DataResult<IntStream> getIntStream(T pInput) {
        return this.delegate.getIntStream(pInput);
    }

    @Override
    public T createIntList(IntStream pInput) {
        return this.delegate.createIntList(pInput);
    }

    @Override
    public DataResult<LongStream> getLongStream(T pInput) {
        return this.delegate.getLongStream(pInput);
    }

    @Override
    public T createLongList(LongStream pInput) {
        return this.delegate.createLongList(pInput);
    }

    @Override
    public T remove(T pInput, String pKey) {
        return this.delegate.remove(pInput, pKey);
    }

    @Override
    public boolean compressMaps() {
        return this.delegate.compressMaps();
    }

    @Override
    public ListBuilder<T> listBuilder() {
        return new DelegatingOps.DelegateListBuilder(this.delegate.listBuilder());
    }

    @Override
    public RecordBuilder<T> mapBuilder() {
        return new DelegatingOps.DelegateRecordBuilder(this.delegate.mapBuilder());
    }

    // Forge start
    java.util.Map<ResourceLocation, Object> ctx = new java.util.HashMap<>();

    @SuppressWarnings("unchecked")
    public <R> R getContext(ResourceLocation rl) {
        var ret = (R)this.ctx.get(rl);
        if (ret == null && this.delegate instanceof DelegatingOps parent) {
            return (R)parent.getContext(rl);
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public <R> R withContext(ResourceLocation key, Object ctx) {
        this.ctx.put(key, ctx);
        return (R)this;
    }

    protected class DelegateListBuilder implements ListBuilder<T> {
        private final ListBuilder<T> original;

        protected DelegateListBuilder(final ListBuilder<T> pOriginal) {
            this.original = pOriginal;
        }

        @Override
        public DynamicOps<T> ops() {
            return DelegatingOps.this;
        }

        @Override
        public DataResult<T> build(T pPrefix) {
            return this.original.build(pPrefix);
        }

        @Override
        public ListBuilder<T> add(T pValue) {
            this.original.add(pValue);
            return this;
        }

        @Override
        public ListBuilder<T> add(DataResult<T> pValue) {
            this.original.add(pValue);
            return this;
        }

        @Override
        public <E> ListBuilder<T> add(E pElement, Encoder<E> pEncoder) {
            this.original.add(pEncoder.encodeStart(this.ops(), pElement));
            return this;
        }

        @Override
        public <E> ListBuilder<T> addAll(Iterable<E> pElements, Encoder<E> pEncoder) {
            pElements.forEach(p_395457_ -> this.original.add(pEncoder.encode((E)p_395457_, this.ops(), (T)this.ops().empty())));
            return this;
        }

        @Override
        public ListBuilder<T> withErrorsFrom(DataResult<?> pResult) {
            this.original.withErrorsFrom(pResult);
            return this;
        }

        @Override
        public ListBuilder<T> mapError(UnaryOperator<String> pOnError) {
            this.original.mapError(pOnError);
            return this;
        }

        @Override
        public DataResult<T> build(DataResult<T> pPrefix) {
            return this.original.build(pPrefix);
        }
    }

    protected class DelegateRecordBuilder implements RecordBuilder<T> {
        private final RecordBuilder<T> original;

        protected DelegateRecordBuilder(final RecordBuilder<T> pOriginal) {
            this.original = pOriginal;
        }

        @Override
        public DynamicOps<T> ops() {
            return DelegatingOps.this;
        }

        @Override
        public RecordBuilder<T> add(T pKey, T pValue) {
            this.original.add(pKey, pValue);
            return this;
        }

        @Override
        public RecordBuilder<T> add(T pKey, DataResult<T> pValue) {
            this.original.add(pKey, pValue);
            return this;
        }

        @Override
        public RecordBuilder<T> add(DataResult<T> pKey, DataResult<T> pValue) {
            this.original.add(pKey, pValue);
            return this;
        }

        @Override
        public RecordBuilder<T> add(String pKey, T pValue) {
            this.original.add(pKey, pValue);
            return this;
        }

        @Override
        public RecordBuilder<T> add(String pKey, DataResult<T> pValue) {
            this.original.add(pKey, pValue);
            return this;
        }

        @Override
        public <E> RecordBuilder<T> add(String pKey, E pElement, Encoder<E> pEncoder) {
            return this.original.add(pKey, pEncoder.encodeStart(this.ops(), pElement));
        }

        @Override
        public RecordBuilder<T> withErrorsFrom(DataResult<?> pResult) {
            this.original.withErrorsFrom(pResult);
            return this;
        }

        @Override
        public RecordBuilder<T> setLifecycle(Lifecycle pLifecycle) {
            this.original.setLifecycle(pLifecycle);
            return this;
        }

        @Override
        public RecordBuilder<T> mapError(UnaryOperator<String> pOnError) {
            this.original.mapError(pOnError);
            return this;
        }

        @Override
        public DataResult<T> build(T pPrefix) {
            return this.original.build(pPrefix);
        }

        @Override
        public DataResult<T> build(DataResult<T> pPrefix) {
            return this.original.build(pPrefix);
        }
    }
}
