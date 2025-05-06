package net.minecraft.core;

import com.google.common.collect.AbstractIterator;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.concurrent.Immutable;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

@Immutable
public class BlockPos extends Vec3i {
    public static final Codec<BlockPos> CODEC = Codec.INT_STREAM
        .<BlockPos>comapFlatMap(
            p_325638_ -> Util.fixedSize(p_325638_, 3).map(p_175270_ -> new BlockPos(p_175270_[0], p_175270_[1], p_175270_[2])),
            p_121924_ -> IntStream.of(p_121924_.getX(), p_121924_.getY(), p_121924_.getZ())
        )
        .stable();
    public static final StreamCodec<ByteBuf, BlockPos> STREAM_CODEC = new StreamCodec<ByteBuf, BlockPos>() {
        public BlockPos decode(ByteBuf p_335731_) {
            return FriendlyByteBuf.readBlockPos(p_335731_);
        }

        public void encode(ByteBuf p_329093_, BlockPos p_330029_) {
            FriendlyByteBuf.writeBlockPos(p_329093_, p_330029_);
        }
    };
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final BlockPos ZERO = new BlockPos(0, 0, 0);
    public static final int PACKED_HORIZONTAL_LENGTH = 1 + Mth.log2(Mth.smallestEncompassingPowerOfTwo(30000000));
    public static final int PACKED_Y_LENGTH = 64 - 2 * PACKED_HORIZONTAL_LENGTH;
    private static final long PACKED_X_MASK = (1L << PACKED_HORIZONTAL_LENGTH) - 1L;
    private static final long PACKED_Y_MASK = (1L << PACKED_Y_LENGTH) - 1L;
    private static final long PACKED_Z_MASK = (1L << PACKED_HORIZONTAL_LENGTH) - 1L;
    private static final int Y_OFFSET = 0;
    private static final int Z_OFFSET = PACKED_Y_LENGTH;
    private static final int X_OFFSET = PACKED_Y_LENGTH + PACKED_HORIZONTAL_LENGTH;
    public static final int MAX_HORIZONTAL_COORDINATE = (1 << PACKED_HORIZONTAL_LENGTH) / 2 - 1;

    public BlockPos(int pX, int pY, int pZ) {
        super(pX, pY, pZ);
    }

    public BlockPos(Vec3i pVector) {
        this(pVector.getX(), pVector.getY(), pVector.getZ());
    }

    public static long offset(long pPos, Direction pDirection) {
        return offset(pPos, pDirection.getStepX(), pDirection.getStepY(), pDirection.getStepZ());
    }

    public static long offset(long pPos, int pDx, int pDy, int pDz) {
        return asLong(getX(pPos) + pDx, getY(pPos) + pDy, getZ(pPos) + pDz);
    }

    public static int getX(long pPackedPos) {
        return (int)(pPackedPos << 64 - X_OFFSET - PACKED_HORIZONTAL_LENGTH >> 64 - PACKED_HORIZONTAL_LENGTH);
    }

    public static int getY(long pPackedPos) {
        return (int)(pPackedPos << 64 - PACKED_Y_LENGTH >> 64 - PACKED_Y_LENGTH);
    }

    public static int getZ(long pPackedPos) {
        return (int)(pPackedPos << 64 - Z_OFFSET - PACKED_HORIZONTAL_LENGTH >> 64 - PACKED_HORIZONTAL_LENGTH);
    }

    public static BlockPos of(long pPackedPos) {
        return new BlockPos(getX(pPackedPos), getY(pPackedPos), getZ(pPackedPos));
    }

    public static BlockPos containing(double pX, double pY, double pZ) {
        return new BlockPos(Mth.floor(pX), Mth.floor(pY), Mth.floor(pZ));
    }

    public static BlockPos containing(Position pPosition) {
        return containing(pPosition.x(), pPosition.y(), pPosition.z());
    }

    public static BlockPos min(BlockPos pPos1, BlockPos pPos2) {
        return new BlockPos(
            Math.min(pPos1.getX(), pPos2.getX()),
            Math.min(pPos1.getY(), pPos2.getY()),
            Math.min(pPos1.getZ(), pPos2.getZ())
        );
    }

