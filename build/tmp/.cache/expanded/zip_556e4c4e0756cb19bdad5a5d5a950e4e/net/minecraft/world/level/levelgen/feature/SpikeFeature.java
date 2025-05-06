package net.minecraft.world.level.levelgen.feature;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.feature.configurations.SpikeConfiguration;
import net.minecraft.world.phys.AABB;

public class SpikeFeature extends Feature<SpikeConfiguration> {
    public static final int NUMBER_OF_SPIKES = 10;
    private static final int SPIKE_DISTANCE = 42;
    private static final LoadingCache<Long, List<SpikeFeature.EndSpike>> SPIKE_CACHE = CacheBuilder.newBuilder()
        .expireAfterWrite(5L, TimeUnit.MINUTES)
        .build(new SpikeFeature.SpikeCacheLoader());

    public SpikeFeature(Codec<SpikeConfiguration> p_66852_) {
        super(p_66852_);
    }

    public static List<SpikeFeature.EndSpike> getSpikesForLevel(WorldGenLevel pLevel) {
        RandomSource randomsource = RandomSource.create(pLevel.getSeed());
        long i = randomsource.nextLong() & 65535L;
        return SPIKE_CACHE.getUnchecked(i);
    }

    @Override
    public boolean place(FeaturePlaceContext<SpikeConfiguration> p_160372_) {
        SpikeConfiguration spikeconfiguration = p_160372_.config();
        WorldGenLevel worldgenlevel = p_160372_.level();
        RandomSource randomsource = p_160372_.random();
        BlockPos blockpos = p_160372_.origin();
        List<SpikeFeature.EndSpike> list = spikeconfiguration.getSpikes();
        if (list.isEmpty()) {
            list = getSpikesForLevel(worldgenlevel);
        }

        for (SpikeFeature.EndSpike spikefeature$endspike : list) {
            if (spikefeature$endspike.isCenterWithinChunk(blockpos)) {
                this.placeSpike(worldgenlevel, randomsource, spikeconfiguration, spikefeature$endspike);
            }
        }

        return true;
    }

    private void placeSpike(ServerLevelAccessor pLevel, RandomSource pRandom, SpikeConfiguration pConfig, SpikeFeature.EndSpike pSpike) {
        int i = pSpike.getRadius();

        for (BlockPos blockpos : BlockPos.betweenClosed(
            new BlockPos(pSpike.getCenterX() - i, pLevel.getMinY(), pSpike.getCenterZ() - i),
            new BlockPos(pSpike.getCenterX() + i, pSpike.getHeight() + 10, pSpike.getCenterZ() + i)
        )) {
            if (blockpos.distToLowCornerSqr(pSpike.getCenterX(), blockpos.getY(), pSpike.getCenterZ()) <= i * i + 1
                && blockpos.getY() < pSpike.getHeight()) {
                this.setBlock(pLevel, blockpos, Blocks.OBSIDIAN.defaultBlockState());
            } else if (blockpos.getY() > 65) {
                this.setBlock(pLevel, blockpos, Blocks.AIR.defaultBlockState());
            }
        }

        if (pSpike.isGuarded()) {
            int j1 = -2;
            int k1 = 2;
            int j = 3;
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            for (int k = -2; k <= 2; k++) {
                for (int l = -2; l <= 2; l++) {
                    for (int i1 = 0; i1 <= 3; i1++) {
                        boolean flag = Mth.abs(k) == 2;
                        boolean flag1 = Mth.abs(l) == 2;
                        boolean flag2 = i1 == 3;
                        if (flag || flag1 || flag2) {
                            boolean flag3 = k == -2 || k == 2 || flag2;
                            boolean flag4 = l == -2 || l == 2 || flag2;
                            BlockState blockstate = Blocks.IRON_BARS
                                .defaultBlockState()
                                .setValue(IronBarsBlock.NORTH, flag3 && l != -2)
                                .setValue(IronBarsBlock.SOUTH, flag3 && l != 2)
                                .setValue(IronBarsBlock.WEST, flag4 && k != -2)
                                .setValue(IronBarsBlock.EAST, flag4 && k != 2);
                            this.setBlock(
                                pLevel,
                                blockpos$mutableblockpos.set(pSpike.getCenterX() + k, pSpike.getHeight() + i1, pSpike.getCenterZ() + l),
                                blockstate
                            );
                        }
                    }
                }
            }
        }

        EndCrystal endcrystal = EntityType.END_CRYSTAL.create(pLevel.getLevel(), EntitySpawnReason.STRUCTURE);
        if (endcrystal != null) {
            endcrystal.setBeamTarget(pConfig.getCrystalBeamTarget());
            endcrystal.setInvulnerable(pConfig.isCrystalInvulnerable());
            endcrystal.snapTo(pSpike.getCenterX() + 0.5, pSpike.getHeight() + 1, pSpike.getCenterZ() + 0.5, pRandom.nextFloat() * 360.0F, 0.0F);
            pLevel.addFreshEntity(endcrystal);
            BlockPos blockpos1 = endcrystal.blockPosition();
            this.setBlock(pLevel, blockpos1.below(), Blocks.BEDROCK.defaultBlockState());
            this.setBlock(pLevel, blockpos1, FireBlock.getState(pLevel, blockpos1));
        }
    }

