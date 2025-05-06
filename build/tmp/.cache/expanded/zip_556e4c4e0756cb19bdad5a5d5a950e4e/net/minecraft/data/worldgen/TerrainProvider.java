package net.minecraft.data.worldgen;

import net.minecraft.util.CubicSpline;
import net.minecraft.util.Mth;
import net.minecraft.util.ToFloatFunction;
import net.minecraft.world.level.levelgen.NoiseRouterData;

public class TerrainProvider {
    private static final float DEEP_OCEAN_CONTINENTALNESS = -0.51F;
    private static final float OCEAN_CONTINENTALNESS = -0.4F;
    private static final float PLAINS_CONTINENTALNESS = 0.1F;
    private static final float BEACH_CONTINENTALNESS = -0.15F;
    private static final ToFloatFunction<Float> NO_TRANSFORM = ToFloatFunction.IDENTITY;
    private static final ToFloatFunction<Float> AMPLIFIED_OFFSET = ToFloatFunction.createUnlimited(p_236651_ -> p_236651_ < 0.0F ? p_236651_ : p_236651_ * 2.0F);
    private static final ToFloatFunction<Float> AMPLIFIED_FACTOR = ToFloatFunction.createUnlimited(p_236649_ -> 1.25F - 6.25F / (p_236649_ + 5.0F));
    private static final ToFloatFunction<Float> AMPLIFIED_JAGGEDNESS = ToFloatFunction.createUnlimited(p_236641_ -> p_236641_ * 2.0F);

    public static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> overworldOffset(I pContinents, I pErosion, I pRidgesFolded, boolean pAmplified) {
        ToFloatFunction<Float> tofloatfunction = pAmplified ? AMPLIFIED_OFFSET : NO_TRANSFORM;
        CubicSpline<C, I> cubicspline = buildErosionOffsetSpline(pErosion, pRidgesFolded, -0.15F, 0.0F, 0.0F, 0.1F, 0.0F, -0.03F, false, false, tofloatfunction);
        CubicSpline<C, I> cubicspline1 = buildErosionOffsetSpline(pErosion, pRidgesFolded, -0.1F, 0.03F, 0.1F, 0.1F, 0.01F, -0.03F, false, false, tofloatfunction);
        CubicSpline<C, I> cubicspline2 = buildErosionOffsetSpline(pErosion, pRidgesFolded, -0.1F, 0.03F, 0.1F, 0.7F, 0.01F, -0.03F, true, true, tofloatfunction);
        CubicSpline<C, I> cubicspline3 = buildErosionOffsetSpline(pErosion, pRidgesFolded, -0.05F, 0.03F, 0.1F, 1.0F, 0.01F, 0.01F, true, true, tofloatfunction);
        return CubicSpline.<C, I>builder(pContinents, tofloatfunction)
            .addPoint(-1.1F, 0.044F)
            .addPoint(-1.02F, -0.2222F)
            .addPoint(-0.51F, -0.2222F)
            .addPoint(-0.44F, -0.12F)
            .addPoint(-0.18F, -0.12F)
            .addPoint(-0.16F, cubicspline)
            .addPoint(-0.15F, cubicspline)
            .addPoint(-0.1F, cubicspline1)
            .addPoint(0.25F, cubicspline2)
            .addPoint(1.0F, cubicspline3)
            .build();
    }

    public static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> overworldFactor(I pContinents, I pErosion, I pRidges, I pRidgesFolded, boolean pAmplified) {
        ToFloatFunction<Float> tofloatfunction = pAmplified ? AMPLIFIED_FACTOR : NO_TRANSFORM;
        return CubicSpline.<C, I>builder(pContinents, NO_TRANSFORM)
            .addPoint(-0.19F, 3.95F)
            .addPoint(-0.15F, getErosionFactor(pErosion, pRidges, pRidgesFolded, 6.25F, true, NO_TRANSFORM))
            .addPoint(-0.1F, getErosionFactor(pErosion, pRidges, pRidgesFolded, 5.47F, true, tofloatfunction))
            .addPoint(0.03F, getErosionFactor(pErosion, pRidges, pRidgesFolded, 5.08F, true, tofloatfunction))
            .addPoint(0.06F, getErosionFactor(pErosion, pRidges, pRidgesFolded, 4.69F, false, tofloatfunction))
            .build();
    }