    public static BlockPos max(BlockPos pPos1, BlockPos pPos2) {
        return new BlockPos(
            Math.max(pPos1.getX(), pPos2.getX()),
            Math.max(pPos1.getY(), pPos2.getY()),
            Math.max(pPos1.getZ(), pPos2.getZ())
        );
    }

    public long asLong() {
        return asLong(this.getX(), this.getY(), this.getZ());
    }

    public static long asLong(int pX, int pY, int pZ) {
        long i = 0L;
        i |= (pX & PACKED_X_MASK) << X_OFFSET;
        i |= (pY & PACKED_Y_MASK) << 0;
        return i | (pZ & PACKED_Z_MASK) << Z_OFFSET;
    }

    public static long getFlatIndex(long pPackedPos) {
        return pPackedPos & -16L;
    }

    public BlockPos offset(int p_121973_, int p_121974_, int p_121975_) {
        return p_121973_ == 0 && p_121974_ == 0 && p_121975_ == 0
            ? this
            : new BlockPos(this.getX() + p_121973_, this.getY() + p_121974_, this.getZ() + p_121975_);
    }

    public Vec3 getCenter() {
        return Vec3.atCenterOf(this);
    }

    public Vec3 getBottomCenter() {
        return Vec3.atBottomCenterOf(this);
    }

    public BlockPos offset(Vec3i p_121956_) {
        return this.offset(p_121956_.getX(), p_121956_.getY(), p_121956_.getZ());
    }

    public BlockPos subtract(Vec3i pVector) {
        return this.offset(-pVector.getX(), -pVector.getY(), -pVector.getZ());
    }

    public BlockPos multiply(int p_175263_) {
        if (p_175263_ == 1) {
            return this;
        } else {
            return p_175263_ == 0 ? ZERO : new BlockPos(this.getX() * p_175263_, this.getY() * p_175263_, this.getZ() * p_175263_);
        }
    }

    public BlockPos above() {
        return this.relative(Direction.UP);
    }

    public BlockPos above(int p_121972_) {
        return this.relative(Direction.UP, p_121972_);
    }

    public BlockPos below() {
        return this.relative(Direction.DOWN);
    }

    public BlockPos below(int p_122000_) {
        return this.relative(Direction.DOWN, p_122000_);
    }

    public BlockPos north() {
        return this.relative(Direction.NORTH);
    }

    public BlockPos north(int p_122014_) {
        return this.relative(Direction.NORTH, p_122014_);
    }

    public BlockPos south() {
        return this.relative(Direction.SOUTH);
    }

    public BlockPos south(int pDistance) {
        return this.relative(Direction.SOUTH, pDistance);
    }

    public BlockPos west() {
        return this.relative(Direction.WEST);
    }

    public BlockPos west(int p_122026_) {
        return this.relative(Direction.WEST, p_122026_);
    }

    public BlockPos east() {
        return this.relative(Direction.EAST);
    }

    public BlockPos east(int p_122031_) {
        return this.relative(Direction.EAST, p_122031_);
    }

    public BlockPos relative(Direction pDirection) {
        return new BlockPos(this.getX() + pDirection.getStepX(), this.getY() + pDirection.getStepY(), this.getZ() + pDirection.getStepZ());
    }

    public BlockPos relative(Direction p_121948_, int p_121949_) {
        return p_121949_ == 0
            ? this
            : new BlockPos(
                this.getX() + p_121948_.getStepX() * p_121949_,
                this.getY() + p_121948_.getStepY() * p_121949_,
                this.getZ() + p_121948_.getStepZ() * p_121949_
            );
    }

    public BlockPos relative(Direction.Axis p_121943_, int p_121944_) {
        if (p_121944_ == 0) {
            return this;
        } else {
            int i = p_121943_ == Direction.Axis.X ? p_121944_ : 0;
            int j = p_121943_ == Direction.Axis.Y ? p_121944_ : 0;
            int k = p_121943_ == Direction.Axis.Z ? p_121944_ : 0;
            return new BlockPos(this.getX() + i, this.getY() + j, this.getZ() + k);
        }
    }

