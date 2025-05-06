package net.minecraft.world.phys.shapes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.math.DoubleMath;
import com.google.common.math.IntMath;
import com.mojang.math.OctahedralGroup;
import com.mojang.math.Quadrant;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import net.minecraft.Util;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Shapes {
    public static final double EPSILON = 1.0E-7;
    public static final double BIG_EPSILON = 1.0E-6;
    private static final VoxelShape BLOCK = Util.make(() -> {
        DiscreteVoxelShape discretevoxelshape = new BitSetDiscreteVoxelShape(1, 1, 1);
        discretevoxelshape.fill(0, 0, 0);
        return new CubeVoxelShape(discretevoxelshape);
    });
    private static final Vec3 BLOCK_CENTER = new Vec3(0.5, 0.5, 0.5);
    public static final VoxelShape INFINITY = box(
        Double.NEGATIVE_INFINITY,
        Double.NEGATIVE_INFINITY,
        Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY,
        Double.POSITIVE_INFINITY,
        Double.POSITIVE_INFINITY
    );
    private static final VoxelShape EMPTY = new ArrayVoxelShape(
        new BitSetDiscreteVoxelShape(0, 0, 0),
        new DoubleArrayList(new double[]{0.0}),
        new DoubleArrayList(new double[]{0.0}),
        new DoubleArrayList(new double[]{0.0})
    );

    public static VoxelShape empty() {
        return EMPTY;
    }

    public static VoxelShape block() {
        return BLOCK;
    }

    public static VoxelShape box(double pMinX, double pMinY, double pMinZ, double pMaxX, double pMaxY, double pMaxZ) {
        if (!(pMinX > pMaxX) && !(pMinY > pMaxY) && !(pMinZ > pMaxZ)) {
            return create(pMinX, pMinY, pMinZ, pMaxX, pMaxY, pMaxZ);
        } else {
            throw new IllegalArgumentException("The min values need to be smaller or equals to the max values");
        }
    }

    public static VoxelShape create(double pMinX, double pMinY, double pMinZ, double pMaxX, double pMaxY, double pMaxZ) {
        if (!(pMaxX - pMinX < 1.0E-7) && !(pMaxY - pMinY < 1.0E-7) && !(pMaxZ - pMinZ < 1.0E-7)) {
            int i = findBits(pMinX, pMaxX);
            int j = findBits(pMinY, pMaxY);
            int k = findBits(pMinZ, pMaxZ);
            if (i < 0 || j < 0 || k < 0) {
                return new ArrayVoxelShape(
                    BLOCK.shape,
                    DoubleArrayList.wrap(new double[]{pMinX, pMaxX}),
                    DoubleArrayList.wrap(new double[]{pMinY, pMaxY}),
                    DoubleArrayList.wrap(new double[]{pMinZ, pMaxZ})
                );
            } else if (i == 0 && j == 0 && k == 0) {
                return block();
            } else {
                int l = 1 << i;
                int i1 = 1 << j;
                int j1 = 1 << k;
                BitSetDiscreteVoxelShape bitsetdiscretevoxelshape = BitSetDiscreteVoxelShape.withFilledBounds(
                    l,
                    i1,
                    j1,
                    (int)Math.round(pMinX * l),
                    (int)Math.round(pMinY * i1),
                    (int)Math.round(pMinZ * j1),
                    (int)Math.round(pMaxX * l),
                    (int)Math.round(pMaxY * i1),
                    (int)Math.round(pMaxZ * j1)
                );
                return new CubeVoxelShape(bitsetdiscretevoxelshape);
            }
        } else {
            return empty();
        }
    }

    public static VoxelShape create(AABB pAabb) {
        return create(pAabb.minX, pAabb.minY, pAabb.minZ, pAabb.maxX, pAabb.maxY, pAabb.maxZ);
    }

    @VisibleForTesting
    protected static int findBits(double pMinBits, double pMaxBits) {
        if (!(pMinBits < -1.0E-7) && !(pMaxBits > 1.0000001)) {
            for (int i = 0; i <= 3; i++) {
                int j = 1 << i;
                double d0 = pMinBits * j;
                double d1 = pMaxBits * j;
                boolean flag = Math.abs(d0 - Math.round(d0)) < 1.0E-7 * j;
                boolean flag1 = Math.abs(d1 - Math.round(d1)) < 1.0E-7 * j;
                if (flag && flag1) {
                    return i;
                }
            }

            return -1;
        } else {
            return -1;
        }
    }

    protected static long lcm(int pAa, int pBb) {
        return (long)pAa * (pBb / IntMath.gcd(pAa, pBb));
    }

    public static VoxelShape or(VoxelShape pShape1, VoxelShape pShape2) {
        return join(pShape1, pShape2, BooleanOp.OR);
    }

    public static VoxelShape or(VoxelShape pShape1, VoxelShape... pOthers) {
        return Arrays.stream(pOthers).reduce(pShape1, Shapes::or);
    }

    public static VoxelShape join(VoxelShape pShape1, VoxelShape pShape2, BooleanOp pFunction) {
        return joinUnoptimized(pShape1, pShape2, pFunction).optimize();
    }

    public static VoxelShape joinUnoptimized(VoxelShape pShape1, VoxelShape pShape2, BooleanOp pFunction) {
        if (pFunction.apply(false, false)) {
            throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException());
        } else if (pShape1 == pShape2) {
            return pFunction.apply(true, true) ? pShape1 : empty();
        } else {
            boolean flag = pFunction.apply(true, false);
            boolean flag1 = pFunction.apply(false, true);
            if (pShape1.isEmpty()) {
                return flag1 ? pShape2 : empty();
            } else if (pShape2.isEmpty()) {
                return flag ? pShape1 : empty();
            } else {
                IndexMerger indexmerger = createIndexMerger(1, pShape1.getCoords(Direction.Axis.X), pShape2.getCoords(Direction.Axis.X), flag, flag1);
                IndexMerger indexmerger1 = createIndexMerger(indexmerger.size() - 1, pShape1.getCoords(Direction.Axis.Y), pShape2.getCoords(Direction.Axis.Y), flag, flag1);
                IndexMerger indexmerger2 = createIndexMerger(
                    (indexmerger.size() - 1) * (indexmerger1.size() - 1), pShape1.getCoords(Direction.Axis.Z), pShape2.getCoords(Direction.Axis.Z), flag, flag1
                );
                BitSetDiscreteVoxelShape bitsetdiscretevoxelshape = BitSetDiscreteVoxelShape.join(
                    pShape1.shape, pShape2.shape, indexmerger, indexmerger1, indexmerger2, pFunction
                );
                return (VoxelShape)(indexmerger instanceof DiscreteCubeMerger
                        && indexmerger1 instanceof DiscreteCubeMerger
                        && indexmerger2 instanceof DiscreteCubeMerger
                    ? new CubeVoxelShape(bitsetdiscretevoxelshape)
                    : new ArrayVoxelShape(bitsetdiscretevoxelshape, indexmerger.getList(), indexmerger1.getList(), indexmerger2.getList()));
            }
        }
    }

    public static boolean joinIsNotEmpty(VoxelShape pShape1, VoxelShape pShape2, BooleanOp pResultOperator) {
        if (pResultOperator.apply(false, false)) {
            throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException());
        } else {
            boolean flag = pShape1.isEmpty();
            boolean flag1 = pShape2.isEmpty();
            if (!flag && !flag1) {
                if (pShape1 == pShape2) {
                    return pResultOperator.apply(true, true);
                } else {
                    boolean flag2 = pResultOperator.apply(true, false);
                    boolean flag3 = pResultOperator.apply(false, true);

                    for (Direction.Axis direction$axis : AxisCycle.AXIS_VALUES) {
                        if (pShape1.max(direction$axis) < pShape2.min(direction$axis) - 1.0E-7) {
                            return flag2 || flag3;
                        }

                        if (pShape2.max(direction$axis) < pShape1.min(direction$axis) - 1.0E-7) {
                            return flag2 || flag3;
                        }
                    }

                    IndexMerger indexmerger = createIndexMerger(1, pShape1.getCoords(Direction.Axis.X), pShape2.getCoords(Direction.Axis.X), flag2, flag3);
                    IndexMerger indexmerger1 = createIndexMerger(
                        indexmerger.size() - 1, pShape1.getCoords(Direction.Axis.Y), pShape2.getCoords(Direction.Axis.Y), flag2, flag3
                    );
                    IndexMerger indexmerger2 = createIndexMerger(
                        (indexmerger.size() - 1) * (indexmerger1.size() - 1),
                        pShape1.getCoords(Direction.Axis.Z),
                        pShape2.getCoords(Direction.Axis.Z),
                        flag2,
                        flag3
                    );
                    return joinIsNotEmpty(indexmerger, indexmerger1, indexmerger2, pShape1.shape, pShape2.shape, pResultOperator);
                }
            } else {
                return pResultOperator.apply(!flag, !flag1);
            }
        }
    }

    private static boolean joinIsNotEmpty(
        IndexMerger pMergerX, IndexMerger pMergerY, IndexMerger pMergerZ, DiscreteVoxelShape pPrimaryShape, DiscreteVoxelShape pSecondaryShape, BooleanOp pResultOperator
    ) {
        return !pMergerX.forMergedIndexes(
            (p_83100_, p_83101_, p_83102_) -> pMergerY.forMergedIndexes(
                (p_166046_, p_166047_, p_166048_) -> pMergerZ.forMergedIndexes(
                    (p_166036_, p_166037_, p_166038_) -> !pResultOperator.apply(
                        pPrimaryShape.isFullWide(p_83100_, p_166046_, p_166036_), pSecondaryShape.isFullWide(p_83101_, p_166047_, p_166037_)
                    )
                )
            )
        );
    }

    public static double collide(Direction.Axis pMovementAxis, AABB pCollisionBox, Iterable<VoxelShape> pPossibleHits, double pDesiredOffset) {
        for (VoxelShape voxelshape : pPossibleHits) {
            if (Math.abs(pDesiredOffset) < 1.0E-7) {
                return 0.0;
            }

            pDesiredOffset = voxelshape.collide(pMovementAxis, pCollisionBox, pDesiredOffset);
        }

        return pDesiredOffset;
    }

    public static boolean blockOccludes(VoxelShape pShape, VoxelShape pAdjacentShape, Direction pSide) {
        if (pShape == block() && pAdjacentShape == block()) {
            return true;
        } else if (pAdjacentShape.isEmpty()) {
            return false;
        } else {
            Direction.Axis direction$axis = pSide.getAxis();
            Direction.AxisDirection direction$axisdirection = pSide.getAxisDirection();
            VoxelShape voxelshape = direction$axisdirection == Direction.AxisDirection.POSITIVE ? pShape : pAdjacentShape;
            VoxelShape voxelshape1 = direction$axisdirection == Direction.AxisDirection.POSITIVE ? pAdjacentShape : pShape;
            BooleanOp booleanop = direction$axisdirection == Direction.AxisDirection.POSITIVE ? BooleanOp.ONLY_FIRST : BooleanOp.ONLY_SECOND;
            return DoubleMath.fuzzyEquals(voxelshape.max(direction$axis), 1.0, 1.0E-7)
                && DoubleMath.fuzzyEquals(voxelshape1.min(direction$axis), 0.0, 1.0E-7)
                && !joinIsNotEmpty(
                    new SliceShape(voxelshape, direction$axis, voxelshape.shape.getSize(direction$axis) - 1),
                    new SliceShape(voxelshape1, direction$axis, 0),
                    booleanop
                );
        }
    }

    public static boolean mergedFaceOccludes(VoxelShape pShape, VoxelShape pAdjacentShape, Direction pSide) {
        if (pShape != block() && pAdjacentShape != block()) {
            Direction.Axis direction$axis = pSide.getAxis();
            Direction.AxisDirection direction$axisdirection = pSide.getAxisDirection();
            VoxelShape voxelshape = direction$axisdirection == Direction.AxisDirection.POSITIVE ? pShape : pAdjacentShape;
            VoxelShape voxelshape1 = direction$axisdirection == Direction.AxisDirection.POSITIVE ? pAdjacentShape : pShape;
            if (!DoubleMath.fuzzyEquals(voxelshape.max(direction$axis), 1.0, 1.0E-7)) {
                voxelshape = empty();
            }

            if (!DoubleMath.fuzzyEquals(voxelshape1.min(direction$axis), 0.0, 1.0E-7)) {
                voxelshape1 = empty();
            }

            return !joinIsNotEmpty(
                block(),
                joinUnoptimized(
                    new SliceShape(voxelshape, direction$axis, voxelshape.shape.getSize(direction$axis) - 1),
                    new SliceShape(voxelshape1, direction$axis, 0),
                    BooleanOp.OR
                ),
                BooleanOp.ONLY_FIRST
            );
        } else {
            return true;
        }
    }

    public static boolean faceShapeOccludes(VoxelShape pVoxelShape1, VoxelShape pVoxelShape2) {
        if (pVoxelShape1 == block() || pVoxelShape2 == block()) {
            return true;
        } else {
            return pVoxelShape1.isEmpty() && pVoxelShape2.isEmpty()
                ? false
                : !joinIsNotEmpty(block(), joinUnoptimized(pVoxelShape1, pVoxelShape2, BooleanOp.OR), BooleanOp.ONLY_FIRST);
        }
    }

    @VisibleForTesting
    protected static IndexMerger createIndexMerger(int pSize, DoubleList pList1, DoubleList pList2, boolean pExcludeUpper, boolean pExcludeLower) {
        int i = pList1.size() - 1;
        int j = pList2.size() - 1;
        if (pList1 instanceof CubePointRange && pList2 instanceof CubePointRange) {
            long k = lcm(i, j);
            if (pSize * k <= 256L) {
                return new DiscreteCubeMerger(i, j);
            }
        }

        if (pList1.getDouble(i) < pList2.getDouble(0) - 1.0E-7) {
            return new NonOverlappingMerger(pList1, pList2, false);
        } else if (pList2.getDouble(j) < pList1.getDouble(0) - 1.0E-7) {
            return new NonOverlappingMerger(pList2, pList1, true);
        } else {
            return (IndexMerger)(i == j && Objects.equals(pList1, pList2)
                ? new IdenticalMerger(pList1)
                : new IndirectMerger(pList1, pList2, pExcludeUpper, pExcludeLower));
        }
    }

    public static VoxelShape rotate(VoxelShape pShape, OctahedralGroup pOctohedralGroup) {
        return rotate(pShape, pOctohedralGroup, BLOCK_CENTER);
    }

    public static VoxelShape rotate(VoxelShape pShape, OctahedralGroup pOctohedralGroup, Vec3 pPos) {
        if (pOctohedralGroup == OctahedralGroup.IDENTITY) {
            return pShape;
        } else {
            DiscreteVoxelShape discretevoxelshape = pShape.shape.rotate(pOctohedralGroup);
            if (pShape instanceof CubeVoxelShape && BLOCK_CENTER.equals(pPos)) {
                return new CubeVoxelShape(discretevoxelshape);
            } else {
                Direction.Axis direction$axis = pOctohedralGroup.permute(Direction.Axis.X);
                Direction.Axis direction$axis1 = pOctohedralGroup.permute(Direction.Axis.Y);
                Direction.Axis direction$axis2 = pOctohedralGroup.permute(Direction.Axis.Z);
                DoubleList doublelist = pShape.getCoords(direction$axis);
                DoubleList doublelist1 = pShape.getCoords(direction$axis1);
                DoubleList doublelist2 = pShape.getCoords(direction$axis2);
                boolean flag = pOctohedralGroup.inverts(direction$axis);
                boolean flag1 = pOctohedralGroup.inverts(direction$axis1);
                boolean flag2 = pOctohedralGroup.inverts(direction$axis2);
                boolean flag3 = direction$axis.choose(flag, flag1, flag2);
                boolean flag4 = direction$axis1.choose(flag, flag1, flag2);
                boolean flag5 = direction$axis2.choose(flag, flag1, flag2);
                return new ArrayVoxelShape(
                    discretevoxelshape,
                    makeAxis(doublelist, flag3, pPos.get(direction$axis), pPos.x),
                    makeAxis(doublelist1, flag4, pPos.get(direction$axis1), pPos.y),
                    makeAxis(doublelist2, flag5, pPos.get(direction$axis2), pPos.z)
                );
            }
        }
    }

    @VisibleForTesting
    static DoubleList makeAxis(DoubleList pInputList, boolean pReverseOrder, double pReferenceValue, double pTargetValue) {
        if (!pReverseOrder && pReferenceValue == pTargetValue) {
            return pInputList;
        } else {
            int i = pInputList.size();
            DoubleList doublelist = new DoubleArrayList(i);
            int j = pReverseOrder ? -1 : 1;

            for (int k = pReverseOrder ? i - 1 : 0; k >= 0 && k < i; k += j) {
                doublelist.add(pTargetValue + j * (pInputList.getDouble(k) - pReferenceValue));
            }

            return doublelist;
        }
    }

    public static boolean equal(VoxelShape pFirst, VoxelShape pSecond) {
        return !joinIsNotEmpty(pFirst, pSecond, BooleanOp.NOT_SAME);
    }

    public static Map<Direction.Axis, VoxelShape> rotateHorizontalAxis(VoxelShape pShape) {
        return rotateHorizontalAxis(pShape, BLOCK_CENTER);
    }

    public static Map<Direction.Axis, VoxelShape> rotateHorizontalAxis(VoxelShape pShape, Vec3 pPos) {
        return Maps.newEnumMap(
            Map.of(Direction.Axis.Z, pShape, Direction.Axis.X, rotate(pShape, OctahedralGroup.fromXYAngles(Quadrant.R0, Quadrant.R90), pPos))
        );
    }

    public static Map<Direction.Axis, VoxelShape> rotateAllAxis(VoxelShape pShape) {
        return rotateAllAxis(pShape, BLOCK_CENTER);
    }

    public static Map<Direction.Axis, VoxelShape> rotateAllAxis(VoxelShape pShape, Vec3 pPos) {
        return Maps.newEnumMap(
            Map.of(
                Direction.Axis.Z,
                pShape,
                Direction.Axis.X,
                rotate(pShape, OctahedralGroup.fromXYAngles(Quadrant.R0, Quadrant.R90), pPos),
                Direction.Axis.Y,
                rotate(pShape, OctahedralGroup.fromXYAngles(Quadrant.R90, Quadrant.R0), pPos)
            )
        );
    }

    public static Map<Direction, VoxelShape> rotateHorizontal(VoxelShape pShape) {
        return rotateHorizontal(pShape, BLOCK_CENTER);
    }

    public static Map<Direction, VoxelShape> rotateHorizontal(VoxelShape pShape, Vec3 pPos) {
        return Maps.newEnumMap(
            Map.of(
                Direction.NORTH,
                pShape,
                Direction.EAST,
                rotate(pShape, OctahedralGroup.fromXYAngles(Quadrant.R0, Quadrant.R90), pPos),
                Direction.SOUTH,
                rotate(pShape, OctahedralGroup.fromXYAngles(Quadrant.R0, Quadrant.R180), pPos),
                Direction.WEST,
                rotate(pShape, OctahedralGroup.fromXYAngles(Quadrant.R0, Quadrant.R270), pPos)
            )
        );
    }

    public static Map<Direction, VoxelShape> rotateAll(VoxelShape pShape) {
        return rotateAll(pShape, BLOCK_CENTER);
    }

    public static Map<Direction, VoxelShape> rotateAll(VoxelShape pShape, Vec3 pPos) {
        return Maps.newEnumMap(
            Map.of(
                Direction.NORTH,
                pShape,
                Direction.EAST,
                rotate(pShape, OctahedralGroup.fromXYAngles(Quadrant.R0, Quadrant.R90), pPos),
                Direction.SOUTH,
                rotate(pShape, OctahedralGroup.fromXYAngles(Quadrant.R0, Quadrant.R180), pPos),
                Direction.WEST,
                rotate(pShape, OctahedralGroup.fromXYAngles(Quadrant.R0, Quadrant.R270), pPos),
                Direction.UP,
                rotate(pShape, OctahedralGroup.fromXYAngles(Quadrant.R270, Quadrant.R0), pPos),
                Direction.DOWN,
                rotate(pShape, OctahedralGroup.fromXYAngles(Quadrant.R90, Quadrant.R0), pPos)
            )
        );
    }

    public static Map<AttachFace, Map<Direction, VoxelShape>> rotateAttachFace(VoxelShape pShape) {
        return Map.of(
            AttachFace.WALL,
            rotateHorizontal(pShape),
            AttachFace.FLOOR,
            rotateHorizontal(rotate(pShape, OctahedralGroup.fromXYAngles(Quadrant.R270, Quadrant.R0))),
            AttachFace.CEILING,
            rotateHorizontal(rotate(pShape, OctahedralGroup.fromXYAngles(Quadrant.R90, Quadrant.R180)))
        );
    }

    public interface DoubleLineConsumer {
        void consume(double pMinX, double pMinY, double pMinZ, double pMaxX, double pMaxY, double pMaxZ);
    }
}