    public static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> overworldJaggedness(I pContinents, I pErosion, I pRidges, I pRidgesFolded, boolean pAmplified) {
        ToFloatFunction<Float> tofloatfunction = pAmplified ? AMPLIFIED_JAGGEDNESS : NO_TRANSFORM;
        float f = 0.65F;
        return CubicSpline.<C, I>builder(pContinents, tofloatfunction)
            .addPoint(-0.11F, 0.0F)
            .addPoint(0.03F, buildErosionJaggednessSpline(pErosion, pRidges, pRidgesFolded, 1.0F, 0.5F, 0.0F, 0.0F, tofloatfunction))
            .addPoint(0.65F, buildErosionJaggednessSpline(pErosion, pRidges, pRidgesFolded, 1.0F, 1.0F, 1.0F, 0.0F, tofloatfunction))
            .build();
    }

    private static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> buildErosionJaggednessSpline(
        I pErosion, I pRidges, I pRidgesFolded, float pHighErosionHighWeirdness, float pLowErosionHighWeirdness, float pHighErosionMidWeirdness, float pLowErosionMidWeirdness, ToFloatFunction<Float> pTransform
    ) {
        float f = -0.5775F;
        CubicSpline<C, I> cubicspline = buildRidgeJaggednessSpline(pRidges, pRidgesFolded, pHighErosionHighWeirdness, pHighErosionMidWeirdness, pTransform);
        CubicSpline<C, I> cubicspline1 = buildRidgeJaggednessSpline(pRidges, pRidgesFolded, pLowErosionHighWeirdness, pLowErosionMidWeirdness, pTransform);
        return CubicSpline.<C, I>builder(pErosion, pTransform)
            .addPoint(-1.0F, cubicspline)
            .addPoint(-0.78F, cubicspline1)
            .addPoint(-0.5775F, cubicspline1)
            .addPoint(-0.375F, 0.0F)
            .build();
    }

    private static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> buildRidgeJaggednessSpline(
        I pRidges, I pRidgesFolded, float pHighWeirdnessMagnitude, float pMidWeirdnessMagnitude, ToFloatFunction<Float> pTransform
    ) {
        float f = NoiseRouterData.peaksAndValleys(0.4F);
        float f1 = NoiseRouterData.peaksAndValleys(0.56666666F);
        float f2 = (f + f1) / 2.0F;
        CubicSpline.Builder<C, I> builder = CubicSpline.builder(pRidgesFolded, pTransform);
        builder.addPoint(f, 0.0F);
        if (pMidWeirdnessMagnitude > 0.0F) {
            builder.addPoint(f2, buildWeirdnessJaggednessSpline(pRidges, pMidWeirdnessMagnitude, pTransform));
        } else {
            builder.addPoint(f2, 0.0F);
        }

        if (pHighWeirdnessMagnitude > 0.0F) {
            builder.addPoint(1.0F, buildWeirdnessJaggednessSpline(pRidges, pHighWeirdnessMagnitude, pTransform));
        } else {
            builder.addPoint(1.0F, 0.0F);
        }

        return builder.build();
    }

    private static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> buildWeirdnessJaggednessSpline(I pRidges, float pMagnitude, ToFloatFunction<Float> pTransform) {
        float f = 0.63F * pMagnitude;
        float f1 = 0.3F * pMagnitude;
        return CubicSpline.<C, I>builder(pRidges, pTransform).addPoint(-0.01F, f).addPoint(0.01F, f1).build();
    }

