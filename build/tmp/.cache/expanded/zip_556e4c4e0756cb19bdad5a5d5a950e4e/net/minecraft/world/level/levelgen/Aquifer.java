package net.minecraft.world.level.levelgen;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import org.apache.commons.lang3.mutable.MutableDouble;

/**
 * Aquifers are responsible for non-sea level fluids found in terrain generation, but also managing that different
 * aquifers don't intersect with each other in ways that would create undesirable fluid placement.
 * The aquifer interface itself is a modifier on a per-block basis. It computes a block state to be placed for each
 * position in the world.
 * <p>
 * Aquifers work by first partitioning a single chunk into a low resolution grid. They then generate, via various noise
 * layers, an {@link NoiseBasedAquifer.AquiferStatus} at each grid point.
 * At each point, the grid cell containing that point is calculated, and then of the eight grid corners, the three
 * closest aquifers are found, by square euclidean distance.
 * Borders between aquifers are created by comparing nearby aquifers to see if the given point is near-equidistant from
 * them, indicating a border if so, or fluid/air depending on the aquifer height if not.
 */
public interface Aquifer {
    static Aquifer create(
        NoiseChunk pChunk,
        ChunkPos pChunkPos,
        NoiseRouter pNoiseRouter,
        PositionalRandomFactory pPositionalRandomFactory,
        int pMinY,
        int pHeight,
        Aquifer.FluidPicker pGlobalFluidPicker
    ) {
        return new Aquifer.NoiseBasedAquifer(pChunk, pChunkPos, pNoiseRouter, pPositionalRandomFactory, pMinY, pHeight, pGlobalFluidPicker);
    }

    static Aquifer createDisabled(final Aquifer.FluidPicker pDefaultFluid) {
        return new Aquifer() {
            @Nullable
            @Override
            public BlockState computeSubstance(DensityFunction.FunctionContext p_208172_, double p_208173_) {
                return p_208173_ > 0.0
                    ? null
                    : pDefaultFluid.computeFluid(p_208172_.blockX(), p_208172_.blockY(), p_208172_.blockZ()).at(p_208172_.blockY());
            }

            /**
             * Returns {@code true} if there should be a fluid update scheduled - due to a fluid block being placed in a
             * possibly unsteady position - at the last position passed into {@link #computeState}.
             * This <strong>must</strong> be invoked only after {@link #computeState}, and will be using the same
             * parameters as that method.
             */
            @Override
            public boolean shouldScheduleFluidUpdate() {
                return false;
            }
        };
    }

    @Nullable
    BlockState computeSubstance(DensityFunction.FunctionContext pContext, double pSubstance);

    boolean shouldScheduleFluidUpdate();

    public interface FluidPicker {
        Aquifer.FluidStatus computeFluid(int pX, int pY, int pZ);
    }

    public record FluidStatus(int fluidLevel, BlockState fluidType) {
        public BlockState at(int pY) {
            return pY < this.fluidLevel ? this.fluidType : Blocks.AIR.defaultBlockState();
        }
    }

    public static class NoiseBasedAquifer implements Aquifer {
        private static final int X_RANGE = 10;
        private static final int Y_RANGE = 9;
        private static final int Z_RANGE = 10;
        private static final int X_SEPARATION = 6;
        private static final int Y_SEPARATION = 3;
        private static final int Z_SEPARATION = 6;
        private static final int X_SPACING = 16;
        private static final int Y_SPACING = 12;
        private static final int Z_SPACING = 16;
        private static final int MAX_REASONABLE_DISTANCE_TO_AQUIFER_CENTER = 11;
        private static final double FLOWING_UPDATE_SIMULARITY = similarity(Mth.square(10), Mth.square(12));
        private final NoiseChunk noiseChunk;
        protected final DensityFunction barrierNoise;
        private final DensityFunction fluidLevelFloodednessNoise;
        private final DensityFunction fluidLevelSpreadNoise;
        protected final DensityFunction lavaNoise;
        private final PositionalRandomFactory positionalRandomFactory;
        protected final Aquifer.FluidStatus[] aquiferCache;
        protected final long[] aquiferLocationCache;
        private final Aquifer.FluidPicker globalFluidPicker;
        private final DensityFunction erosion;
        private final DensityFunction depth;
        protected boolean shouldScheduleFluidUpdate;
        protected final int minGridX;
        protected final int minGridY;
        protected final int minGridZ;
        protected final int gridSizeX;
        protected final int gridSizeZ;
        private static final int[][] SURFACE_SAMPLING_OFFSETS_IN_CHUNKS = new int[][]{
            {0, 0}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {-3, 0}, {-2, 0}, {-1, 0}, {1, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}
        };

