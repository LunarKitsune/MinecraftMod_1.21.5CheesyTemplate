package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import org.slf4j.Logger;

public final class NbtUtils {
    private static final Comparator<ListTag> YXZ_LISTTAG_INT_COMPARATOR = Comparator.<ListTag>comparingInt(p_389895_ -> p_389895_.getIntOr(1, 0))
        .thenComparingInt(p_389897_ -> p_389897_.getIntOr(0, 0))
        .thenComparingInt(p_389901_ -> p_389901_.getIntOr(2, 0));
    private static final Comparator<ListTag> YXZ_LISTTAG_DOUBLE_COMPARATOR = Comparator.<ListTag>comparingDouble(p_389902_ -> p_389902_.getDoubleOr(1, 0.0))
        .thenComparingDouble(p_389889_ -> p_389889_.getDoubleOr(0, 0.0))
        .thenComparingDouble(p_389886_ -> p_389886_.getDoubleOr(2, 0.0));
    private static final Codec<ResourceKey<Block>> BLOCK_NAME_CODEC = ResourceKey.codec(Registries.BLOCK);
    public static final String SNBT_DATA_TAG = "data";
    private static final char PROPERTIES_START = '{';
    private static final char PROPERTIES_END = '}';
    private static final String ELEMENT_SEPARATOR = ",";
    private static final char KEY_VALUE_SEPARATOR = ':';
    private static final Splitter COMMA_SPLITTER = Splitter.on(",");
    private static final Splitter COLON_SPLITTER = Splitter.on(':').limit(2);
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int INDENT = 2;
    private static final int NOT_FOUND = -1;

    private NbtUtils() {
    }

    @VisibleForTesting
    public static boolean compareNbt(@Nullable Tag pTag, @Nullable Tag pOther, boolean pCompareListTag) {
        if (pTag == pOther) {
            return true;
        } else if (pTag == null) {
            return true;
        } else if (pOther == null) {
            return false;
        } else if (!pTag.getClass().equals(pOther.getClass())) {
            return false;
        } else if (pTag instanceof CompoundTag compoundtag) {
            CompoundTag compoundtag1 = (CompoundTag)pOther;
            if (compoundtag1.size() < compoundtag.size()) {
                return false;
            } else {
                for (Entry<String, Tag> entry : compoundtag.entrySet()) {
                    Tag tag2 = entry.getValue();
                    if (!compareNbt(tag2, compoundtag1.get(entry.getKey()), pCompareListTag)) {
                        return false;
                    }
                }

                return true;
            }
        } else if (pTag instanceof ListTag listtag && pCompareListTag) {
            ListTag listtag1 = (ListTag)pOther;
            if (listtag.isEmpty()) {
                return listtag1.isEmpty();
            } else if (listtag1.size() < listtag.size()) {
                return false;
            } else {
                for (Tag tag : listtag) {
                    boolean flag = false;

                    for (Tag tag1 : listtag1) {
                        if (compareNbt(tag, tag1, pCompareListTag)) {
                            flag = true;
                            break;
                        }
                    }

                    if (!flag) {
                        return false;
                    }
                }

                return true;
            }
        } else {
            return pTag.equals(pOther);
        }
    }

    public static BlockState readBlockState(HolderGetter<Block> pBlockGetter, CompoundTag pTag) {
        Optional<? extends Holder<Block>> optional = pTag.read("Name", BLOCK_NAME_CODEC).flatMap(pBlockGetter::get);
        if (optional.isEmpty()) {
            return Blocks.AIR.defaultBlockState();
        } else {
            Block block = optional.get().value();
            BlockState blockstate = block.defaultBlockState();
            Optional<CompoundTag> optional1 = pTag.getCompound("Properties");
            if (optional1.isPresent()) {
                StateDefinition<Block, BlockState> statedefinition = block.getStateDefinition();

                for (String s : optional1.get().keySet()) {
                    Property<?> property = statedefinition.getProperty(s);
                    if (property != null) {
                        blockstate = setValueHelper(blockstate, property, s, optional1.get(), pTag);
                    }
                }
            }

            return blockstate;
        }
    }

