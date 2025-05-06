package net.minecraft.nbt;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import org.slf4j.Logger;

public final class CompoundTag implements Tag {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static net.minecraftforge.common.util.INBTBuilder.Builder builder() {
        return (new net.minecraftforge.common.util.INBTBuilder(){}).nbt();
    }

    public static final Codec<CompoundTag> CODEC = Codec.PASSTHROUGH
        .comapFlatMap(
            p_308555_ -> {
                Tag tag = p_308555_.convert(NbtOps.INSTANCE).getValue();
                return tag instanceof CompoundTag compoundtag
                    ? DataResult.success(compoundtag == p_308555_.getValue() ? compoundtag.copy() : compoundtag)
                    : DataResult.error(() -> "Not a compound tag: " + tag);
            },
            p_308554_ -> new Dynamic<>(NbtOps.INSTANCE, p_308554_.copy())
        );
    private static final int SELF_SIZE_IN_BYTES = 48;
    private static final int MAP_ENTRY_SIZE_IN_BYTES = 32;
    public static final TagType<CompoundTag> TYPE = new TagType.VariableSize<CompoundTag>() {
        public CompoundTag load(DataInput p_128485_, NbtAccounter p_128487_) throws IOException {
            p_128487_.pushDepth();

            CompoundTag compoundtag;
            try {
                compoundtag = loadCompound(p_128485_, p_128487_);
            } finally {
                p_128487_.popDepth();
            }

            return compoundtag;
        }

        private static CompoundTag loadCompound(DataInput p_301703_, NbtAccounter p_301763_) throws IOException {
            p_301763_.accountBytes(48L);
            Map<String, Tag> map = Maps.newHashMap();

            byte b0;
            while ((b0 = p_301703_.readByte()) != 0) {
                String s = readString(p_301703_, p_301763_);
                Tag tag = CompoundTag.readNamedTagData(TagTypes.getType(b0), s, p_301703_, p_301763_);
                if (map.put(s, tag) == null) {
                    p_301763_.accountBytes(36L);
                }
            }

            return new CompoundTag(map);
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput p_197446_, StreamTagVisitor p_197447_, NbtAccounter p_301769_) throws IOException {
            p_301769_.pushDepth();

            StreamTagVisitor.ValueResult streamtagvisitor$valueresult;
            try {
                streamtagvisitor$valueresult = parseCompound(p_197446_, p_197447_, p_301769_);
            } finally {
                p_301769_.popDepth();
            }

            return streamtagvisitor$valueresult;
        }

        private static StreamTagVisitor.ValueResult parseCompound(DataInput p_301721_, StreamTagVisitor p_301777_, NbtAccounter p_301778_) throws IOException {
            p_301778_.accountBytes(48L);

            byte b0;
            label35:
            while ((b0 = p_301721_.readByte()) != 0) {
                TagType<?> tagtype = TagTypes.getType(b0);
                switch (p_301777_.visitEntry(tagtype)) {
                    case HALT:
                        return StreamTagVisitor.ValueResult.HALT;
                    case BREAK:
                        StringTag.skipString(p_301721_);
                        tagtype.skip(p_301721_, p_301778_);
                        break label35;
                    case SKIP:
                        StringTag.skipString(p_301721_);
                        tagtype.skip(p_301721_, p_301778_);
                        break;
                    default:
                        String s = readString(p_301721_, p_301778_);
                        switch (p_301777_.visitEntry(tagtype, s)) {
                            case HALT:
                                return StreamTagVisitor.ValueResult.HALT;
                            case BREAK:
                                tagtype.skip(p_301721_, p_301778_);
                                break label35;
                            case SKIP:
                                tagtype.skip(p_301721_, p_301778_);
                                break;
                            default:
                                p_301778_.accountBytes(36L);
                                switch (tagtype.parse(p_301721_, p_301777_, p_301778_)) {
                                    case HALT:
                                        return StreamTagVisitor.ValueResult.HALT;
                                    case BREAK:
                                }
                        }
                }
            }

            if (b0 != 0) {
                while ((b0 = p_301721_.readByte()) != 0) {
                    StringTag.skipString(p_301721_);
                    TagTypes.getType(b0).skip(p_301721_, p_301778_);
                }
            }

            return p_301777_.visitContainerEnd();
        }

        private static String readString(DataInput p_301867_, NbtAccounter p_301863_) throws IOException {
            String s = p_301867_.readUTF();
            p_301863_.accountBytes(28L);
            p_301863_.accountBytes(2L, s.length());
            return s;
        }

        @Override
        public void skip(DataInput p_197444_, NbtAccounter p_301720_) throws IOException {
            p_301720_.pushDepth();

            byte b0;
            try {
                while ((b0 = p_197444_.readByte()) != 0) {
                    StringTag.skipString(p_197444_);
                    TagTypes.getType(b0).skip(p_197444_, p_301720_);
                }
            } finally {
                p_301720_.popDepth();
            }
        }

        @Override
        public String getName() {
            return "COMPOUND";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Compound";
        }
    };
    private final Map<String, Tag> tags;