    public BlockPos rotate(Rotation pRotation) {
        switch (pRotation) {
            case NONE:
            default:
                return this;
            case CLOCKWISE_90:
                return new BlockPos(-this.getZ(), this.getY(), this.getX());
            case CLOCKWISE_180:
                return new BlockPos(-this.getX(), this.getY(), -this.getZ());
            case COUNTERCLOCKWISE_90:
                return new BlockPos(this.getZ(), this.getY(), -this.getX());
        }
    }

    public BlockPos cross(Vec3i pVector) {
        return new BlockPos(
            this.getY() * pVector.getZ() - this.getZ() * pVector.getY(),
            this.getZ() * pVector.getX() - this.getX() * pVector.getZ(),
            this.getX() * pVector.getY() - this.getY() * pVector.getX()
        );
    }

    public BlockPos atY(int pY) {
        return new BlockPos(this.getX(), pY, this.getZ());
    }

    public BlockPos immutable() {
        return this;
    }

    public BlockPos.MutableBlockPos mutable() {
        return new BlockPos.MutableBlockPos(this.getX(), this.getY(), this.getZ());
    }

    public Vec3 clampLocationWithin(Vec3 pPos) {
        return new Vec3(
            Mth.clamp(pPos.x, this.getX() + 1.0E-5F, this.getX() + 1.0 - 1.0E-5F),
            Mth.clamp(pPos.y, this.getY() + 1.0E-5F, this.getY() + 1.0 - 1.0E-5F),
            Mth.clamp(pPos.z, this.getZ() + 1.0E-5F, this.getZ() + 1.0 - 1.0E-5F)
        );
    }

    public static Iterable<BlockPos> randomInCube(RandomSource pRandom, int pAmount, BlockPos pCenter, int pRadius) {
        return randomBetweenClosed(
            pRandom,
            pAmount,
            pCenter.getX() - pRadius,
            pCenter.getY() - pRadius,
            pCenter.getZ() - pRadius,
            pCenter.getX() + pRadius,
            pCenter.getY() + pRadius,
            pCenter.getZ() + pRadius
        );
    }

    @Deprecated
    public static Stream<BlockPos> squareOutSouthEast(BlockPos pPos) {
        return Stream.of(pPos, pPos.south(), pPos.east(), pPos.south().east());
    }

    public static Iterable<BlockPos> randomBetweenClosed(
        RandomSource pRandom, int pAmount, int pMinX, int pMinY, int pMinZ, int pMaxX, int pMaxY, int pMaxZ
    ) {
        int i = pMaxX - pMinX + 1;
        int j = pMaxY - pMinY + 1;
        int k = pMaxZ - pMinZ + 1;
        return () -> new AbstractIterator<BlockPos>() {
            final BlockPos.MutableBlockPos nextPos = new BlockPos.MutableBlockPos();
            int counter = pAmount;

            protected BlockPos computeNext() {
                if (this.counter <= 0) {
                    return this.endOfData();
                } else {
                    BlockPos blockpos = this.nextPos
                        .set(pMinX + pRandom.nextInt(i), pMinY + pRandom.nextInt(j), pMinZ + pRandom.nextInt(k));
                    this.counter--;
                    return blockpos;
                }
            }
        };
    }

    public static Iterable<BlockPos> withinManhattan(BlockPos pPos, int pXSize, int pYSize, int pZSize) {
        int i = pXSize + pYSize + pZSize;
        int j = pPos.getX();
        int k = pPos.getY();
        int l = pPos.getZ();
        return () -> new AbstractIterator<BlockPos>() {
            private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            private int currentDepth;
            private int maxX;
            private int maxY;
            private int x;
            private int y;
            private boolean zMirror;

            protected BlockPos computeNext() {
                if (this.zMirror) {
                    this.zMirror = false;
                    this.cursor.setZ(l - (this.cursor.getZ() - l));
                    return this.cursor;
                } else {
                    BlockPos blockpos;
                    for (blockpos = null; blockpos == null; this.y++) {
                        if (this.y > this.maxY) {
                            this.x++;
                            if (this.x > this.maxX) {
                                this.currentDepth++;
                                if (this.currentDepth > i) {
                                    return this.endOfData();
                                }

                                this.maxX = Math.min(pXSize, this.currentDepth);
                                this.x = -this.maxX;
                            }

                            this.maxY = Math.min(pYSize, this.currentDepth - Math.abs(this.x));
                            this.y = -this.maxY;
                        }

                        int i1 = this.x;
                        int j1 = this.y;
                        int k1 = this.currentDepth - Math.abs(i1) - Math.abs(j1);
                        if (k1 <= pZSize) {
                            this.zMirror = k1 != 0;
                            blockpos = this.cursor.set(j + i1, k + j1, l + k1);
                        }
                    }

                    return blockpos;
                }
            }
        };
    }