    private static <S extends StateHolder<?, S>, T extends Comparable<T>> S setValueHelper(
        S pStateHolder, Property<T> pProperty, String pPropertyName, CompoundTag pPropertiesTag, CompoundTag pBlockStateTag
    ) {
        Optional<T> optional = pPropertiesTag.getString(pPropertyName).flatMap(pProperty::getValue);
        if (optional.isPresent()) {
            return pStateHolder.setValue(pProperty, optional.get());
        } else {
            LOGGER.warn("Unable to read property: {} with value: {} for blockstate: {}", pPropertyName, pPropertiesTag.get(pPropertyName), pBlockStateTag);
            return pStateHolder;
        }
    }

    public static CompoundTag writeBlockState(BlockState pState) {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putString("Name", BuiltInRegistries.BLOCK.getKey(pState.getBlock()).toString());
        Map<Property<?>, Comparable<?>> map = pState.getValues();
        if (!map.isEmpty()) {
            CompoundTag compoundtag1 = new CompoundTag();

            for (Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
                Property<?> property = entry.getKey();
                compoundtag1.putString(property.getName(), getName(property, entry.getValue()));
            }

            compoundtag.put("Properties", compoundtag1);
        }

        return compoundtag;
    }

    public static CompoundTag writeFluidState(FluidState pState) {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putString("Name", BuiltInRegistries.FLUID.getKey(pState.getType()).toString());
        Map<Property<?>, Comparable<?>> map = pState.getValues();
        if (!map.isEmpty()) {
            CompoundTag compoundtag1 = new CompoundTag();

            for (Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
                Property<?> property = entry.getKey();
                compoundtag1.putString(property.getName(), getName(property, entry.getValue()));
            }

            compoundtag.put("Properties", compoundtag1);
        }

        return compoundtag;
    }

    private static <T extends Comparable<T>> String getName(Property<T> pProperty, Comparable<?> pValue) {
        return pProperty.getName((T)pValue);
    }

    public static String prettyPrint(Tag pTag) {
        return prettyPrint(pTag, false);
    }

    public static String prettyPrint(Tag pTag, boolean pPrettyPrintArray) {
        return prettyPrint(new StringBuilder(), pTag, 0, pPrettyPrintArray).toString();
    }