    CompoundTag(Map<String, Tag> pTags) {
        this.tags = pTags;
    }

    public CompoundTag() {
        this(new HashMap<>());
    }

    @Override
    public void write(DataOutput pOutput) throws IOException {
        for (String s : this.tags.keySet()) {
            Tag tag = this.tags.get(s);
            writeNamedTag(s, tag, pOutput);
        }

        pOutput.writeByte(0);
    }

    @Override
    public int sizeInBytes() {
        int i = 48;

        for (Entry<String, Tag> entry : this.tags.entrySet()) {
            i += 28 + 2 * entry.getKey().length();
            i += 36;
            i += entry.getValue().sizeInBytes();
        }

        return i;
    }

    public Set<String> keySet() {
        return this.tags.keySet();
    }

    public Set<Entry<String, Tag>> entrySet() {
        return this.tags.entrySet();
    }

    public Collection<Tag> values() {
        return this.tags.values();
    }

    public void forEach(BiConsumer<String, Tag> pAction) {
        this.tags.forEach(pAction);
    }

    @Override
    public byte getId() {
        return 10;
    }

    @Override
    public TagType<CompoundTag> getType() {
        return TYPE;
    }

    public int size() {
        return this.tags.size();
    }

    @Nullable
    public Tag put(String pKey, Tag pValue) {
        return this.tags.put(pKey, pValue);
    }

    public void putByte(String pKey, byte pValue) {
        this.tags.put(pKey, ByteTag.valueOf(pValue));
    }

    public void putShort(String pKey, short pValue) {
        this.tags.put(pKey, ShortTag.valueOf(pValue));
    }

    public void putInt(String pKey, int pValue) {
        this.tags.put(pKey, IntTag.valueOf(pValue));
    }

    public void putLong(String pKey, long pValue) {
        this.tags.put(pKey, LongTag.valueOf(pValue));
    }

    public void putFloat(String pKey, float pValue) {
        this.tags.put(pKey, FloatTag.valueOf(pValue));
    }

    public void putDouble(String pKey, double pValue) {
        this.tags.put(pKey, DoubleTag.valueOf(pValue));
    }

    public void putString(String pKey, String pValue) {
        this.tags.put(pKey, StringTag.valueOf(pValue));
    }

    public void putByteArray(String pKey, byte[] pValue) {
        this.tags.put(pKey, new ByteArrayTag(pValue));
    }

    public void putIntArray(String pKey, int[] pValue) {
        this.tags.put(pKey, new IntArrayTag(pValue));
    }

    public void putLongArray(String pKey, long[] pValue) {
        this.tags.put(pKey, new LongArrayTag(pValue));
    }

    public void putBoolean(String pKey, boolean pValue) {
        this.tags.put(pKey, ByteTag.valueOf(pValue));
    }

    @Nullable
    public Tag get(String pKey) {
        return this.tags.get(pKey);
    }

    public boolean contains(String pKey) {
        return this.tags.containsKey(pKey);
    }

    private Optional<Tag> getOptional(String pKey) {
        return Optional.ofNullable(this.tags.get(pKey));
    }

    public Optional<Byte> getByte(String pKey) {
        return this.getOptional(pKey).flatMap(Tag::asByte);
    }

    public byte getByteOr(String pKey, byte pDefaultValue) {
        return this.tags.get(pKey) instanceof NumericTag numerictag ? numerictag.byteValue() : pDefaultValue;
    }

    public Optional<Short> getShort(String pKey) {
        return this.getOptional(pKey).flatMap(Tag::asShort);
    }

    public short getShortOr(String pKey, short pDefaultValue) {
        return this.tags.get(pKey) instanceof NumericTag numerictag ? numerictag.shortValue() : pDefaultValue;
    }

    public Optional<Integer> getInt(String pKey) {
        return this.getOptional(pKey).flatMap(Tag::asInt);
    }

