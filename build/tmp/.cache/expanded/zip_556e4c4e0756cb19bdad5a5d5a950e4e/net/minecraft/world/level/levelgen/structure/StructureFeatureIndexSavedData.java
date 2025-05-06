package net.minecraft.world.level.levelgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class StructureFeatureIndexSavedData extends SavedData {
    private final LongSet all;
    private final LongSet remaining;
    private static final Codec<LongSet> LONG_SET = Codec.LONG_STREAM.xmap(LongOpenHashSet::toSet, LongCollection::longStream);
    public static final Codec<StructureFeatureIndexSavedData> CODEC = RecordCodecBuilder.create(
        p_395424_ -> p_395424_.group(
                LONG_SET.fieldOf("All").forGetter(p_396607_ -> p_396607_.all), LONG_SET.fieldOf("Remaining").forGetter(p_393898_ -> p_393898_.remaining)
            )
            .apply(p_395424_, StructureFeatureIndexSavedData::new)
    );

    public static SavedDataType<StructureFeatureIndexSavedData> type(String pId) {
        return new SavedDataType<>(pId, StructureFeatureIndexSavedData::new, CODEC, DataFixTypes.SAVED_DATA_STRUCTURE_FEATURE_INDICES);
    }

    private StructureFeatureIndexSavedData(LongSet pAll, LongSet pRemaining) {
        this.all = pAll;
        this.remaining = pRemaining;
    }

    public StructureFeatureIndexSavedData() {
        this(new LongOpenHashSet(), new LongOpenHashSet());
    }

    public void addIndex(long pIndex) {
        this.all.add(pIndex);
        this.remaining.add(pIndex);
        this.setDirty();
    }

    public boolean hasStartIndex(long pIndex) {
        return this.all.contains(pIndex);
    }

    public boolean hasUnhandledIndex(long pIndex) {
        return this.remaining.contains(pIndex);
    }

    public void removeIndex(long pIndex) {
        if (this.remaining.remove(pIndex)) {
            this.setDirty();
        }
    }

    public LongSet getAll() {
        return this.all;
    }
}