package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;

/**
 * A carver which creates Minecraft's most common cave types.
 */
public class CaveWorldCarver extends WorldCarver<CaveCarverConfiguration> {
    public CaveWorldCarver(Codec<CaveCarverConfiguration> p_159194_) {
        super(p_159194_);
    }

    public boolean isStartChunk(CaveCarverConfiguration p_224894_, RandomSource p_224895_) {
        return p_224895_.nextFloat() <= p_224894_.probability;
    }

    public boolean carve(
        CarvingContext p_224885_,
        CaveCarverConfiguration p_224886_,
        ChunkAccess p_224887_,
        Function<BlockPos, Holder<Biome>> p_224888_,
        RandomSource p_224889_,
        Aquifer p_224890_,
        ChunkPos p_224891_,
        CarvingMask p_224892_
    ) {
        int i = SectionPos.sectionToBlockCoord(this.getRange() * 2 - 1);
        int j = p_224889_.nextInt(p_224889_.nextInt(p_224889_.nextInt(this.getCaveBound()) + 1) + 1);

        for (int k = 0; k < j; k++) {
            double d0 = p_224891_.getBlockX(p_224889_.nextInt(16));
            double d1 = p_224886_.y.sample(p_224889_, p_224885_);
            double d2 = p_224891_.getBlockZ(p_224889_.nextInt(16));
            double d3 = p_224886_.horizontalRadiusMultiplier.sample(p_224889_);
            double d4 = p_224886_.verticalRadiusMultiplier.sample(p_224889_);
            double d5 = p_224886_.floorLevel.sample(p_224889_);
            WorldCarver.CarveSkipChecker worldcarver$carveskipchecker = (p_159202_, p_159203_, p_159204_, p_159205_, p_159206_) -> shouldSkip(
                p_159203_, p_159204_, p_159205_, d5
            );
            int l = 1;
            if (p_224889_.nextInt(4) == 0) {
                double d6 = p_224886_.yScale.sample(p_224889_);
                float f1 = 1.0F + p_224889_.nextFloat() * 6.0F;
                this.createRoom(p_224885_, p_224886_, p_224887_, p_224888_, p_224890_, d0, d1, d2, f1, d6, p_224892_, worldcarver$carveskipchecker);
                l += p_224889_.nextInt(4);
            }

            for (int k1 = 0; k1 < l; k1++) {
                float f = p_224889_.nextFloat() * (float) (Math.PI * 2);
                float f3 = (p_224889_.nextFloat() - 0.5F) / 4.0F;
                float f2 = this.getThickness(p_224889_);
                int i1 = i - p_224889_.nextInt(i / 4);
                int j1 = 0;
                this.createTunnel(
                    p_224885_,
                    p_224886_,
                    p_224887_,
                    p_224888_,
                    p_224889_.nextLong(),
                    p_224890_,
                    d0,
                    d1,
                    d2,
                    d3,
                    d4,
                    f2,
                    f,
                    f3,
                    0,
                    i1,
                    this.getYScale(),
                    p_224892_,
                    worldcarver$carveskipchecker
                );
            }
        }

        return true;
    }

    protected int getCaveBound() {
        return 15;
    }

    protected float getThickness(RandomSource pRandom) {
        float f = pRandom.nextFloat() * 2.0F + pRandom.nextFloat();
        if (pRandom.nextInt(10) == 0) {
            f *= pRandom.nextFloat() * pRandom.nextFloat() * 3.0F + 1.0F;
        }

        return f;
    }

    protected double getYScale() {
        return 1.0;
    }

    protected void createRoom(
        CarvingContext pContext,
        CaveCarverConfiguration pConfig,
        ChunkAccess pChunk,
        Function<BlockPos, Holder<Biome>> pBiomeAccessor,
        Aquifer pAquifer,
        double pX,
        double pY,
        double pZ,
        float pRadius,
        double pHorizontalVerticalRatio,
        CarvingMask pCarvingMask,
        WorldCarver.CarveSkipChecker pSkipChecker
    ) {
        double d0 = 1.5 + Mth.sin((float) (Math.PI / 2)) * pRadius;
        double d1 = d0 * pHorizontalVerticalRatio;
        this.carveEllipsoid(pContext, pConfig, pChunk, pBiomeAccessor, pAquifer, pX + 1.0, pY, pZ, d0, d1, pCarvingMask, pSkipChecker);
    }