    public int getIntOr(String pKey, int pDefaultValue) {
        return this.tags.get(pKey) instanceof NumericTag numerictag ? numerictag.intValue() : pDefaultValue;
    }

    public Optional<Long> getLong(String pKey) {
        return this.getOptional(pKey).flatMap(Tag::asLong);
    }

    public long getLongOr(String pKey, long pDefaultValue) {
        return this.tags.get(pKey) instanceof NumericTag numerictag ? numerictag.longValue() : pDefaultValue;
    }

    public Optional<Float> getFloat(String pKey) {
        return this.getOptional(pKey).flatMap(Tag::asFloat);
    }

    public float getFloatOr(String pKey, float pDefaultValue) {
        return this.tags.get(pKey) instanceof NumericTag numerictag ? numerictag.floatValue() : pDefaultValue;
    }

    public Optional<Double> getDouble(String pKey) {
        return this.getOptional(pKey).flatMap(Tag::asDouble);
    }

    public double getDoubleOr(String pKey, double pDefaultValue) {
        return this.tags.get(pKey) instanceof NumericTag numerictag ? numerictag.doubleValue() : pDefaultValue;
    }

    public Optional<String> getString(String pKey) {
        return this.getOptional(pKey).flatMap(Tag::asString);
    }

    public String getStringOr(String pKey, String pDefaultValue) {
        return this.tags.get(pKey) instanceof StringTag(String s) ? s : pDefaultValue;
    }

    public Optional<byte[]> getByteArray(String pKey) {
        return this.tags.get(pKey) instanceof ByteArrayTag bytearraytag ? Optional.of(bytearraytag.getAsByteArray()) : Optional.empty();
    }

    public Optional<int[]> getIntArray(String pKey) {
        return this.tags.get(pKey) instanceof IntArrayTag intarraytag ? Optional.of(intarraytag.getAsIntArray()) : Optional.empty();
    }

    public Optional<long[]> getLongArray(String pKey) {
        return this.tags.get(pKey) instanceof LongArrayTag longarraytag ? Optional.of(longarraytag.getAsLongArray()) : Optional.empty();
    }

    public Optional<CompoundTag> getCompound(String pKey) {
        return this.tags.get(pKey) instanceof CompoundTag compoundtag ? Optional.of(compoundtag) : Optional.empty();
    }

    public CompoundTag getCompoundOrEmpty(String pKey) {
        return this.getCompound(pKey).orElseGet(CompoundTag::new);
    }

    public Optional<ListTag> getList(String pKey) {
        return this.tags.get(pKey) instanceof ListTag listtag ? Optional.of(listtag) : Optional.empty();
    }

    public ListTag getListOrEmpty(String pKey) {
        return this.getList(pKey).orElseGet(ListTag::new);
    }

    public Optional<Boolean> getBoolean(String pKey) {
        return this.getOptional(pKey).flatMap(Tag::asBoolean);
    }

    public boolean getBooleanOr(String pKey, boolean pDefaultValue) {
        return this.getByteOr(pKey, (byte)(pDefaultValue ? 1 : 0)) != 0;
    }

    public void remove(String pKey) {
        this.tags.remove(pKey);
    }

    @Override
    public String toString() {
        StringTagVisitor stringtagvisitor = new StringTagVisitor();
        stringtagvisitor.visitCompound(this);
        return stringtagvisitor.build();
    }

    public boolean isEmpty() {
        return this.tags.isEmpty();
    }

    protected CompoundTag shallowCopy() {
        return new CompoundTag(new HashMap<>(this.tags));
    }

    public CompoundTag copy() {
        HashMap<String, Tag> hashmap = new HashMap<>();
        this.tags.forEach((p_389877_, p_389878_) -> hashmap.put(p_389877_, p_389878_.copy()));
        return new CompoundTag(hashmap);
    }

    @Override
    public Optional<CompoundTag> asCompound() {
        return Optional.of(this);
    }

    @Override
    public boolean equals(Object pOther) {
        return this == pOther ? true : pOther instanceof CompoundTag && Objects.equals(this.tags, ((CompoundTag)pOther).tags);
    }

    @Override
    public int hashCode() {
        return this.tags.hashCode();
    }

    private static void writeNamedTag(String pName, Tag pTag, DataOutput pOutput) throws IOException {
        pOutput.writeByte(pTag.getId());
        if (pTag.getId() != 0) {
            pOutput.writeUTF(pName);
            pTag.write(pOutput);
        }
    }