        NoiseBasedAquifer(
            NoiseChunk pNoiseChunk,
            ChunkPos pChunkPos,
            NoiseRouter pNoiseRouter,
            PositionalRandomFactory pPositionalRandomFactory,
            int pMinY,
            int pHeight,
            Aquifer.FluidPicker pGlobalFluidPicker
        ) {
            this.noiseChunk = pNoiseChunk;
            this.barrierNoise = pNoiseRouter.barrierNoise();
            this.fluidLevelFloodednessNoise = pNoiseRouter.fluidLevelFloodednessNoise();
            this.fluidLevelSpreadNoise = pNoiseRouter.fluidLevelSpreadNoise();
            this.lavaNoise = pNoiseRouter.lavaNoise();
            this.erosion = pNoiseRouter.erosion();
            this.depth = pNoiseRouter.depth();
            this.positionalRandomFactory = pPositionalRandomFactory;
            this.minGridX = this.gridX(pChunkPos.getMinBlockX()) - 1;
            this.globalFluidPicker = pGlobalFluidPicker;
            int i = this.gridX(pChunkPos.getMaxBlockX()) + 1;
            this.gridSizeX = i - this.minGridX + 1;
            this.minGridY = this.gridY(pMinY) - 1;
            int j = this.gridY(pMinY + pHeight) + 1;
            int k = j - this.minGridY + 1;
            this.minGridZ = this.gridZ(pChunkPos.getMinBlockZ()) - 1;
            int l = this.gridZ(pChunkPos.getMaxBlockZ()) + 1;
            this.gridSizeZ = l - this.minGridZ + 1;
            int i1 = this.gridSizeX * k * this.gridSizeZ;
            this.aquiferCache = new Aquifer.FluidStatus[i1];
            this.aquiferLocationCache = new long[i1];
            Arrays.fill(this.aquiferLocationCache, Long.MAX_VALUE);
        }

        protected int getIndex(int pGridX, int pGridY, int pGridZ) {
            int i = pGridX - this.minGridX;
            int j = pGridY - this.minGridY;
            int k = pGridZ - this.minGridZ;
            return (j * this.gridSizeZ + k) * this.gridSizeX + i;
        }