    public static Optional<BlockPos> findClosestMatch(BlockPos pPos, int pWidth, int pHeight, Predicate<BlockPos> pPosFilter) {
        for (BlockPos blockpos : withinManhattan(pPos, pWidth, pHeight, pWidth)) {
            if (pPosFilter.test(blockpos)) {
                return Optional.of(blockpos);
            }
        }

        return Optional.empty();
    }

    public static Stream<BlockPos> withinManhattanStream(BlockPos pPos, int pXSize, int pYSize, int pZSize) {
        return StreamSupport.stream(withinManhattan(pPos, pXSize, pYSize, pZSize).spliterator(), false);
    }

    public static Iterable<BlockPos> betweenClosed(AABB pBox) {
        BlockPos blockpos = containing(pBox.minX, pBox.minY, pBox.minZ);
        BlockPos blockpos1 = containing(pBox.maxX, pBox.maxY, pBox.maxZ);
        return betweenClosed(blockpos, blockpos1);
    }

    public static Iterable<BlockPos> betweenClosed(BlockPos pFirstPos, BlockPos pSecondPos) {
        return betweenClosed(
            Math.min(pFirstPos.getX(), pSecondPos.getX()),
            Math.min(pFirstPos.getY(), pSecondPos.getY()),
            Math.min(pFirstPos.getZ(), pSecondPos.getZ()),
            Math.max(pFirstPos.getX(), pSecondPos.getX()),
            Math.max(pFirstPos.getY(), pSecondPos.getY()),
            Math.max(pFirstPos.getZ(), pSecondPos.getZ())
        );
    }

    public static Stream<BlockPos> betweenClosedStream(BlockPos pFirstPos, BlockPos pSecondPos) {
        return StreamSupport.stream(betweenClosed(pFirstPos, pSecondPos).spliterator(), false);
    }

    public static Stream<BlockPos> betweenClosedStream(BoundingBox pBox) {
        return betweenClosedStream(
            Math.min(pBox.minX(), pBox.maxX()),
            Math.min(pBox.minY(), pBox.maxY()),
            Math.min(pBox.minZ(), pBox.maxZ()),
            Math.max(pBox.minX(), pBox.maxX()),
            Math.max(pBox.minY(), pBox.maxY()),
            Math.max(pBox.minZ(), pBox.maxZ())
        );
    }

    public static Stream<BlockPos> betweenClosedStream(AABB pAabb) {
        return betweenClosedStream(
            Mth.floor(pAabb.minX),
            Mth.floor(pAabb.minY),
            Mth.floor(pAabb.minZ),
            Mth.floor(pAabb.maxX),
            Mth.floor(pAabb.maxY),
            Mth.floor(pAabb.maxZ)
        );
    }

    public static Stream<BlockPos> betweenClosedStream(int pMinX, int pMinY, int pMinZ, int pMaxX, int pMaxY, int pMaxZ) {
        return StreamSupport.stream(betweenClosed(pMinX, pMinY, pMinZ, pMaxX, pMaxY, pMaxZ).spliterator(), false);
    }

    public static Iterable<BlockPos> betweenClosed(int pX1, int pY1, int pZ1, int pX2, int pY2, int pZ2) {
        int i = pX2 - pX1 + 1;
        int j = pY2 - pY1 + 1;
        int k = pZ2 - pZ1 + 1;
        int l = i * j * k;
        return () -> new AbstractIterator<BlockPos>() {
            private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            private int index;

            protected BlockPos computeNext() {
                if (this.index == l) {
                    return this.endOfData();
                } else {
                    int i1 = this.index % i;
                    int j1 = this.index / i;
                    int k1 = j1 % j;
                    int l1 = j1 / j;
                    this.index++;
                    return this.cursor.set(pX1 + i1, pY1 + k1, pZ1 + l1);
                }
            }
        };
    }