    static Tag readNamedTagData(TagType<?> pType, String pName, DataInput pInput, NbtAccounter pAccounter) {
        try {
            return pType.load(pInput, pAccounter);
        } catch (IOException ioexception) {
            CrashReport crashreport = CrashReport.forThrowable(ioexception, "Loading NBT data");
            CrashReportCategory crashreportcategory = crashreport.addCategory("NBT Tag");
            crashreportcategory.setDetail("Tag name", pName);
            crashreportcategory.setDetail("Tag type", pType.getName());
            throw new ReportedNbtException(crashreport);
        }
    }

    public CompoundTag merge(CompoundTag pOther) {
        for (String s : pOther.tags.keySet()) {
            Tag tag = pOther.tags.get(s);
            if (tag instanceof CompoundTag compoundtag && this.tags.get(s) instanceof CompoundTag compoundtag1) {
                compoundtag1.merge(compoundtag);
            } else {
                this.put(s, tag.copy());
            }
        }

        return this;
    }

    @Override
    public void accept(TagVisitor p_177857_) {
        p_177857_.visitCompound(this);
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor p_197442_) {
        for (Entry<String, Tag> entry : this.tags.entrySet()) {
            Tag tag = entry.getValue();
            TagType<?> tagtype = tag.getType();
            StreamTagVisitor.EntryResult streamtagvisitor$entryresult = p_197442_.visitEntry(tagtype);
            switch (streamtagvisitor$entryresult) {
                case HALT:
                    return StreamTagVisitor.ValueResult.HALT;
                case BREAK:
                    return p_197442_.visitContainerEnd();
                case SKIP:
                    break;
                default:
                    streamtagvisitor$entryresult = p_197442_.visitEntry(tagtype, entry.getKey());
                    switch (streamtagvisitor$entryresult) {
                        case HALT:
                            return StreamTagVisitor.ValueResult.HALT;
                        case BREAK:
                            return p_197442_.visitContainerEnd();
                        case SKIP:
                            break;
                        default:
                            StreamTagVisitor.ValueResult streamtagvisitor$valueresult = tag.accept(p_197442_);
                            switch (streamtagvisitor$valueresult) {
                                case HALT:
                                    return StreamTagVisitor.ValueResult.HALT;
                                case BREAK:
                                    return p_197442_.visitContainerEnd();
                            }
                    }
            }
        }

        return p_197442_.visitContainerEnd();
    }

    public <T> void store(String pKey, Codec<T> pCodec, T pData) {
        this.store(pKey, pCodec, NbtOps.INSTANCE, pData);
    }

    public <T> void storeNullable(String pKey, Codec<T> pCodec, @Nullable T pData) {
        if (pData != null) {
            this.store(pKey, pCodec, pData);
        }
    }

    public <T> void store(String pKey, Codec<T> pCodec, DynamicOps<Tag> pOps, T pData) {
        this.put(pKey, pCodec.encodeStart(pOps, pData).getOrThrow());
    }

    public <T> void storeNullable(String pKey, Codec<T> pCodec, DynamicOps<Tag> pOps, @Nullable T pData) {
        if (pData != null) {
            this.store(pKey, pCodec, pOps, pData);
        }
    }

    public <T> void store(MapCodec<T> pMapCodec, T pData) {
        this.store(pMapCodec, NbtOps.INSTANCE, pData);
    }

    public <T> void store(MapCodec<T> pMapCodec, DynamicOps<Tag> pOps, T pData) {
        this.merge((CompoundTag)pMapCodec.encoder().encodeStart(pOps, pData).getOrThrow());
    }

    public <T> Optional<T> read(String pKey, Codec<T> pCodec) {
        return this.read(pKey, pCodec, NbtOps.INSTANCE);
    }

    public <T> Optional<T> read(String pKey, Codec<T> pCodec, DynamicOps<Tag> pOps) {
        Tag tag = this.get(pKey);
        return tag == null
            ? Optional.empty()
            : pCodec.parse(pOps, tag).resultOrPartial(p_389874_ -> LOGGER.error("Failed to read field ({}={}): {}", pKey, tag, p_389874_));
    }

    public <T> Optional<T> read(MapCodec<T> pMapCodec) {
        return this.read(pMapCodec, NbtOps.INSTANCE);
    }

    public <T> Optional<T> read(MapCodec<T> pMapCodec, DynamicOps<Tag> pOps) {
        return pMapCodec.decode(pOps, pOps.getMap(this).getOrThrow())
            .resultOrPartial(p_389875_ -> LOGGER.error("Failed to read value ({}): {}", this, p_389875_));
    }
}