        @Nullable
        @Override
        public BlockState computeSubstance(DensityFunction.FunctionContext p_208186_, double p_208187_) {
            int i = p_208186_.blockX();
            int j = p_208186_.blockY();
            int k = p_208186_.blockZ();
            if (p_208187_ > 0.0) {
                this.shouldScheduleFluidUpdate = false;
                return null;
            } else {
                Aquifer.FluidStatus aquifer$fluidstatus = this.globalFluidPicker.computeFluid(i, j, k);
                if (aquifer$fluidstatus.at(j).is(Blocks.LAVA)) {
                    this.shouldScheduleFluidUpdate = false;
                    return Blocks.LAVA.defaultBlockState();
                } else {
                    int l = Math.floorDiv(i - 5, 16);
                    int i1 = Math.floorDiv(j + 1, 12);
                    int j1 = Math.floorDiv(k - 5, 16);
                    int k1 = Integer.MAX_VALUE;
                    int l1 = Integer.MAX_VALUE;
                    int i2 = Integer.MAX_VALUE;
                    int j2 = Integer.MAX_VALUE;
                    long k2 = 0L;
                    long l2 = 0L;
                    long i3 = 0L;
                    long j3 = 0L;

                    for (int k3 = 0; k3 <= 1; k3++) {
                        for (int l3 = -1; l3 <= 1; l3++) {
                            for (int i4 = 0; i4 <= 1; i4++) {
                                int j4 = l + k3;
                                int k4 = i1 + l3;
                                int l4 = j1 + i4;
                                int i5 = this.getIndex(j4, k4, l4);
                                long k5 = this.aquiferLocationCache[i5];
                                long j5;
                                if (k5 != Long.MAX_VALUE) {
                                    j5 = k5;
                                } else {
                                    RandomSource randomsource = this.positionalRandomFactory.at(j4, k4, l4);
                                    j5 = BlockPos.asLong(
                                        j4 * 16 + randomsource.nextInt(10), k4 * 12 + randomsource.nextInt(9), l4 * 16 + randomsource.nextInt(10)
                                    );
                                    this.aquiferLocationCache[i5] = j5;
                                }

                                int k6 = BlockPos.getX(j5) - i;
                                int l5 = BlockPos.getY(j5) - j;
                                int i6 = BlockPos.getZ(j5) - k;
                                int j6 = k6 * k6 + l5 * l5 + i6 * i6;
                                if (k1 >= j6) {
                                    j3 = i3;
                                    i3 = l2;
                                    l2 = k2;
                                    k2 = j5;
                                    j2 = i2;
                                    i2 = l1;
                                    l1 = k1;
                                    k1 = j6;
                                } else if (l1 >= j6) {
                                    j3 = i3;
                                    i3 = l2;
                                    l2 = j5;
                                    j2 = i2;
                                    i2 = l1;
                                    l1 = j6;
                                } else if (i2 >= j6) {
                                    j3 = i3;
                                    i3 = j5;
                                    j2 = i2;
                                    i2 = j6;
                                } else if (j2 >= j6) {
                                    j3 = j5;
                                    j2 = j6;
                                }
                            }
                        }
                    }

                    Aquifer.FluidStatus aquifer$fluidstatus1 = this.getAquiferStatus(k2);
                    double d1 = similarity(k1, l1);
                    BlockState blockstate = aquifer$fluidstatus1.at(j);
                    if (d1 <= 0.0) {
                        if (d1 >= FLOWING_UPDATE_SIMULARITY) {
                            Aquifer.FluidStatus aquifer$fluidstatus2 = this.getAquiferStatus(l2);
                            this.shouldScheduleFluidUpdate = !aquifer$fluidstatus1.equals(aquifer$fluidstatus2);
                        } else {
                            this.shouldScheduleFluidUpdate = false;
                        }

                        return blockstate;
                    } else if (blockstate.is(Blocks.WATER) && this.globalFluidPicker.computeFluid(i, j - 1, k).at(j - 1).is(Blocks.LAVA)) {
                        this.shouldScheduleFluidUpdate = true;
                        return blockstate;
                    } else {
                        MutableDouble mutabledouble = new MutableDouble(Double.NaN);
                        Aquifer.FluidStatus aquifer$fluidstatus3 = this.getAquiferStatus(l2);
                        double d2 = d1 * this.calculatePressure(p_208186_, mutabledouble, aquifer$fluidstatus1, aquifer$fluidstatus3);
                        if (p_208187_ + d2 > 0.0) {
                            this.shouldScheduleFluidUpdate = false;
                            return null;
                        } else {
                            Aquifer.FluidStatus aquifer$fluidstatus4 = this.getAquiferStatus(i3);
                            double d0 = similarity(k1, i2);
                            if (d0 > 0.0) {
                                double d3 = d1 * d0 * this.calculatePressure(p_208186_, mutabledouble, aquifer$fluidstatus1, aquifer$fluidstatus4);
                                if (p_208187_ + d3 > 0.0) {
                                    this.shouldScheduleFluidUpdate = false;
                                    return null;
                                }
                            }

                            double d4 = similarity(l1, i2);
                            if (d4 > 0.0) {
                                double d5 = d1 * d4 * this.calculatePressure(p_208186_, mutabledouble, aquifer$fluidstatus3, aquifer$fluidstatus4);
                                if (p_208187_ + d5 > 0.0) {
                                    this.shouldScheduleFluidUpdate = false;
                                    return null;
                                }
                            }

                            boolean flag2 = !aquifer$fluidstatus1.equals(aquifer$fluidstatus3);
                            boolean flag = d4 >= FLOWING_UPDATE_SIMULARITY && !aquifer$fluidstatus3.equals(aquifer$fluidstatus4);
                            boolean flag1 = d0 >= FLOWING_UPDATE_SIMULARITY && !aquifer$fluidstatus1.equals(aquifer$fluidstatus4);
                            if (!flag2 && !flag && !flag1) {
                                this.shouldScheduleFluidUpdate = d0 >= FLOWING_UPDATE_SIMULARITY && similarity(k1, j2) >= FLOWING_UPDATE_SIMULARITY && !aquifer$fluidstatus1.equals(this.getAquiferStatus(j3));
                            } else {
                                this.shouldScheduleFluidUpdate = true;
                            }

                            return blockstate;
                        }
                    }
                }
            }
        }

