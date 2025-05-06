package net.minecraft.nbt;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractStringBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;

public class NbtOps implements DynamicOps<Tag> {
    public static final NbtOps INSTANCE = new NbtOps();

    private NbtOps() {
    }

    public Tag empty() {
        return EndTag.INSTANCE;
    }

    public <U> U convertTo(DynamicOps<U> pOps, Tag pTag) {
        return (U)(switch (pTag) {
            case EndTag endtag -> (Object)pOps.empty();
            case ByteTag(byte b0) -> (Object)pOps.createByte(b0);
            case ShortTag(short short1) -> (Object)pOps.createShort(short1);
            case IntTag(int i) -> (Object)pOps.createInt(i);
            case LongTag(long j) -> (Object)pOps.createLong(j);
            case FloatTag(float f) -> (Object)pOps.createFloat(f);
            case DoubleTag(double d0) -> (Object)pOps.createDouble(d0);
            case ByteArrayTag bytearraytag -> (Object)pOps.createByteList(ByteBuffer.wrap(bytearraytag.getAsByteArray()));
            case StringTag(String s) -> (Object)pOps.createString(s);
            case ListTag listtag -> (Object)this.convertList(pOps, listtag);
            case CompoundTag compoundtag -> (Object)this.convertMap(pOps, compoundtag);
            case IntArrayTag intarraytag -> (Object)pOps.createIntList(Arrays.stream(intarraytag.getAsIntArray()));
            case LongArrayTag longarraytag -> (Object)pOps.createLongList(Arrays.stream(longarraytag.getAsLongArray()));
            default -> throw new MatchException(null, null);
        });
    }

    public DataResult<Number> getNumberValue(Tag pTag) {
        return pTag.asNumber().map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Not a number"));
    }

    public Tag createNumeric(Number pData) {
        return DoubleTag.valueOf(pData.doubleValue());
    }

    public Tag createByte(byte pData) {
        return ByteTag.valueOf(pData);
    }

    public Tag createShort(short pData) {
        return ShortTag.valueOf(pData);
    }

    public Tag createInt(int pData) {
        return IntTag.valueOf(pData);
    }

    public Tag createLong(long pData) {
        return LongTag.valueOf(pData);
    }

    public Tag createFloat(float pData) {
        return FloatTag.valueOf(pData);
    }

    public Tag createDouble(double pData) {
        return DoubleTag.valueOf(pData);
    }

    public Tag createBoolean(boolean pData) {
        return ByteTag.valueOf(pData);
    }

    public DataResult<String> getStringValue(Tag pTag) {
        return pTag instanceof StringTag(String s) ? DataResult.success(s) : DataResult.error(() -> "Not a string");
    }

    public Tag createString(String pData) {
        return StringTag.valueOf(pData);
    }

