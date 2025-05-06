package net.minecraft.world.level;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface BlockGetter extends LevelHeightAccessor, net.minecraftforge.common.extensions.IForgeBlockGetter {
    int MAX_BLOCK_ITERATIONS_ALONG_TRAVEL = 16;

    @Nullable
    BlockEntity getBlockEntity(BlockPos pPos);

    default <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pPos, BlockEntityType<T> pBlockEntityType) {
        BlockEntity blockentity = this.getBlockEntity(pPos);
        return blockentity != null && blockentity.getType() == pBlockEntityType ? Optional.of((T)blockentity) : Optional.empty();
    }

    BlockState getBlockState(BlockPos pPos);

    FluidState getFluidState(BlockPos pPos);

    default int getLightEmission(BlockPos pPos) {
        return this.getBlockState(pPos).getLightEmission(this, pPos);
    }

    default Stream<BlockState> getBlockStates(AABB pArea) {
        return BlockPos.betweenClosedStream(pArea).map(this::getBlockState);
    }

    default BlockHitResult isBlockInLine(ClipBlockStateContext pContext) {
        return traverseBlocks(
            pContext.getFrom(),
            pContext.getTo(),
            pContext,
            (p_275154_, p_275155_) -> {
                BlockState blockstate = this.getBlockState(p_275155_);
                Vec3 vec3 = p_275154_.getFrom().subtract(p_275154_.getTo());
                return p_275154_.isTargetBlock().test(blockstate)
                    ? new BlockHitResult(
                        p_275154_.getTo(),
                        Direction.getApproximateNearest(vec3.x, vec3.y, vec3.z),
                        BlockPos.containing(p_275154_.getTo()),
                        false
                    )
                    : null;
            },
            p_275156_ -> {
                Vec3 vec3 = p_275156_.getFrom().subtract(p_275156_.getTo());
                return BlockHitResult.miss(
                    p_275156_.getTo(), Direction.getApproximateNearest(vec3.x, vec3.y, vec3.z), BlockPos.containing(p_275156_.getTo())
                );
            }
        );
    }

    default BlockHitResult clip(ClipContext pContext) {
        return traverseBlocks(
            pContext.getFrom(),
            pContext.getTo(),
            pContext,
            (p_151359_, p_151360_) -> {
                BlockState blockstate = this.getBlockState(p_151360_);
                FluidState fluidstate = this.getFluidState(p_151360_);
                Vec3 vec3 = p_151359_.getFrom();
                Vec3 vec31 = p_151359_.getTo();
                VoxelShape voxelshape = p_151359_.getBlockShape(blockstate, this, p_151360_);
                BlockHitResult blockhitresult = this.clipWithInteractionOverride(vec3, vec31, p_151360_, voxelshape, blockstate);
                VoxelShape voxelshape1 = p_151359_.getFluidShape(fluidstate, this, p_151360_);
                BlockHitResult blockhitresult1 = voxelshape1.clip(vec3, vec31, p_151360_);
                double d0 = blockhitresult == null ? Double.MAX_VALUE : p_151359_.getFrom().distanceToSqr(blockhitresult.getLocation());
                double d1 = blockhitresult1 == null ? Double.MAX_VALUE : p_151359_.getFrom().distanceToSqr(blockhitresult1.getLocation());
                return d0 <= d1 ? blockhitresult : blockhitresult1;
            },
            p_275153_ -> {
                Vec3 vec3 = p_275153_.getFrom().subtract(p_275153_.getTo());
                return BlockHitResult.miss(
                    p_275153_.getTo(), Direction.getApproximateNearest(vec3.x, vec3.y, vec3.z), BlockPos.containing(p_275153_.getTo())
                );
            }
        );
    }

    @Nullable
    default BlockHitResult clipWithInteractionOverride(Vec3 pStartVec, Vec3 pEndVec, BlockPos pPos, VoxelShape pShape, BlockState pState) {
        BlockHitResult blockhitresult = pShape.clip(pStartVec, pEndVec, pPos);
        if (blockhitresult != null) {
            BlockHitResult blockhitresult1 = pState.getInteractionShape(this, pPos).clip(pStartVec, pEndVec, pPos);
            if (blockhitresult1 != null && blockhitresult1.getLocation().subtract(pStartVec).lengthSqr() < blockhitresult.getLocation().subtract(pStartVec).lengthSqr()) {
                return blockhitresult.withDirection(blockhitresult1.getDirection());
            }
        }

        return blockhitresult;
    }

    default double getBlockFloorHeight(VoxelShape pShape, Supplier<VoxelShape> pBelowShapeSupplier) {
        if (!pShape.isEmpty()) {
            return pShape.max(Direction.Axis.Y);
        } else {
            double d0 = pBelowShapeSupplier.get().max(Direction.Axis.Y);
            return d0 >= 1.0 ? d0 - 1.0 : Double.NEGATIVE_INFINITY;
        }
    }

    default double getBlockFloorHeight(BlockPos pPos) {
        return this.getBlockFloorHeight(this.getBlockState(pPos).getCollisionShape(this, pPos), () -> {
            BlockPos blockpos = pPos.below();
            return this.getBlockState(blockpos).getCollisionShape(this, blockpos);
        });
    }

    static <T, C> T traverseBlocks(Vec3 pFrom, Vec3 pTo, C pContext, BiFunction<C, BlockPos, T> pTester, Function<C, T> pOnFail) {
        if (pFrom.equals(pTo)) {
            return pOnFail.apply(pContext);
        } else {
            double d0 = Mth.lerp(-1.0E-7, pTo.x, pFrom.x);
            double d1 = Mth.lerp(-1.0E-7, pTo.y, pFrom.y);
            double d2 = Mth.lerp(-1.0E-7, pTo.z, pFrom.z);
            double d3 = Mth.lerp(-1.0E-7, pFrom.x, pTo.x);
            double d4 = Mth.lerp(-1.0E-7, pFrom.y, pTo.y);
            double d5 = Mth.lerp(-1.0E-7, pFrom.z, pTo.z);
            int i = Mth.floor(d3);
            int j = Mth.floor(d4);
            int k = Mth.floor(d5);
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(i, j, k);
            T t = pTester.apply(pContext, blockpos$mutableblockpos);
            if (t != null) {
                return t;
            } else {
                double d6 = d0 - d3;
                double d7 = d1 - d4;
                double d8 = d2 - d5;
                int l = Mth.sign(d6);
                int i1 = Mth.sign(d7);
                int j1 = Mth.sign(d8);
                double d9 = l == 0 ? Double.MAX_VALUE : l / d6;
                double d10 = i1 == 0 ? Double.MAX_VALUE : i1 / d7;
                double d11 = j1 == 0 ? Double.MAX_VALUE : j1 / d8;
                double d12 = d9 * (l > 0 ? 1.0 - Mth.frac(d3) : Mth.frac(d3));
                double d13 = d10 * (i1 > 0 ? 1.0 - Mth.frac(d4) : Mth.frac(d4));
                double d14 = d11 * (j1 > 0 ? 1.0 - Mth.frac(d5) : Mth.frac(d5));

                while (d12 <= 1.0 || d13 <= 1.0 || d14 <= 1.0) {
                    if (d12 < d13) {
                        if (d12 < d14) {
                            i += l;
                            d12 += d9;
                        } else {
                            k += j1;
                            d14 += d11;
                        }
                    } else if (d13 < d14) {
                        j += i1;
                        d13 += d10;
                    } else {
                        k += j1;
                        d14 += d11;
                    }

                    T t1 = pTester.apply(pContext, blockpos$mutableblockpos.set(i, j, k));
                    if (t1 != null) {
                        return t1;
                    }
                }

                return pOnFail.apply(pContext);
            }
        }
    }

    static void forEachBlockIntersectedBetween(Vec3 pFrom, Vec3 pTo, AABB pBoundingBox, BlockGetter.BlockStepVisitor pStepVisitor) {
        Vec3 vec3 = pTo.subtract(pFrom);
        if (!(vec3.lengthSqr() < Mth.square(0.99999F))) {
            LongSet longset = new LongOpenHashSet();
            Vec3 vec32 = pBoundingBox.getMinPosition();
            Vec3 vec31 = vec32.subtract(vec3);
            int i = addCollisionsAlongTravel(longset, vec31, vec32, pBoundingBox, pStepVisitor);

            for (BlockPos blockpos1 : BlockPos.betweenClosed(pBoundingBox)) {
                if (!longset.contains(blockpos1.asLong())) {
                    pStepVisitor.visit(blockpos1, i + 1);
                }
            }
        } else {
            for (BlockPos blockpos : BlockPos.betweenClosed(pBoundingBox)) {
                pStepVisitor.visit(blockpos, 0);
            }
        }
    }

    private static int addCollisionsAlongTravel(LongSet pOutput, Vec3 pFrom, Vec3 pTo, AABB pBoundingBox, BlockGetter.BlockStepVisitor pStepVisitor) {
        Vec3 vec3 = pTo.subtract(pFrom);
        int i = Mth.floor(pFrom.x);
        int j = Mth.floor(pFrom.y);
        int k = Mth.floor(pFrom.z);
        int l = Mth.sign(vec3.x);
        int i1 = Mth.sign(vec3.y);
        int j1 = Mth.sign(vec3.z);
        double d0 = l == 0 ? Double.MAX_VALUE : l / vec3.x;
        double d1 = i1 == 0 ? Double.MAX_VALUE : i1 / vec3.y;
        double d2 = j1 == 0 ? Double.MAX_VALUE : j1 / vec3.z;
        double d3 = d0 * (l > 0 ? 1.0 - Mth.frac(pFrom.x) : Mth.frac(pFrom.x));
        double d4 = d1 * (i1 > 0 ? 1.0 - Mth.frac(pFrom.y) : Mth.frac(pFrom.y));
        double d5 = d2 * (j1 > 0 ? 1.0 - Mth.frac(pFrom.z) : Mth.frac(pFrom.z));
        int k1 = 0;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        while (d3 <= 1.0 || d4 <= 1.0 || d5 <= 1.0) {
            if (d3 < d4) {
                if (d3 < d5) {
                    i += l;
                    d3 += d0;
                } else {
                    k += j1;
                    d5 += d2;
                }
            } else if (d4 < d5) {
                j += i1;
                d4 += d1;
            } else {
                k += j1;
                d5 += d2;
            }

            if (k1++ > 16) {
                break;
            }

            Optional<Vec3> optional = AABB.clip(i, j, k, i + 1, j + 1, k + 1, pFrom, pTo);
            if (!optional.isEmpty()) {
                Vec3 vec31 = optional.get();
                double d6 = Mth.clamp(vec31.x, i + 1.0E-5F, i + 1.0 - 1.0E-5F);
                double d7 = Mth.clamp(vec31.y, j + 1.0E-5F, j + 1.0 - 1.0E-5F);
                double d8 = Mth.clamp(vec31.z, k + 1.0E-5F, k + 1.0 - 1.0E-5F);
                int l1 = Mth.floor(d6 + pBoundingBox.getXsize());
                int i2 = Mth.floor(d7 + pBoundingBox.getYsize());
                int j2 = Mth.floor(d8 + pBoundingBox.getZsize());

                for (int k2 = i; k2 <= l1; k2++) {
                    for (int l2 = j; l2 <= i2; l2++) {
                        for (int i3 = k; i3 <= j2; i3++) {
                            if (pOutput.add(BlockPos.asLong(k2, l2, i3))) {
                                pStepVisitor.visit(blockpos$mutableblockpos.set(k2, l2, i3), k1);
                            }
                        }
                    }
                }
            }
        }

        return k1;
    }

    @FunctionalInterface
    public interface BlockStepVisitor {
        void visit(BlockPos pPos, int pStep);
    }
}