        @Override
        public boolean shouldScheduleFluidUpdate() {
            return this.shouldScheduleFluidUpdate;
        }

        protected static double similarity(int pFirstDistance, int pSecondDistance) {
            double d0 = 25.0;
            return 1.0 - Math.abs(pSecondDistance - pFirstDistance) / 25.0;
        }

        private double calculatePressure(
            DensityFunction.FunctionContext pContext, MutableDouble pSubstance, Aquifer.FluidStatus pFirstFluid, Aquifer.FluidStatus pSecondFluid
        ) {
            int i = pContext.blockY();
            BlockState blockstate = pFirstFluid.at(i);
            BlockState blockstate1 = pSecondFluid.at(i);
            if ((!blockstate.is(Blocks.LAVA) || !blockstate1.is(Blocks.WATER))
                && (!blockstate.is(Blocks.WATER) || !blockstate1.is(Blocks.LAVA))) {
                int j = Math.abs(pFirstFluid.fluidLevel - pSecondFluid.fluidLevel);
                if (j == 0) {
                    return 0.0;
                } else {
                    double d0 = 0.5 * (pFirstFluid.fluidLevel + pSecondFluid.fluidLevel);
                    double d1 = i + 0.5 - d0;
                    double d2 = j / 2.0;
                    double d3 = 0.0;
                    double d4 = 2.5;
                    double d5 = 1.5;
                    double d6 = 3.0;
                    double d7 = 10.0;
                    double d8 = 3.0;
                    double d9 = d2 - Math.abs(d1);
                    double d10;
                    if (d1 > 0.0) {
                        double d11 = 0.0 + d9;
                        if (d11 > 0.0) {
                            d10 = d11 / 1.5;
                        } else {
                            d10 = d11 / 2.5;
                        }
                    } else {
                        double d15 = 3.0 + d9;
                        if (d15 > 0.0) {
                            d10 = d15 / 3.0;
                        } else {
                            d10 = d15 / 10.0;
                        }
                    }

                    double d16 = 2.0;
                    double d12;
                    if (!(d10 < -2.0) && !(d10 > 2.0)) {
                        double d13 = pSubstance.getValue();
                        if (Double.isNaN(d13)) {
                            double d14 = this.barrierNoise.compute(pContext);
                            pSubstance.setValue(d14);
                            d12 = d14;
                        } else {
                            d12 = d13;
                        }
                    } else {
                        d12 = 0.0;
                    }

                    return 2.0 * (d12 + d10);
                }
            } else {
                return 2.0;
            }
        }

        protected int gridX(int pX) {
            return Math.floorDiv(pX, 16);
        }

        protected int gridY(int pY) {
            return Math.floorDiv(pY, 12);
        }

        protected int gridZ(int pZ) {
            return Math.floorDiv(pZ, 16);
        }

        private Aquifer.FluidStatus getAquiferStatus(long pPackedPos) {
            int i = BlockPos.getX(pPackedPos);
            int j = BlockPos.getY(pPackedPos);
            int k = BlockPos.getZ(pPackedPos);
            int l = this.gridX(i);
            int i1 = this.gridY(j);
            int j1 = this.gridZ(k);
            int k1 = this.getIndex(l, i1, j1);
            Aquifer.FluidStatus aquifer$fluidstatus = this.aquiferCache[k1];
            if (aquifer$fluidstatus != null) {
                return aquifer$fluidstatus;
            } else {
                Aquifer.FluidStatus aquifer$fluidstatus1 = this.computeFluid(i, j, k);
                this.aquiferCache[k1] = aquifer$fluidstatus1;
                return aquifer$fluidstatus1;
            }
        }