    public static Iterable<BlockPos.MutableBlockPos> spiralAround(BlockPos pCenter, int pSize, Direction pRotationDirection, Direction pExpansionDirection) {
        Validate.validState(pRotationDirection.getAxis() != pExpansionDirection.getAxis(), "The two directions cannot be on the same axis");
        return () -> new AbstractIterator<BlockPos.MutableBlockPos>() {
            private final Direction[] directions = new Direction[]{pRotationDirection, pExpansionDirection, pRotationDirection.getOpposite(), pExpansionDirection.getOpposite()};
            private final BlockPos.MutableBlockPos cursor = pCenter.mutable().move(pExpansionDirection);
            private final int legs = 4 * pSize;
            private int leg = -1;
            private int legSize;
            private int legIndex;
            private int lastX = this.cursor.getX();
            private int lastY = this.cursor.getY();
            private int lastZ = this.cursor.getZ();

            protected BlockPos.MutableBlockPos computeNext() {
                this.cursor.set(this.lastX, this.lastY, this.lastZ).move(this.directions[(this.leg + 4) % 4]);
                this.lastX = this.cursor.getX();
                this.lastY = this.cursor.getY();
                this.lastZ = this.cursor.getZ();
                if (this.legIndex >= this.legSize) {
                    if (this.leg >= this.legs) {
                        return this.endOfData();
                    }

                    this.leg++;
                    this.legIndex = 0;
                    this.legSize = this.leg / 2 + 1;
                }

                this.legIndex++;
                return this.cursor;
            }
        };
    }

    public static int breadthFirstTraversal(
        BlockPos pStartPos,
        int pRadius,
        int pMaxBlocks,
        BiConsumer<BlockPos, Consumer<BlockPos>> pChildrenGetter,
        Function<BlockPos, BlockPos.TraversalNodeStatus> pAction
    ) {
        Queue<Pair<BlockPos, Integer>> queue = new ArrayDeque<>();
        LongSet longset = new LongOpenHashSet();
        queue.add(Pair.of(pStartPos, 0));
        int i = 0;

        while (!queue.isEmpty()) {
            Pair<BlockPos, Integer> pair = queue.poll();
            BlockPos blockpos = pair.getLeft();
            int j = pair.getRight();
            long k = blockpos.asLong();
            if (longset.add(k)) {
                BlockPos.TraversalNodeStatus blockpos$traversalnodestatus = pAction.apply(blockpos);
                if (blockpos$traversalnodestatus != BlockPos.TraversalNodeStatus.SKIP) {
                    if (blockpos$traversalnodestatus == BlockPos.TraversalNodeStatus.STOP) {
                        break;
                    }

                    if (++i >= pMaxBlocks) {
                        return i;
                    }

                    if (j < pRadius) {
                        pChildrenGetter.accept(blockpos, p_277234_ -> queue.add(Pair.of(p_277234_, j + 1)));
                    }
                }
            }
        }

        return i;
    }

    public static class MutableBlockPos extends BlockPos {
        public MutableBlockPos() {
            this(0, 0, 0);
        }

        public MutableBlockPos(int p_122130_, int p_122131_, int p_122132_) {
            super(p_122130_, p_122131_, p_122132_);
        }

        public MutableBlockPos(double pX, double pY, double pZ) {
            this(Mth.floor(pX), Mth.floor(pY), Mth.floor(pZ));
        }

        @Override
        public BlockPos offset(int p_122163_, int p_122164_, int p_122165_) {
            return super.offset(p_122163_, p_122164_, p_122165_).immutable();
        }

        @Override
        public BlockPos multiply(int p_175305_) {
            return super.multiply(p_175305_).immutable();
        }

        @Override
        public BlockPos relative(Direction p_122152_, int p_122153_) {
            return super.relative(p_122152_, p_122153_).immutable();
        }

