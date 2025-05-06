package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public final class ListTag extends AbstractList<Tag> implements CollectionTag {
    private static final String WRAPPER_MARKER = "";
    private static final int SELF_SIZE_IN_BYTES = 36;
    public static final TagType<ListTag> TYPE = new TagType.VariableSize<ListTag>() {
        public ListTag load(DataInput p_128792_, NbtAccounter p_128794_) throws IOException {
            p_128794_.pushDepth();

            ListTag listtag;
            try {
                listtag = loadList(p_128792_, p_128794_);
            } finally {
                p_128794_.popDepth();
            }

            return listtag;
        }

        private static ListTag loadList(DataInput p_301758_, NbtAccounter p_301694_) throws IOException {
            p_301694_.accountBytes(36L);
            byte b0 = p_301758_.readByte();
            int i = p_301758_.readInt();
            if (b0 == 0 && i > 0) {
                throw new NbtFormatException("Missing type on ListTag");
            } else {
                p_301694_.accountBytes(4L, i);
                TagType<?> tagtype = TagTypes.getType(b0);
                ListTag listtag = new ListTag(new ArrayList<>(i));

                for (int j = 0; j < i; j++) {
                    listtag.addAndUnwrap(tagtype.load(p_301758_, p_301694_));
                }

                return listtag;
            }
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput p_197491_, StreamTagVisitor p_197492_, NbtAccounter p_301731_) throws IOException {
            p_301731_.pushDepth();

            StreamTagVisitor.ValueResult streamtagvisitor$valueresult;
            try {
                streamtagvisitor$valueresult = parseList(p_197491_, p_197492_, p_301731_);
            } finally {
                p_301731_.popDepth();
            }

            return streamtagvisitor$valueresult;
        }

        private static StreamTagVisitor.ValueResult parseList(DataInput p_301745_, StreamTagVisitor p_301695_, NbtAccounter p_301734_) throws IOException {
            p_301734_.accountBytes(36L);
            TagType<?> tagtype = TagTypes.getType(p_301745_.readByte());
            int i = p_301745_.readInt();
            switch (p_301695_.visitList(tagtype, i)) {
                case HALT:
                    return StreamTagVisitor.ValueResult.HALT;
                case BREAK:
                    tagtype.skip(p_301745_, i, p_301734_);
                    return p_301695_.visitContainerEnd();
                default:
                    p_301734_.accountBytes(4L, i);
                    int j = 0;

                    while (true) {
                        label41: {
                            if (j < i) {
                                switch (p_301695_.visitElement(tagtype, j)) {
                                    case HALT:
                                        return StreamTagVisitor.ValueResult.HALT;
                                    case BREAK:
                                        tagtype.skip(p_301745_, p_301734_);
                                        break;
                                    case SKIP:
                                        tagtype.skip(p_301745_, p_301734_);
                                        break label41;
                                    default:
                                        switch (tagtype.parse(p_301745_, p_301695_, p_301734_)) {
                                            case HALT:
                                                return StreamTagVisitor.ValueResult.HALT;
                                            case BREAK:
                                                break;
                                            default:
                                                break label41;
                                        }
                                }
                            }

                            int k = i - 1 - j;
                            if (k > 0) {
                                tagtype.skip(p_301745_, k, p_301734_);
                            }

                            return p_301695_.visitContainerEnd();
                        }

                        j++;
                    }
            }
        }

        @Override
        public void skip(DataInput p_301743_, NbtAccounter p_301728_) throws IOException {
            p_301728_.pushDepth();

            try {
                TagType<?> tagtype = TagTypes.getType(p_301743_.readByte());
                int i = p_301743_.readInt();
                tagtype.skip(p_301743_, i, p_301728_);
            } finally {
                p_301728_.popDepth();
            }
        }

        @Override
        public String getName() {
            return "LIST";
        }

        @Override
        public String getPrettyName() {
            return "TAG_List";
        }
    };
    private final List<Tag> list;

    public ListTag() {
        this(new ArrayList<>());
    }

    ListTag(List<Tag> pList) {
        this.list = pList;
    }

    private static Tag tryUnwrap(CompoundTag pTag) {
        if (pTag.size() == 1) {
            Tag tag = pTag.get("");
            if (tag != null) {
                return tag;
            }
        }

        return pTag;
    }

    private static boolean isWrapper(CompoundTag pTag) {
        return pTag.size() == 1 && pTag.contains("");
    }

    private static Tag wrapIfNeeded(byte pElementType, Tag pTag) {
        if (pElementType != 10) {
            return pTag;
        } else {
            return pTag instanceof CompoundTag compoundtag && !isWrapper(compoundtag) ? compoundtag : wrapElement(pTag);
        }
    }

    private static CompoundTag wrapElement(Tag pTag) {
        return new CompoundTag(Map.of("", pTag));
    }

    @Override
    public void write(DataOutput pOutput) throws IOException {
        byte b0 = this.identifyRawElementType();
        pOutput.writeByte(b0);
        pOutput.writeInt(this.list.size());

        for (Tag tag : this.list) {
            wrapIfNeeded(b0, tag).write(pOutput);
        }
    }

    @VisibleForTesting
    byte identifyRawElementType() {
        byte b0 = 0;

        for (Tag tag : this.list) {
            byte b1 = tag.getId();
            if (b0 == 0) {
                b0 = b1;
            } else if (b0 != b1) {
                return 10;
            }
        }

        return b0;
    }

    public void addAndUnwrap(Tag pTag) {
        if (pTag instanceof CompoundTag compoundtag) {
            this.add(tryUnwrap(compoundtag));
        } else {
            this.add(pTag);
        }
    }

    @Override
    public int sizeInBytes() {
        int i = 36;
        i += 4 * this.list.size();

        for (Tag tag : this.list) {
            i += tag.sizeInBytes();
        }

        return i;
    }

    @Override
    public byte getId() {
        return 9;
    }

    @Override
    public TagType<ListTag> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        StringTagVisitor stringtagvisitor = new StringTagVisitor();
        stringtagvisitor.visitList(this);
        return stringtagvisitor.build();
    }

    @Override
    public Tag remove(int p_128751_) {
        return this.list.remove(p_128751_);
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    public Optional<CompoundTag> getCompound(int pIndex) {
        return this.getNullable(pIndex) instanceof CompoundTag compoundtag ? Optional.of(compoundtag) : Optional.empty();
    }

    public CompoundTag getCompoundOrEmpty(int pIndex) {
        return this.getCompound(pIndex).orElseGet(CompoundTag::new);
    }

    public Optional<ListTag> getList(int pIndex) {
        return this.getNullable(pIndex) instanceof ListTag listtag ? Optional.of(listtag) : Optional.empty();
    }

    public ListTag getListOrEmpty(int pIndex) {
        return this.getList(pIndex).orElseGet(ListTag::new);
    }

    public Optional<Short> getShort(int pIndex) {
        return this.getOptional(pIndex).flatMap(Tag::asShort);
    }

    public short getShortOr(int pIndex, short pDefaultValue) {
        return this.getNullable(pIndex) instanceof NumericTag numerictag ? numerictag.shortValue() : pDefaultValue;
    }

    public Optional<Integer> getInt(int pIndex) {
        return this.getOptional(pIndex).flatMap(Tag::asInt);
    }

    public int getIntOr(int pIndex, int pDefaultValue) {
        return this.getNullable(pIndex) instanceof NumericTag numerictag ? numerictag.intValue() : pDefaultValue;
    }

    public Optional<int[]> getIntArray(int pIndex) {
        return this.getNullable(pIndex) instanceof IntArrayTag intarraytag ? Optional.of(intarraytag.getAsIntArray()) : Optional.empty();
    }

    public Optional<long[]> getLongArray(int pIndex) {
        return this.getNullable(pIndex) instanceof LongArrayTag longarraytag ? Optional.of(longarraytag.getAsLongArray()) : Optional.empty();
    }

    public Optional<Double> getDouble(int pIndex) {
        return this.getOptional(pIndex).flatMap(Tag::asDouble);
    }

    public double getDoubleOr(int pIndex, double pDefaultValue) {
        return this.getNullable(pIndex) instanceof NumericTag numerictag ? numerictag.doubleValue() : pDefaultValue;
    }

    public Optional<Float> getFloat(int pIndex) {
        return this.getOptional(pIndex).flatMap(Tag::asFloat);
    }

    public float getFloatOr(int pIndex, float pDefaultValue) {
        return this.getNullable(pIndex) instanceof NumericTag numerictag ? numerictag.floatValue() : pDefaultValue;
    }

    public Optional<String> getString(int pIndex) {
        return this.getOptional(pIndex).flatMap(Tag::asString);
    }

    public String getStringOr(int pIndex, String pDefaultValue) {
        return this.getNullable(pIndex) instanceof StringTag(String s) ? s : pDefaultValue;
    }

    @Nullable
    private Tag getNullable(int pIndex) {
        return pIndex >= 0 && pIndex < this.list.size() ? this.list.get(pIndex) : null;
    }

    private Optional<Tag> getOptional(int pIndex) {
        return Optional.ofNullable(this.getNullable(pIndex));
    }

    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public Tag get(int pIndex) {
        return this.list.get(pIndex);
    }

    public Tag set(int p_128760_, Tag p_128761_) {
        return this.list.set(p_128760_, p_128761_);
    }

    public void add(int p_128753_, Tag p_128754_) {
        this.list.add(p_128753_, p_128754_);
    }

    @Override
    public boolean setTag(int pIndex, Tag pNbt) {
        this.list.set(pIndex, pNbt);
        return true;
    }

    @Override
    public boolean addTag(int pIndex, Tag pNbt) {
        this.list.add(pIndex, pNbt);
        return true;
    }

    public ListTag copy() {
        List<Tag> list = new ArrayList<>(this.list.size());

        for (Tag tag : this.list) {
            list.add(tag.copy());
        }

        return new ListTag(list);
    }

    @Override
    public Optional<ListTag> asList() {
        return Optional.of(this);
    }

    @Override
    public boolean equals(Object pOther) {
        return this == pOther ? true : pOther instanceof ListTag && Objects.equals(this.list, ((ListTag)pOther).list);
    }

    @Override
    public int hashCode() {
        return this.list.hashCode();
    }

    @Override
    public Stream<Tag> stream() {
        return super.stream();
    }

    public Stream<CompoundTag> compoundStream() {
        return this.stream().mapMulti((p_396018_, p_392733_) -> {
            if (p_396018_ instanceof CompoundTag compoundtag) {
                p_392733_.accept(compoundtag);
            }
        });
    }

    @Override
    public void accept(TagVisitor p_177990_) {
        p_177990_.visitList(this);
    }

    @Override
    public void clear() {
        this.list.clear();
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor p_197487_) {
        byte b0 = this.identifyRawElementType();
        switch (p_197487_.visitList(TagTypes.getType(b0), this.list.size())) {
            case HALT:
                return StreamTagVisitor.ValueResult.HALT;
            case BREAK:
                return p_197487_.visitContainerEnd();
            default:
                int i = 0;

                while (i < this.list.size()) {
                    Tag tag = wrapIfNeeded(b0, this.list.get(i));
                    switch (p_197487_.visitElement(tag.getType(), i)) {
                        case HALT:
                            return StreamTagVisitor.ValueResult.HALT;
                        case BREAK:
                            return p_197487_.visitContainerEnd();
                        default:
                            switch (tag.accept(p_197487_)) {
                                case HALT:
                                    return StreamTagVisitor.ValueResult.HALT;
                                case BREAK:
                                    return p_197487_.visitContainerEnd();
                            }
                        case SKIP:
                            i++;
                    }
                }

                return p_197487_.visitContainerEnd();
        }
    }
}