    public static class EndSpike {
        public static final Codec<SpikeFeature.EndSpike> CODEC = RecordCodecBuilder.create(
            p_66890_ -> p_66890_.group(
                    Codec.INT.fieldOf("centerX").orElse(0).forGetter(p_160382_ -> p_160382_.centerX),
                    Codec.INT.fieldOf("centerZ").orElse(0).forGetter(p_160380_ -> p_160380_.centerZ),
                    Codec.INT.fieldOf("radius").orElse(0).forGetter(p_160378_ -> p_160378_.radius),
                    Codec.INT.fieldOf("height").orElse(0).forGetter(p_160376_ -> p_160376_.height),
                    Codec.BOOL.fieldOf("guarded").orElse(false).forGetter(p_160374_ -> p_160374_.guarded)
                )
                .apply(p_66890_, SpikeFeature.EndSpike::new)
        );
        private final int centerX;
        private final int centerZ;
        private final int radius;
        private final int height;
        private final boolean guarded;
        private final AABB topBoundingBox;

        public EndSpike(int pCenterX, int pCenterZ, int pRadius, int pHeight, boolean pGuarded) {
            this.centerX = pCenterX;
            this.centerZ = pCenterZ;
            this.radius = pRadius;
            this.height = pHeight;
            this.guarded = pGuarded;
            this.topBoundingBox = new AABB(
                pCenterX - pRadius, DimensionType.MIN_Y, pCenterZ - pRadius, pCenterX + pRadius, DimensionType.MAX_Y, pCenterZ + pRadius
            );
        }

        public boolean isCenterWithinChunk(BlockPos pPos) {
            return SectionPos.blockToSectionCoord(pPos.getX()) == SectionPos.blockToSectionCoord(this.centerX)
                && SectionPos.blockToSectionCoord(pPos.getZ()) == SectionPos.blockToSectionCoord(this.centerZ);
        }

        public int getCenterX() {
            return this.centerX;
        }

        public int getCenterZ() {
            return this.centerZ;
        }

        public int getRadius() {
            return this.radius;
        }

        public int getHeight() {
            return this.height;
        }

        public boolean isGuarded() {
            return this.guarded;
        }

        public AABB getTopBoundingBox() {
            return this.topBoundingBox;
        }
    }

    static class SpikeCacheLoader extends CacheLoader<Long, List<SpikeFeature.EndSpike>> {
        public List<SpikeFeature.EndSpike> load(Long p_66910_) {
            IntArrayList intarraylist = Util.toShuffledList(IntStream.range(0, 10), RandomSource.create(p_66910_));
            List<SpikeFeature.EndSpike> list = Lists.newArrayList();

            for (int i = 0; i < 10; i++) {
                int j = Mth.floor(42.0 * Math.cos(2.0 * (-Math.PI + (Math.PI / 10) * i)));
                int k = Mth.floor(42.0 * Math.sin(2.0 * (-Math.PI + (Math.PI / 10) * i)));
                int l = intarraylist.get(i);
                int i1 = 2 + l / 3;
                int j1 = 76 + l * 3;
                boolean flag = l == 1 || l == 2;
                list.add(new SpikeFeature.EndSpike(j, k, i1, j1, flag));
            }

            return list;
        }
    }
}