    private static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> getErosionFactor(
        I pErosion, I pRidges, I pRidgesFolded, float pValue, boolean pHigherValues, ToFloatFunction<Float> pTransform
    ) {
        CubicSpline<C, I> cubicspline = CubicSpline.<C, I>builder(pRidges, pTransform).addPoint(-0.2F, 6.3F).addPoint(0.2F, pValue).build();
        CubicSpline.Builder<C, I> builder = CubicSpline.<C, I>builder(pErosion, pTransform)
            .addPoint(-0.6F, cubicspline)
            .addPoint(-0.5F, CubicSpline.<C, I>builder(pRidges, pTransform).addPoint(-0.05F, 6.3F).addPoint(0.05F, 2.67F).build())
            .addPoint(-0.35F, cubicspline)
            .addPoint(-0.25F, cubicspline)
            .addPoint(-0.1F, CubicSpline.<C, I>builder(pRidges, pTransform).addPoint(-0.05F, 2.67F).addPoint(0.05F, 6.3F).build())
            .addPoint(0.03F, cubicspline);
        if (pHigherValues) {
            CubicSpline<C, I> cubicspline1 = CubicSpline.<C, I>builder(pRidges, pTransform).addPoint(0.0F, pValue).addPoint(0.1F, 0.625F).build();
            CubicSpline<C, I> cubicspline2 = CubicSpline.<C, I>builder(pRidgesFolded, pTransform)
                .addPoint(-0.9F, pValue)
                .addPoint(-0.69F, cubicspline1)
                .build();
            builder.addPoint(0.35F, pValue).addPoint(0.45F, cubicspline2).addPoint(0.55F, cubicspline2).addPoint(0.62F, pValue);
        } else {
            CubicSpline<C, I> cubicspline3 = CubicSpline.<C, I>builder(pRidgesFolded, pTransform)
                .addPoint(-0.7F, cubicspline)
                .addPoint(-0.15F, 1.37F)
                .build();
            CubicSpline<C, I> cubicspline4 = CubicSpline.<C, I>builder(pRidgesFolded, pTransform).addPoint(0.45F, cubicspline).addPoint(0.7F, 1.56F).build();
            builder.addPoint(0.05F, cubicspline4)
                .addPoint(0.4F, cubicspline4)
                .addPoint(0.45F, cubicspline3)
                .addPoint(0.55F, cubicspline3)
                .addPoint(0.58F, pValue);
        }

        return builder.build();
    }

    private static float calculateSlope(float pY1, float pY2, float pX1, float pX2) {
        return (pY2 - pY1) / (pX2 - pX1);
    }

    private static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> buildMountainRidgeSplineWithPoints(
        I pRidgesFolded, float pMagnitude, boolean pUseMaxSlope, ToFloatFunction<Float> pTransform
    ) {
        CubicSpline.Builder<C, I> builder = CubicSpline.builder(pRidgesFolded, pTransform);
        float f = -0.7F;
        float f1 = -1.0F;
        float f2 = mountainContinentalness(-1.0F, pMagnitude, -0.7F);
        float f3 = 1.0F;
        float f4 = mountainContinentalness(1.0F, pMagnitude, -0.7F);
        float f5 = calculateMountainRidgeZeroContinentalnessPoint(pMagnitude);
        float f6 = -0.65F;
        if (-0.65F < f5 && f5 < 1.0F) {
            float f14 = mountainContinentalness(-0.65F, pMagnitude, -0.7F);
            float f8 = -0.75F;
            float f9 = mountainContinentalness(-0.75F, pMagnitude, -0.7F);
            float f10 = calculateSlope(f2, f9, -1.0F, -0.75F);
            builder.addPoint(-1.0F, f2, f10);
            builder.addPoint(-0.75F, f9);
            builder.addPoint(-0.65F, f14);
            float f11 = mountainContinentalness(f5, pMagnitude, -0.7F);
            float f12 = calculateSlope(f11, f4, f5, 1.0F);
            float f13 = 0.01F;
            builder.addPoint(f5 - 0.01F, f11);
            builder.addPoint(f5, f11, f12);
            builder.addPoint(1.0F, f4, f12);
        } else {
            float f7 = calculateSlope(f2, f4, -1.0F, 1.0F);
            if (pUseMaxSlope) {
                builder.addPoint(-1.0F, Math.max(0.2F, f2));
                builder.addPoint(0.0F, Mth.lerp(0.5F, f2, f4), f7);
            } else {
                builder.addPoint(-1.0F, f2, f7);
            }

            builder.addPoint(1.0F, f4, f7);
        }

        return builder.build();
    }

    private static float mountainContinentalness(float pHeightFactor, float pMagnitude, float pCutoffHeight) {
        float f = 1.17F;
        float f1 = 0.46082947F;
        float f2 = 1.0F - (1.0F - pMagnitude) * 0.5F;
        float f3 = 0.5F * (1.0F - pMagnitude);
        float f4 = (pHeightFactor + 1.17F) * 0.46082947F;
        float f5 = f4 * f2 - f3;
        return pHeightFactor < pCutoffHeight ? Math.max(f5, -0.2222F) : Math.max(f5, 0.0F);
    }