        private Aquifer.FluidStatus computeFluid(int pX, int pY, int pZ) {
            Aquifer.FluidStatus aquifer$fluidstatus = this.globalFluidPicker.computeFluid(pX, pY, pZ);
            int i = Integer.MAX_VALUE;
            int j = pY + 12;
            int k = pY - 12;
            boolean flag = false;

            for (int[] aint : SURFACE_SAMPLING_OFFSETS_IN_CHUNKS) {
                int l = pX + SectionPos.sectionToBlockCoord(aint[0]);
                int i1 = pZ + SectionPos.sectionToBlockCoord(aint[1]);
                int j1 = this.noiseChunk.preliminarySurfaceLevel(l, i1);
                int k1 = j1 + 8;
                boolean flag1 = aint[0] == 0 && aint[1] == 0;
                if (flag1 && k > k1) {
                    return aquifer$fluidstatus;
                }

                boolean flag2 = j > k1;
                if (flag2 || flag1) {
                    Aquifer.FluidStatus aquifer$fluidstatus1 = this.globalFluidPicker.computeFluid(l, k1, i1);
                    if (!aquifer$fluidstatus1.at(k1).isAir()) {
                        if (flag1) {
                            flag = true;
                        }

                        if (flag2) {
                            return aquifer$fluidstatus1;
                        }
                    }
                }

                i = Math.min(i, j1);
            }

            int l1 = this.computeSurfaceLevel(pX, pY, pZ, aquifer$fluidstatus, i, flag);
            return new Aquifer.FluidStatus(l1, this.computeFluidType(pX, pY, pZ, aquifer$fluidstatus, l1));
        }

        private int computeSurfaceLevel(int pX, int pY, int pZ, Aquifer.FluidStatus pFluidStatus, int pMaxSurfaceLevel, boolean pFluidPresent) {
            DensityFunction.SinglePointContext densityfunction$singlepointcontext = new DensityFunction.SinglePointContext(pX, pY, pZ);
            double d0;
            double d1;
            if (OverworldBiomeBuilder.isDeepDarkRegion(this.erosion, this.depth, densityfunction$singlepointcontext)) {
                d0 = -1.0;
                d1 = -1.0;
            } else {
                int i = pMaxSurfaceLevel + 8 - pY;
                int j = 64;
                double d2 = pFluidPresent ? Mth.clampedMap(i, 0.0, 64.0, 1.0, 0.0) : 0.0;
                double d3 = Mth.clamp(this.fluidLevelFloodednessNoise.compute(densityfunction$singlepointcontext), -1.0, 1.0);
                double d4 = Mth.map(d2, 1.0, 0.0, -0.3, 0.8);
                double d5 = Mth.map(d2, 1.0, 0.0, -0.8, 0.4);
                d0 = d3 - d5;
                d1 = d3 - d4;
            }

            int k;
            if (d1 > 0.0) {
                k = pFluidStatus.fluidLevel;
            } else if (d0 > 0.0) {
                k = this.computeRandomizedFluidSurfaceLevel(pX, pY, pZ, pMaxSurfaceLevel);
            } else {
                k = DimensionType.WAY_BELOW_MIN_Y;
            }

            return k;
        }

        private int computeRandomizedFluidSurfaceLevel(int pX, int pY, int pZ, int pMaxSurfaceLevel) {
            int i = 16;
            int j = 40;
            int k = Math.floorDiv(pX, 16);
            int l = Math.floorDiv(pY, 40);
            int i1 = Math.floorDiv(pZ, 16);
            int j1 = l * 40 + 20;
            int k1 = 10;
            double d0 = this.fluidLevelSpreadNoise.compute(new DensityFunction.SinglePointContext(k, l, i1)) * 10.0;
            int l1 = Mth.quantize(d0, 3);
            int i2 = j1 + l1;
            return Math.min(pMaxSurfaceLevel, i2);
        }

        private BlockState computeFluidType(int pX, int pY, int pZ, Aquifer.FluidStatus pFluidStatus, int pSurfaceLevel) {
            BlockState blockstate = pFluidStatus.fluidType;
            if (pSurfaceLevel <= -10 && pSurfaceLevel != DimensionType.WAY_BELOW_MIN_Y && pFluidStatus.fluidType != Blocks.LAVA.defaultBlockState()) {
                int i = 64;
                int j = 40;
                int k = Math.floorDiv(pX, 64);
                int l = Math.floorDiv(pY, 40);
                int i1 = Math.floorDiv(pZ, 64);
                double d0 = this.lavaNoise.compute(new DensityFunction.SinglePointContext(k, l, i1));
                if (Math.abs(d0) > 0.3) {
                    blockstate = Blocks.LAVA.defaultBlockState();
                }
            }

            return blockstate;
        }
    }
}