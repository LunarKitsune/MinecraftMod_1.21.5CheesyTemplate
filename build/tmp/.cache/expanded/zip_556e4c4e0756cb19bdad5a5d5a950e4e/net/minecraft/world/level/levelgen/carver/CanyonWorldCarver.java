package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;

/**
 * A carver responsible for creating ravines, or canyons.
 */
public class CanyonWorldCarver extends WorldCarver<CanyonCarverConfiguration> {
    public CanyonWorldCarver(Codec<CanyonCarverConfiguration> p_64711_) {
        super(p_64711_);
    }

    public boolean isStartChunk(CanyonCarverConfiguration p_224797_, RandomSource p_224798_) {
        return p_224798_.nextFloat() <= p_224797_.probability;
    }

    public boolean carve(
        CarvingContext p_224813_,
        CanyonCarverConfiguration p_224814_,
        ChunkAccess p_224815_,
        Function<BlockPos, Holder<Biome>> p_224816_,
        RandomSource p_224817_,
        Aquifer p_224818_,
        ChunkPos p_224819_,
        CarvingMask p_224820_
    ) {
        int i = (this.getRange() * 2 - 1) * 16;
        double d0 = p_224819_.getBlockX(p_224817_.nextInt(16));
        int j = p_224814_.y.sample(p_224817_, p_224813_);
        double d1 = p_224819_.getBlockZ(p_224817_.nextInt(16));
        float f = p_224817_.nextFloat() * (float) (Math.PI * 2);
        float f1 = p_224814_.verticalRotation.sample(p_224817_);
        double d2 = p_224814_.yScale.sample(p_224817_);
        float f2 = p_224814_.shape.thickness.sample(p_224817_);
        int k = (int)(i * p_224814_.shape.distanceFactor.sample(p_224817_));
        int l = 0;
        this.doCarve(p_224813_, p_224814_, p_224815_, p_224816_, p_224817_.nextLong(), p_224818_, d0, j, d1, f2, f, f1, 0, k, d2, p_224820_);
        return true;
    }

    private void doCarve(
        CarvingContext pContext,
        CanyonCarverConfiguration pConfig,
        ChunkAccess pChunk,
        Function<BlockPos, Holder<Biome>> pBiomeAccessor,
        long pSeed,
        Aquifer pAquifer,
        double pX,
        double pY,
        double pZ,
        float pThickness,
        float pYaw,
        float pPitch,
        int pBranchIndex,
        int pBranchCount,
        double pHorizontalVerticalRatio,
        CarvingMask pCarvingMask
    ) {
        RandomSource randomsource = RandomSource.create(pSeed);
        float[] afloat = this.initWidthFactors(pContext, pConfig, randomsource);
        float f = 0.0F;
        float f1 = 0.0F;

        for (int i = pBranchIndex; i < pBranchCount; i++) {
            double d0 = 1.5 + Mth.sin(i * (float) Math.PI / pBranchCount) * pThickness;
            double d1 = d0 * pHorizontalVerticalRatio;
            d0 *= pConfig.shape.horizontalRadiusFactor.sample(randomsource);
            d1 = this.updateVerticalRadius(pConfig, randomsource, d1, pBranchCount, i);
            float f2 = Mth.cos(pPitch);
            float f3 = Mth.sin(pPitch);
            pX += Mth.cos(pYaw) * f2;
            pY += f3;
            pZ += Mth.sin(pYaw) * f2;
            pPitch *= 0.7F;
            pPitch += f1 * 0.05F;
            pYaw += f * 0.05F;
            f1 *= 0.8F;
            f *= 0.5F;
            f1 += (randomsource.nextFloat() - randomsource.nextFloat()) * randomsource.nextFloat() * 2.0F;
            f += (randomsource.nextFloat() - randomsource.nextFloat()) * randomsource.nextFloat() * 4.0F;
            if (randomsource.nextInt(4) != 0) {
                if (!canReach(pChunk.getPos(), pX, pZ, i, pBranchCount, pThickness)) {
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
                    d0,
                    d1,
                    pCarvingMask,
                    (p_159082_, p_159083_, p_159084_, p_159085_, p_159086_) -> this.shouldSkip(p_159082_, afloat, p_159083_, p_159084_, p_159085_, p_159086_)
                );
            }
        }
    }

    private float[] initWidthFactors(CarvingContext pContext, CanyonCarverConfiguration pConfig, RandomSource pRandom) {
        int i = pContext.getGenDepth();
        float[] afloat = new float[i];
        float f = 1.0F;

        for (int j = 0; j < i; j++) {
            if (j == 0 || pRandom.nextInt(pConfig.shape.widthSmoothness) == 0) {
                f = 1.0F + pRandom.nextFloat() * pRandom.nextFloat();
            }

            afloat[j] = f * f;
        }

        return afloat;
    }

    private double updateVerticalRadius(CanyonCarverConfiguration pConfig, RandomSource pRandom, double pVerticalRadius, float pBranchCount, float pCurrentBranch) {
        float f = 1.0F - Mth.abs(0.5F - pCurrentBranch / pBranchCount) * 2.0F;
        float f1 = pConfig.shape.verticalRadiusDefaultFactor + pConfig.shape.verticalRadiusCenterFactor * f;
        return f1 * pVerticalRadius * Mth.randomBetween(pRandom, 0.75F, 1.0F);
    }

    private boolean shouldSkip(CarvingContext pContext, float[] pWidthFactors, double pRelativeX, double pRelativeY, double pRelativeZ, int pY) {
        int i = pY - pContext.getMinGenY();
        return (pRelativeX * pRelativeX + pRelativeZ * pRelativeZ) * pWidthFactors[i - 1] + pRelativeY * pRelativeY / 6.0 >= 1.0;
    }
}