    private static float calculateMountainRidgeZeroContinentalnessPoint(float pInput) {
        float f = 1.17F;
        float f1 = 0.46082947F;
        float f2 = 1.0F - (1.0F - pInput) * 0.5F;
        float f3 = 0.5F * (1.0F - pInput);
        return f3 / (0.46082947F * f2) - 1.17F;
    }

    public static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> buildErosionOffsetSpline(
        I pErosion,
        I pRidgesFolded,
        float p_236598_,
        float p_236599_,
        float p_236600_,
        float pMagnitude,
        float p_236602_,
        float p_236603_,
        boolean pExtended,
        boolean pUseMaxSlope,
        ToFloatFunction<Float> pTransform
    ) {
        float f = 0.6F;
        float f1 = 0.5F;
        float f2 = 0.5F;
        CubicSpline<C, I> cubicspline = buildMountainRidgeSplineWithPoints(pRidgesFolded, Mth.lerp(pMagnitude, 0.6F, 1.5F), pUseMaxSlope, pTransform);
        CubicSpline<C, I> cubicspline1 = buildMountainRidgeSplineWithPoints(pRidgesFolded, Mth.lerp(pMagnitude, 0.6F, 1.0F), pUseMaxSlope, pTransform);
        CubicSpline<C, I> cubicspline2 = buildMountainRidgeSplineWithPoints(pRidgesFolded, pMagnitude, pUseMaxSlope, pTransform);
        CubicSpline<C, I> cubicspline3 = ridgeSpline(
            pRidgesFolded, p_236598_ - 0.15F, 0.5F * pMagnitude, Mth.lerp(0.5F, 0.5F, 0.5F) * pMagnitude, 0.5F * pMagnitude, 0.6F * pMagnitude, 0.5F, pTransform
        );
        CubicSpline<C, I> cubicspline4 = ridgeSpline(
            pRidgesFolded, p_236598_, p_236602_ * pMagnitude, p_236599_ * pMagnitude, 0.5F * pMagnitude, 0.6F * pMagnitude, 0.5F, pTransform
        );
        CubicSpline<C, I> cubicspline5 = ridgeSpline(pRidgesFolded, p_236598_, p_236602_, p_236602_, p_236599_, p_236600_, 0.5F, pTransform);
        CubicSpline<C, I> cubicspline6 = ridgeSpline(pRidgesFolded, p_236598_, p_236602_, p_236602_, p_236599_, p_236600_, 0.5F, pTransform);
        CubicSpline<C, I> cubicspline7 = CubicSpline.<C, I>builder(pRidgesFolded, pTransform)
            .addPoint(-1.0F, p_236598_)
            .addPoint(-0.4F, cubicspline5)
            .addPoint(0.0F, p_236600_ + 0.07F)
            .build();
        CubicSpline<C, I> cubicspline8 = ridgeSpline(pRidgesFolded, -0.02F, p_236603_, p_236603_, p_236599_, p_236600_, 0.0F, pTransform);
        CubicSpline.Builder<C, I> builder = CubicSpline.<C, I>builder(pErosion, pTransform)
            .addPoint(-0.85F, cubicspline)
            .addPoint(-0.7F, cubicspline1)
            .addPoint(-0.4F, cubicspline2)
            .addPoint(-0.35F, cubicspline3)
            .addPoint(-0.1F, cubicspline4)
            .addPoint(0.2F, cubicspline5);
        if (pExtended) {
            builder.addPoint(0.4F, cubicspline6).addPoint(0.45F, cubicspline7).addPoint(0.55F, cubicspline7).addPoint(0.58F, cubicspline6);
        }

        builder.addPoint(0.7F, cubicspline8);
        return builder.build();
    }

    private static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> ridgeSpline(
        I pRidgesFolded, float pY1, float pY2, float pY3, float pY4, float pY5, float pMinSmoothing, ToFloatFunction<Float> pTransform
    ) {
        float f = Math.max(0.5F * (pY2 - pY1), pMinSmoothing);
        float f1 = 5.0F * (pY3 - pY2);
        return CubicSpline.<C, I>builder(pRidgesFolded, pTransform)
            .addPoint(-1.0F, pY1, f)
            .addPoint(-0.4F, pY2, Math.min(f, f1))
            .addPoint(0.0F, pY3, f1)
            .addPoint(0.4F, pY4, 2.0F * (pY4 - pY3))
            .addPoint(1.0F, pY5, 0.7F * (pY5 - pY4))
            .build();
    }
}