    protected void createTunnel(
        CarvingContext pContext,
        CaveCarverConfiguration pConfig,
        ChunkAccess pChunk,
        Function<BlockPos, Holder<Biome>> pBiomeAccessor,
        long pSeed,
        Aquifer pAquifer,
        double pX,
        double pY,
        double pZ,
        double pHorizontalRadiusMultiplier,
        double pVerticalRadiusMultiplier,
        float pThickness,
        float pYaw,
        float pPitch,
        int pBranchIndex,
        int pBranchCount,
        double pHorizontalVerticalRatio,
        CarvingMask pCarvingMask,
        WorldCarver.CarveSkipChecker pSkipChecker
    ) {
        RandomSource randomsource = RandomSource.create(pSeed);
        int i = randomsource.nextInt(pBranchCount / 2) + pBranchCount / 4;
        boolean flag = randomsource.nextInt(6) == 0;
        float f = 0.0F;
        float f1 = 0.0F;

        for (int j = pBranchIndex; j < pBranchCount; j++) {
            double d0 = 1.5 + Mth.sin((float) Math.PI * j / pBranchCount) * pThickness;
            double d1 = d0 * pHorizontalVerticalRatio;
            float f2 = Mth.cos(pPitch);
            pX += Mth.cos(pYaw) * f2;
            pY += Mth.sin(pPitch);
            pZ += Mth.sin(pYaw) * f2;
            pPitch *= flag ? 0.92F : 0.7F;
            pPitch += f1 * 0.1F;
            pYaw += f * 0.1F;
            f1 *= 0.9F;
            f *= 0.75F;
            f1 += (randomsource.nextFloat() - randomsource.nextFloat()) * randomsource.nextFloat() * 2.0F;
            f += (randomsource.nextFloat() - randomsource.nextFloat()) * randomsource.nextFloat() * 4.0F;
            if (j == i && pThickness > 1.0F) {
                this.createTunnel(
                    pContext,
                    pConfig,
                    pChunk,
                    pBiomeAccessor,
                    randomsource.nextLong(),
                    pAquifer,
                    pX,
                    pY,
                    pZ,
                    pHorizontalRadiusMultiplier,
                    pVerticalRadiusMultiplier,
                    randomsource.nextFloat() * 0.5F + 0.5F,
                    pYaw - (float) (Math.PI / 2),
                    pPitch / 3.0F,
                    j,
                    pBranchCount,
                    1.0,
                    pCarvingMask,
                    pSkipChecker
                );
                this.createTunnel(
                    pContext,
                    pConfig,
                    pChunk,
                    pBiomeAccessor,
                    randomsource.nextLong(),
                    pAquifer,
                    pX,
                    pY,
                    pZ,
                    pHorizontalRadiusMultiplier,
                    pVerticalRadiusMultiplier,
                    randomsource.nextFloat() * 0.5F + 0.5F,
                    pYaw + (float) (Math.PI / 2),
                    pPitch / 3.0F,
                    j,
                    pBranchCount,
                    1.0,
                    pCarvingMask,
                    pSkipChecker
                );
                return;
            }

            if (randomsource.nextInt(4) != 0) {
                if (!canReach(pChunk.getPos(), pX, pZ, j, pBranchCount, pThickness)) {
                    return;
                }

                this.carveEllipsoid(
                    pContext,
                    pConfig,
                    pChunk,
                    pBiomeAccessor,
                    pAquifer,
                    pX,
                    pY,
                    pZ,
                    d0 * pHorizontalRadiusMultiplier,
                    d1 * pVerticalRadiusMultiplier,
                    pCarvingMask,
                    pSkipChecker
                );
            }
        }
    }

    private static boolean shouldSkip(double pRelative, double pRelativeY, double pRelativeZ, double pMinrelativeY) {
        return pRelativeY <= pMinrelativeY ? true : pRelative * pRelative + pRelativeY * pRelativeY + pRelativeZ * pRelativeZ >= 1.0;
    }
}