    public DataResult<Tag> mergeToList(Tag pList, Tag pTag) {
        return createCollector(pList)
            .map(p_248053_ -> DataResult.success(p_248053_.accept(pTag).result()))
            .orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + pList, pList));
    }

    public DataResult<Tag> mergeToList(Tag pList, List<Tag> pTags) {
        return createCollector(pList)
            .map(p_248048_ -> DataResult.success(p_248048_.acceptAll(pTags).result()))
            .orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + pList, pList));
    }

    public DataResult<Tag> mergeToMap(Tag pMap, Tag pKey, Tag pValue) {
        if (!(pMap instanceof CompoundTag) && !(pMap instanceof EndTag)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + pMap, pMap);
        } else if (pKey instanceof StringTag(String s1)) {
            String $$5 = s1;
            CompoundTag compoundtag = pMap instanceof CompoundTag compoundtag1 ? compoundtag1.shallowCopy() : new CompoundTag();
            compoundtag.put($$5, pValue);
            return DataResult.success(compoundtag);
        } else {
            return DataResult.error(() -> "key is not a string: " + pKey, pMap);
        }
    }

    public DataResult<Tag> mergeToMap(Tag pMap, MapLike<Tag> pOtherMap) {
        if (!(pMap instanceof CompoundTag) && !(pMap instanceof EndTag)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + pMap, pMap);
        } else {
            CompoundTag compoundtag = pMap instanceof CompoundTag compoundtag1 ? compoundtag1.shallowCopy() : new CompoundTag();
            List<Tag> list = new ArrayList<>();
            pOtherMap.entries().forEach(p_389883_ -> {
                Tag tag = p_389883_.getFirst();
                if (tag instanceof StringTag(String s)) {
                    compoundtag.put(s, p_389883_.getSecond());
                } else {
                    list.add(tag);
                }
            });
            return !list.isEmpty() ? DataResult.error(() -> "some keys are not strings: " + list, compoundtag) : DataResult.success(compoundtag);
        }
    }

    public DataResult<Tag> mergeToMap(Tag p_336265_, Map<Tag, Tag> p_331137_) {
        if (!(p_336265_ instanceof CompoundTag) && !(p_336265_ instanceof EndTag)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + p_336265_, p_336265_);
        } else {
            CompoundTag compoundtag = p_336265_ instanceof CompoundTag compoundtag1 ? compoundtag1.shallowCopy() : new CompoundTag();
            List<Tag> list = new ArrayList<>();

            for (Entry<Tag, Tag> entry : p_331137_.entrySet()) {
                Tag tag = entry.getKey();
                if (tag instanceof StringTag(String s)) {
                    compoundtag.put(s, entry.getValue());
                } else {
                    list.add(tag);
                }
            }

            return !list.isEmpty() ? DataResult.error(() -> "some keys are not strings: " + list, compoundtag) : DataResult.success(compoundtag);
        }
    }

    public DataResult<Stream<Pair<Tag, Tag>>> getMapValues(Tag pMap) {
        return pMap instanceof CompoundTag compoundtag
            ? DataResult.success(compoundtag.entrySet().stream().map(p_326024_ -> Pair.of(this.createString(p_326024_.getKey()), p_326024_.getValue())))
            : DataResult.error(() -> "Not a map: " + pMap);
    }

    public DataResult<Consumer<BiConsumer<Tag, Tag>>> getMapEntries(Tag pMap) {
        return pMap instanceof CompoundTag compoundtag ? DataResult.success(p_326020_ -> {
            for (Entry<String, Tag> entry : compoundtag.entrySet()) {
                p_326020_.accept(this.createString(entry.getKey()), entry.getValue());
            }
        }) : DataResult.error(() -> "Not a map: " + pMap);
    }

    public DataResult<MapLike<Tag>> getMap(Tag pMap) {
        return pMap instanceof CompoundTag compoundtag ? DataResult.success(new MapLike<Tag>() {
            @Nullable
            public Tag get(Tag p_129174_) {
                if (p_129174_ instanceof StringTag(String s)) {
                    return compoundtag.get(s);
                } else {
                    throw new UnsupportedOperationException("Cannot get map entry with non-string key: " + p_129174_);
                }
            }

            @Nullable
            public Tag get(String p_129169_) {
                return compoundtag.get(p_129169_);
            }

            @Override
            public Stream<Pair<Tag, Tag>> entries() {
                return compoundtag.entrySet().stream().map(p_326034_ -> Pair.of(NbtOps.this.createString(p_326034_.getKey()), p_326034_.getValue()));
            }

            @Override
            public String toString() {
                return "MapLike[" + compoundtag + "]";
            }
        }) : DataResult.error(() -> "Not a map: " + pMap);
    }

    public Tag createMap(Stream<Pair<Tag, Tag>> pData) {
        CompoundTag compoundtag = new CompoundTag();
        pData.forEach(p_389880_ -> {
            Tag tag = p_389880_.getFirst();
            Tag tag1 = p_389880_.getSecond();
            if (tag instanceof StringTag(String s)) {
                compoundtag.put(s, tag1);
            } else {
                throw new UnsupportedOperationException("Cannot create map with non-string key: " + tag);
            }
        });
        return compoundtag;
    }

    public DataResult<Stream<Tag>> getStream(Tag pTag) {
        return pTag instanceof CollectionTag collectiontag ? DataResult.success(collectiontag.stream()) : DataResult.error(() -> "Not a list");
    }

    public DataResult<Consumer<Consumer<Tag>>> getList(Tag pTag) {
        return pTag instanceof CollectionTag collectiontag
            ? DataResult.success(collectiontag::forEach)
            : DataResult.error(() -> "Not a list: " + pTag);
    }

    public DataResult<ByteBuffer> getByteBuffer(Tag pTag) {
        return pTag instanceof ByteArrayTag bytearraytag
            ? DataResult.success(ByteBuffer.wrap(bytearraytag.getAsByteArray()))
            : DynamicOps.super.getByteBuffer(pTag);
    }

    public Tag createByteList(ByteBuffer pData) {
        ByteBuffer bytebuffer = pData.duplicate().clear();
        byte[] abyte = new byte[pData.capacity()];
        bytebuffer.get(0, abyte, 0, abyte.length);
        return new ByteArrayTag(abyte);
    }

    public DataResult<IntStream> getIntStream(Tag pTag) {
        return pTag instanceof IntArrayTag intarraytag
            ? DataResult.success(Arrays.stream(intarraytag.getAsIntArray()))
            : DynamicOps.super.getIntStream(pTag);
    }

    public Tag createIntList(IntStream pData) {
        return new IntArrayTag(pData.toArray());
    }

    public DataResult<LongStream> getLongStream(Tag pTag) {
        return pTag instanceof LongArrayTag longarraytag
            ? DataResult.success(Arrays.stream(longarraytag.getAsLongArray()))
            : DynamicOps.super.getLongStream(pTag);
    }

    public Tag createLongList(LongStream pData) {
        return new LongArrayTag(pData.toArray());
    }

    public Tag createList(Stream<Tag> pData) {
        return new ListTag(pData.collect(Util.toMutableList()));
    }

    public Tag remove(Tag pMap, String pRemoveKey) {
        if (pMap instanceof CompoundTag compoundtag) {
            CompoundTag compoundtag1 = compoundtag.shallowCopy();
            compoundtag1.remove(pRemoveKey);
            return compoundtag1;
        } else {
            return pMap;
        }
    }

    @Override
    public String toString() {
        return "NBT";
    }

    @Override
    public RecordBuilder<Tag> mapBuilder() {
        return new NbtOps.NbtRecordBuilder();
    }

    private static Optional<NbtOps.ListCollector> createCollector(Tag pTag) {
        if (pTag instanceof EndTag) {
            return Optional.of(new NbtOps.GenericListCollector());
        } else if (pTag instanceof CollectionTag collectiontag) {
            if (collectiontag.isEmpty()) {
                return Optional.of(new NbtOps.GenericListCollector());
            } else {
                return switch (collectiontag) {
                    case ListTag listtag -> Optional.of(new NbtOps.GenericListCollector(listtag));
                    case ByteArrayTag bytearraytag -> Optional.of(new NbtOps.ByteListCollector(bytearraytag.getAsByteArray()));
                    case IntArrayTag intarraytag -> Optional.of(new NbtOps.IntListCollector(intarraytag.getAsIntArray()));
                    case LongArrayTag longarraytag -> Optional.of(new NbtOps.LongListCollector(longarraytag.getAsLongArray()));
                    default -> throw new MatchException(null, null);
                };
            }
        } else {
            return Optional.empty();
        }
    }

    static class ByteListCollector implements NbtOps.ListCollector {
        private final ByteArrayList values = new ByteArrayList();

        public ByteListCollector(byte[] pValues) {
            this.values.addElements(0, pValues);
        }

        @Override
        public NbtOps.ListCollector accept(Tag p_250723_) {
            if (p_250723_ instanceof ByteTag bytetag) {
                this.values.add(bytetag.byteValue());
                return this;
            } else {
                return new NbtOps.GenericListCollector(this.values).accept(p_250723_);
            }
        }

        @Override
        public Tag result() {
            return new ByteArrayTag(this.values.toByteArray());
        }
    }

    static class GenericListCollector implements NbtOps.ListCollector {
        private final ListTag result = new ListTag();

        GenericListCollector() {
        }

        GenericListCollector(ListTag pList) {
            this.result.addAll(pList);
        }

        public GenericListCollector(IntArrayList pList) {
            pList.forEach(p_393744_ -> this.result.add(IntTag.valueOf(p_393744_)));
        }

        public GenericListCollector(ByteArrayList pList) {
            pList.forEach(p_393979_ -> this.result.add(ByteTag.valueOf(p_393979_)));
        }

        public GenericListCollector(LongArrayList pList) {
            pList.forEach(p_395643_ -> this.result.add(LongTag.valueOf(p_395643_)));
        }

        @Override
        public NbtOps.ListCollector accept(Tag p_391617_) {
            this.result.add(p_391617_);
            return this;
        }

        @Override
        public Tag result() {
            return this.result;
        }
    }

    static class IntListCollector implements NbtOps.ListCollector {
        private final IntArrayList values = new IntArrayList();

        public IntListCollector(int[] pValues) {
            this.values.addElements(0, pValues);
        }

        @Override
        public NbtOps.ListCollector accept(Tag p_251372_) {
            if (p_251372_ instanceof IntTag inttag) {
                this.values.add(inttag.intValue());
                return this;
            } else {
                return new NbtOps.GenericListCollector(this.values).accept(p_251372_);
            }
        }

        @Override
        public Tag result() {
            return new IntArrayTag(this.values.toIntArray());
        }
    }

    interface ListCollector {
        NbtOps.ListCollector accept(Tag pTag);

        default NbtOps.ListCollector acceptAll(Iterable<Tag> pTags) {
            NbtOps.ListCollector nbtops$listcollector = this;

            for (Tag tag : pTags) {
                nbtops$listcollector = nbtops$listcollector.accept(tag);
            }

            return nbtops$listcollector;
        }

        default NbtOps.ListCollector acceptAll(Stream<Tag> pTags) {
            return this.acceptAll(pTags::iterator);
        }

        Tag result();
    }

    static class LongListCollector implements NbtOps.ListCollector {
        private final LongArrayList values = new LongArrayList();

        public LongListCollector(long[] pValues) {
            this.values.addElements(0, pValues);
        }

        @Override
        public NbtOps.ListCollector accept(Tag p_252167_) {
            if (p_252167_ instanceof LongTag longtag) {
                this.values.add(longtag.longValue());
                return this;
            } else {
                return new NbtOps.GenericListCollector(this.values).accept(p_252167_);
            }
        }

        @Override
        public Tag result() {
            return new LongArrayTag(this.values.toLongArray());
        }
    }

    class NbtRecordBuilder extends AbstractStringBuilder<Tag, CompoundTag> {
        protected NbtRecordBuilder() {
            super(NbtOps.this);
        }

        protected CompoundTag initBuilder() {
            return new CompoundTag();
        }

        protected CompoundTag append(String pKey, Tag pValue, CompoundTag pTag) {
            pTag.put(pKey, pValue);
            return pTag;
        }

        protected DataResult<Tag> build(CompoundTag p_129190_, Tag p_129191_) {
            if (p_129191_ == null || p_129191_ == EndTag.INSTANCE) {
                return DataResult.success(p_129190_);
            } else if (!(p_129191_ instanceof CompoundTag compoundtag)) {
                return DataResult.error(() -> "mergeToMap called with not a map: " + p_129191_, p_129191_);
            } else {
                CompoundTag compoundtag1 = compoundtag.shallowCopy();

                for (Entry<String, Tag> entry : p_129190_.entrySet()) {
                    compoundtag1.put(entry.getKey(), entry.getValue());
                }

                return DataResult.success(compoundtag1);
            }
        }
    }
}