    public static StringBuilder prettyPrint(StringBuilder pStringBuilder, Tag pTag, int pIndentLevel, boolean pPrettyPrintArray) {
        return switch (pTag) {
            case PrimitiveTag primitivetag -> pStringBuilder.append(primitivetag);
            case EndTag endtag -> pStringBuilder;
            case ByteArrayTag bytearraytag -> {
                byte[] abyte = bytearraytag.getAsByteArray();
                int i1 = abyte.length;
                indent(pIndentLevel, pStringBuilder).append("byte[").append(i1).append("] {\n");
                if (pPrettyPrintArray) {
                    indent(pIndentLevel + 1, pStringBuilder);

                    for (int k1 = 0; k1 < abyte.length; k1++) {
                        if (k1 != 0) {
                            pStringBuilder.append(',');
                        }

                        if (k1 % 16 == 0 && k1 / 16 > 0) {
                            pStringBuilder.append('\n');
                            if (k1 < abyte.length) {
                                indent(pIndentLevel + 1, pStringBuilder);
                            }
                        } else if (k1 != 0) {
                            pStringBuilder.append(' ');
                        }

                        pStringBuilder.append(String.format(Locale.ROOT, "0x%02X", abyte[k1] & 255));
                    }
                } else {
                    indent(pIndentLevel + 1, pStringBuilder).append(" // Skipped, supply withBinaryBlobs true");
                }

                pStringBuilder.append('\n');
                indent(pIndentLevel, pStringBuilder).append('}');
                yield pStringBuilder;
            }
            case ListTag listtag -> {
                int l = listtag.size();
                indent(pIndentLevel, pStringBuilder).append("list").append("[").append(l).append("] [");
                if (l != 0) {
                    pStringBuilder.append('\n');
                }

                for (int j1 = 0; j1 < l; j1++) {
                    if (j1 != 0) {
                        pStringBuilder.append(",\n");
                    }

                    indent(pIndentLevel + 1, pStringBuilder);
                    prettyPrint(pStringBuilder, listtag.get(j1), pIndentLevel + 1, pPrettyPrintArray);
                }

                if (l != 0) {
                    pStringBuilder.append('\n');
                }

                indent(pIndentLevel, pStringBuilder).append(']');
                yield pStringBuilder;
            }
            case IntArrayTag intarraytag -> {
                int[] aint = intarraytag.getAsIntArray();
                int l1 = 0;

                for (int i3 : aint) {
                    l1 = Math.max(l1, String.format(Locale.ROOT, "%X", i3).length());
                }

                int j2 = aint.length;
                indent(pIndentLevel, pStringBuilder).append("int[").append(j2).append("] {\n");
                if (pPrettyPrintArray) {
                    indent(pIndentLevel + 1, pStringBuilder);

                    for (int k2 = 0; k2 < aint.length; k2++) {
                        if (k2 != 0) {
                            pStringBuilder.append(',');
                        }

                        if (k2 % 16 == 0 && k2 / 16 > 0) {
                            pStringBuilder.append('\n');
                            if (k2 < aint.length) {
                                indent(pIndentLevel + 1, pStringBuilder);
                            }
                        } else if (k2 != 0) {
                            pStringBuilder.append(' ');
                        }

                        pStringBuilder.append(String.format(Locale.ROOT, "0x%0" + l1 + "X", aint[k2]));
                    }
                } else {
                    indent(pIndentLevel + 1, pStringBuilder).append(" // Skipped, supply withBinaryBlobs true");
                }

                pStringBuilder.append('\n');
                indent(pIndentLevel, pStringBuilder).append('}');
                yield pStringBuilder;
            }
            case CompoundTag compoundtag -> {
                List<String> list = Lists.newArrayList(compoundtag.keySet());
                Collections.sort(list);
                indent(pIndentLevel, pStringBuilder).append('{');
                if (pStringBuilder.length() - pStringBuilder.lastIndexOf("\n") > 2 * (pIndentLevel + 1)) {
                    pStringBuilder.append('\n');
                    indent(pIndentLevel + 1, pStringBuilder);
                }

                int i2 = list.stream().mapToInt(String::length).max().orElse(0);
                String s = Strings.repeat(" ", i2);

                for (int j = 0; j < list.size(); j++) {
                    if (j != 0) {
                        pStringBuilder.append(",\n");
                    }

                    String s1 = list.get(j);
                    indent(pIndentLevel + 1, pStringBuilder).append('"').append(s1).append('"').append(s, 0, s.length() - s1.length()).append(": ");
                    prettyPrint(pStringBuilder, compoundtag.get(s1), pIndentLevel + 1, pPrettyPrintArray);
                }

                if (!list.isEmpty()) {
                    pStringBuilder.append('\n');
                }

                indent(pIndentLevel, pStringBuilder).append('}');
                yield pStringBuilder;
            }
            case LongArrayTag longarraytag -> {
                long[] along = longarraytag.getAsLongArray();
                long i = 0L;

                for (long k : along) {
                    i = Math.max(i, (long)String.format(Locale.ROOT, "%X", k).length());
                }

                long l2 = along.length;
                indent(pIndentLevel, pStringBuilder).append("long[").append(l2).append("] {\n");
                if (pPrettyPrintArray) {
                    indent(pIndentLevel + 1, pStringBuilder);

                    for (int j3 = 0; j3 < along.length; j3++) {
                        if (j3 != 0) {
                            pStringBuilder.append(',');
                        }

                        if (j3 % 16 == 0 && j3 / 16 > 0) {
                            pStringBuilder.append('\n');
                            if (j3 < along.length) {
                                indent(pIndentLevel + 1, pStringBuilder);
                            }
                        } else if (j3 != 0) {
                            pStringBuilder.append(' ');
                        }

                        pStringBuilder.append(String.format(Locale.ROOT, "0x%0" + i + "X", along[j3]));
                    }
                } else {
                    indent(pIndentLevel + 1, pStringBuilder).append(" // Skipped, supply withBinaryBlobs true");
                }

                pStringBuilder.append('\n');
                indent(pIndentLevel, pStringBuilder).append('}');
                yield pStringBuilder;
            }
            default -> throw new MatchException(null, null);
        };
    }