        @Override
        public BlockPos relative(Direction.Axis p_122145_, int p_122146_) {
            return super.relative(p_122145_, p_122146_).immutable();
        }

        @Override
        public BlockPos rotate(Rotation p_122138_) {
            return super.rotate(p_122138_).immutable();
        }

        public BlockPos.MutableBlockPos set(int pX, int pY, int pZ) {
            this.setX(pX);
            this.setY(pY);
            this.setZ(pZ);
            return this;
        }

        public BlockPos.MutableBlockPos set(double pX, double pY, double pZ) {
            return this.set(Mth.floor(pX), Mth.floor(pY), Mth.floor(pZ));
        }

        public BlockPos.MutableBlockPos set(Vec3i pVector) {
            return this.set(pVector.getX(), pVector.getY(), pVector.getZ());
        }

        public BlockPos.MutableBlockPos set(long pPackedPos) {
            return this.set(getX(pPackedPos), getY(pPackedPos), getZ(pPackedPos));
        }

        public BlockPos.MutableBlockPos set(AxisCycle pCycle, int pX, int pY, int pZ) {
            return this.set(
                pCycle.cycle(pX, pY, pZ, Direction.Axis.X),
                pCycle.cycle(pX, pY, pZ, Direction.Axis.Y),
                pCycle.cycle(pX, pY, pZ, Direction.Axis.Z)
            );
        }

        public BlockPos.MutableBlockPos setWithOffset(Vec3i pPos, Direction pDirection) {
            return this.set(
                pPos.getX() + pDirection.getStepX(), pPos.getY() + pDirection.getStepY(), pPos.getZ() + pDirection.getStepZ()
            );
        }

        public BlockPos.MutableBlockPos setWithOffset(Vec3i pVector, int pOffsetX, int pOffsetY, int pOffsetZ) {
            return this.set(pVector.getX() + pOffsetX, pVector.getY() + pOffsetY, pVector.getZ() + pOffsetZ);
        }

        public BlockPos.MutableBlockPos setWithOffset(Vec3i pPos, Vec3i pOffset) {
            return this.set(
                pPos.getX() + pOffset.getX(), pPos.getY() + pOffset.getY(), pPos.getZ() + pOffset.getZ()
            );
        }

        public BlockPos.MutableBlockPos move(Direction pDirection) {
            return this.move(pDirection, 1);
        }

        public BlockPos.MutableBlockPos move(Direction pDirection, int pN) {
            return this.set(
                this.getX() + pDirection.getStepX() * pN,
                this.getY() + pDirection.getStepY() * pN,
                this.getZ() + pDirection.getStepZ() * pN
            );
        }

        public BlockPos.MutableBlockPos move(int pX, int pY, int pZ) {
            return this.set(this.getX() + pX, this.getY() + pY, this.getZ() + pZ);
        }

        public BlockPos.MutableBlockPos move(Vec3i pOffset) {
            return this.set(this.getX() + pOffset.getX(), this.getY() + pOffset.getY(), this.getZ() + pOffset.getZ());
        }

        public BlockPos.MutableBlockPos clamp(Direction.Axis pAxis, int pMin, int pMax) {
            switch (pAxis) {
                case X:
                    return this.set(Mth.clamp(this.getX(), pMin, pMax), this.getY(), this.getZ());
                case Y:
                    return this.set(this.getX(), Mth.clamp(this.getY(), pMin, pMax), this.getZ());
                case Z:
                    return this.set(this.getX(), this.getY(), Mth.clamp(this.getZ(), pMin, pMax));
                default:
                    throw new IllegalStateException("Unable to clamp axis " + pAxis);
            }
        }

        public BlockPos.MutableBlockPos setX(int p_175341_) {
            super.setX(p_175341_);
            return this;
        }

        public BlockPos.MutableBlockPos setY(int p_175343_) {
            super.setY(p_175343_);
            return this;
        }

        public BlockPos.MutableBlockPos setZ(int p_175345_) {
            super.setZ(p_175345_);
            return this;
        }

        @Override
        public BlockPos immutable() {
            return new BlockPos(this);
        }
    }

    public static enum TraversalNodeStatus {
        ACCEPT,
        SKIP,
        STOP;
    }
}