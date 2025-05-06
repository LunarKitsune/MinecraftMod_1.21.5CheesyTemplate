package net.minecraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class RandomSequences extends SavedData {
    public static final SavedDataType<RandomSequences> TYPE = new SavedDataType<>(
        "random_sequences",
        p_390464_ -> new RandomSequences(p_390464_.worldSeed()),
        p_390466_ -> codec(p_390466_.worldSeed()),
        DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
    );
    private final long worldSeed;
    private int salt;
    private boolean includeWorldSeed = true;
    private boolean includeSequenceId = true;
    private final Map<ResourceLocation, RandomSequence> sequences = new Object2ObjectOpenHashMap<>();

    public RandomSequences(long pSeed) {
        this.worldSeed = pSeed;
    }

    private RandomSequences(long pWorldSeed, int pSalt, boolean pIncludeWorldSeed, boolean pIncludeSequenceId, Map<ResourceLocation, RandomSequence> pSequences) {
        this.worldSeed = pWorldSeed;
        this.salt = pSalt;
        this.includeWorldSeed = pIncludeWorldSeed;
        this.includeSequenceId = pIncludeSequenceId;
        this.sequences.putAll(pSequences);
    }

    public static Codec<RandomSequences> codec(long pWorldSeed) {
        return RecordCodecBuilder.create(
            p_390468_ -> p_390468_.group(
                    RecordCodecBuilder.point(pWorldSeed),
                    Codec.INT.fieldOf("salt").forGetter(p_390465_ -> p_390465_.salt),
                    Codec.BOOL.optionalFieldOf("include_world_seed", true).forGetter(p_390469_ -> p_390469_.includeWorldSeed),
                    Codec.BOOL.optionalFieldOf("include_sequence_id", true).forGetter(p_390470_ -> p_390470_.includeSequenceId),
                    Codec.unboundedMap(ResourceLocation.CODEC, RandomSequence.CODEC).fieldOf("sequences").forGetter(p_390463_ -> p_390463_.sequences)
                )
                .apply(p_390468_, RandomSequences::new)
        );
    }

    public RandomSource get(ResourceLocation pLocation) {
        RandomSource randomsource = this.sequences.computeIfAbsent(pLocation, this::createSequence).random();
        return new RandomSequences.DirtyMarkingRandomSource(randomsource);
    }

    private RandomSequence createSequence(ResourceLocation pLocation) {
        return this.createSequence(pLocation, this.salt, this.includeWorldSeed, this.includeSequenceId);
    }

    private RandomSequence createSequence(ResourceLocation pLocation, int pSalt, boolean pIncludeWorldSeed, boolean pIncludeSequenceId) {
        long i = (pIncludeWorldSeed ? this.worldSeed : 0L) ^ pSalt;
        return new RandomSequence(i, pIncludeSequenceId ? Optional.of(pLocation) : Optional.empty());
    }

    public void forAllSequences(BiConsumer<ResourceLocation, RandomSequence> pAction) {
        this.sequences.forEach(pAction);
    }

    public void setSeedDefaults(int pSalt, boolean pIncludeWorldSeed, boolean pIncludeSequenceId) {
        this.salt = pSalt;
        this.includeWorldSeed = pIncludeWorldSeed;
        this.includeSequenceId = pIncludeSequenceId;
    }

    public int clear() {
        int i = this.sequences.size();
        this.sequences.clear();
        return i;
    }

    public void reset(ResourceLocation pSequence) {
        this.sequences.put(pSequence, this.createSequence(pSequence));
    }

    public void reset(ResourceLocation pSequence, int pSeed, boolean pIncludeWorldSeed, boolean pIncludeSequenceId) {
        this.sequences.put(pSequence, this.createSequence(pSequence, pSeed, pIncludeWorldSeed, pIncludeSequenceId));
    }

    class DirtyMarkingRandomSource implements RandomSource {
        private final RandomSource random;

        DirtyMarkingRandomSource(final RandomSource pRandom) {
            this.random = pRandom;
        }

        @Override
        public RandomSource fork() {
            RandomSequences.this.setDirty();
            return this.random.fork();
        }

        @Override
        public PositionalRandomFactory forkPositional() {
            RandomSequences.this.setDirty();
            return this.random.forkPositional();
        }

        @Override
        public void setSeed(long p_300098_) {
            RandomSequences.this.setDirty();
            this.random.setSeed(p_300098_);
        }

        @Override
        public int nextInt() {
            RandomSequences.this.setDirty();
            return this.random.nextInt();
        }

        @Override
        public int nextInt(int p_301106_) {
            RandomSequences.this.setDirty();
            return this.random.nextInt(p_301106_);
        }

        @Override
        public long nextLong() {
            RandomSequences.this.setDirty();
            return this.random.nextLong();
        }

        @Override
        public boolean nextBoolean() {
            RandomSequences.this.setDirty();
            return this.random.nextBoolean();
        }

        @Override
        public float nextFloat() {
            RandomSequences.this.setDirty();
            return this.random.nextFloat();
        }

        @Override
        public double nextDouble() {
            RandomSequences.this.setDirty();
            return this.random.nextDouble();
        }

        @Override
        public double nextGaussian() {
            RandomSequences.this.setDirty();
            return this.random.nextGaussian();
        }

        @Override
        public boolean equals(Object pOther) {
            if (this == pOther) {
                return true;
            } else {
                return pOther instanceof RandomSequences.DirtyMarkingRandomSource randomsequences$dirtymarkingrandomsource
                    ? this.random.equals(randomsequences$dirtymarkingrandomsource.random)
                    : false;
            }
        }
    }
}