    private static StringBuilder indent(int pIndentLevel, StringBuilder pStringBuilder) {
        int i = pStringBuilder.lastIndexOf("\n") + 1;
        int j = pStringBuilder.length() - i;

        for (int k = 0; k < 2 * pIndentLevel - j; k++) {
            pStringBuilder.append(' ');
        }

        return pStringBuilder;
    }

    public static Component toPrettyComponent(Tag pTag) {
        return new TextComponentTagVisitor("").visit(pTag);
    }

    public static String structureToSnbt(CompoundTag pTag) {
        return new SnbtPrinterTagVisitor().visit(packStructureTemplate(pTag));
    }

    public static CompoundTag snbtToStructure(String pText) throws CommandSyntaxException {
        return unpackStructureTemplate(TagParser.parseCompoundFully(pText));
    }

    @VisibleForTesting
    static CompoundTag packStructureTemplate(CompoundTag pTag) {
        Optional<ListTag> optional = pTag.getList("palettes");
        ListTag listtag;
        if (optional.isPresent()) {
            listtag = optional.get().getListOrEmpty(0);
        } else {
            listtag = pTag.getListOrEmpty("palette");
        }

        ListTag listtag1 = listtag.compoundStream().map(NbtUtils::packBlockState).map(StringTag::valueOf).collect(Collectors.toCollection(ListTag::new));
        pTag.put("palette", listtag1);
        if (optional.isPresent()) {
            ListTag listtag2 = new ListTag();
            optional.get().stream().flatMap(p_389905_ -> p_389905_.asList().stream()).forEach(p_389894_ -> {
                CompoundTag compoundtag = new CompoundTag();

                for (int i = 0; i < p_389894_.size(); i++) {
                    compoundtag.putString(listtag1.getString(i).orElseThrow(), packBlockState(p_389894_.getCompound(i).orElseThrow()));
                }

                listtag2.add(compoundtag);
            });
            pTag.put("palettes", listtag2);
        }

        Optional<ListTag> optional1 = pTag.getList("entities");
        if (optional1.isPresent()) {
            ListTag listtag3 = optional1.get()
                .compoundStream()
                .sorted(Comparator.comparing(p_389903_ -> p_389903_.getList("pos"), Comparators.emptiesLast(YXZ_LISTTAG_DOUBLE_COMPARATOR)))
                .collect(Collectors.toCollection(ListTag::new));
            pTag.put("entities", listtag3);
        }

        ListTag listtag4 = pTag.getList("blocks")
            .stream()
            .flatMap(ListTag::compoundStream)
            .sorted(Comparator.comparing(p_389898_ -> p_389898_.getList("pos"), Comparators.emptiesLast(YXZ_LISTTAG_INT_COMPARATOR)))
            .peek(p_389885_ -> p_389885_.putString("state", listtag1.getString(p_389885_.getIntOr("state", 0)).orElseThrow()))
            .collect(Collectors.toCollection(ListTag::new));
        pTag.put("data", listtag4);
        pTag.remove("blocks");
        return pTag;
    }

    @VisibleForTesting
    static CompoundTag unpackStructureTemplate(CompoundTag pTag) {
        ListTag listtag = pTag.getListOrEmpty("palette");
        Map<String, Tag> map = listtag.stream()
            .flatMap(p_389904_ -> p_389904_.asString().stream())
            .collect(ImmutableMap.toImmutableMap(Function.identity(), NbtUtils::unpackBlockState));
        Optional<ListTag> optional = pTag.getList("palettes");
        if (optional.isPresent()) {
            pTag.put(
                "palettes",
                optional.get()
                    .compoundStream()
                    .map(
                        p_389891_ -> map.keySet()
                            .stream()
                            .map(p_389900_ -> p_389891_.getString(p_389900_).orElseThrow())
                            .map(NbtUtils::unpackBlockState)
                            .collect(Collectors.toCollection(ListTag::new))
                    )
                    .collect(Collectors.toCollection(ListTag::new))
            );
            pTag.remove("palette");
        } else {
            pTag.put("palette", map.values().stream().collect(Collectors.toCollection(ListTag::new)));
        }

        Optional<ListTag> optional1 = pTag.getList("data");
        if (optional1.isPresent()) {
            Object2IntMap<String> object2intmap = new Object2IntOpenHashMap<>();
            object2intmap.defaultReturnValue(-1);

            for (int i = 0; i < listtag.size(); i++) {
                object2intmap.put(listtag.getString(i).orElseThrow(), i);
            }

            ListTag listtag1 = optional1.get();

            for (int j = 0; j < listtag1.size(); j++) {
                CompoundTag compoundtag = listtag1.getCompound(j).orElseThrow();
                String s = compoundtag.getString("state").orElseThrow();
                int k = object2intmap.getInt(s);
                if (k == -1) {
                    throw new IllegalStateException("Entry " + s + " missing from palette");
                }

                compoundtag.putInt("state", k);
            }

            pTag.put("blocks", listtag1);
            pTag.remove("data");
        }

        return pTag;
    }

    @VisibleForTesting
    static String packBlockState(CompoundTag pTag) {
        StringBuilder stringbuilder = new StringBuilder(pTag.getString("Name").orElseThrow());
        pTag.getCompound("Properties")
            .ifPresent(
                p_389888_ -> {
                    String s = p_389888_.entrySet()
                        .stream()
                        .sorted(Entry.comparingByKey())
                        .map(p_389896_ -> p_389896_.getKey() + ":" + p_389896_.getValue().asString().orElseThrow())
                        .collect(Collectors.joining(","));
                    stringbuilder.append('{').append(s).append('}');
                }
            );
        return stringbuilder.toString();
    }

    @VisibleForTesting
    static CompoundTag unpackBlockState(String pBlockStateText) {
        CompoundTag compoundtag = new CompoundTag();
        int i = pBlockStateText.indexOf(123);
        String s;
        if (i >= 0) {
            s = pBlockStateText.substring(0, i);
            CompoundTag compoundtag1 = new CompoundTag();
            if (i + 2 <= pBlockStateText.length()) {
                String s1 = pBlockStateText.substring(i + 1, pBlockStateText.indexOf(125, i));
                COMMA_SPLITTER.split(s1).forEach(p_178040_ -> {
                    List<String> list = COLON_SPLITTER.splitToList(p_178040_);
                    if (list.size() == 2) {
                        compoundtag1.putString(list.get(0), list.get(1));
                    } else {
                        LOGGER.error("Something went wrong parsing: '{}' -- incorrect gamedata!", pBlockStateText);
                    }
                });
                compoundtag.put("Properties", compoundtag1);
            }
        } else {
            s = pBlockStateText;
        }

        compoundtag.putString("Name", s);
        return compoundtag;
    }

    public static CompoundTag addCurrentDataVersion(CompoundTag pTag) {
        int i = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
        return addDataVersion(pTag, i);
    }

    public static CompoundTag addDataVersion(CompoundTag pTag, int pDataVersion) {
        pTag.putInt("DataVersion", pDataVersion);
        return pTag;
    }

    public static int getDataVersion(CompoundTag pTag, int pDefaultValue) {
        return pTag.getIntOr("DataVersion", pDefaultValue);
    }

    public static int getDataVersion(Dynamic<?> pTag, int pDefaultValue) {
        return pTag.get("DataVersion").asInt(pDefaultValue);
    }
}