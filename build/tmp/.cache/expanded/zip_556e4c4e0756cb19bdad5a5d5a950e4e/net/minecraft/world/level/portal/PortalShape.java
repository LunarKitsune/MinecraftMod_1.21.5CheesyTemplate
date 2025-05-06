package net.minecraft.world.level.portal;

import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableInt;

public class PortalShape {
    private static final int MIN_WIDTH = 2;
    public static final int MAX_WIDTH = 21;
    private static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 21;
    private static final BlockBehaviour.StatePredicate FRAME = (p_77720_, p_77721_, p_77722_) -> p_77720_.isPortalFrame(p_77721_, p_77722_);
    private static final float SAFE_TRAVEL_MAX_ENTITY_XY = 4.0F;
    private static final double SAFE_TRAVEL_MAX_VERTICAL_DELTA = 1.0;
    private final Direction.Axis axis;
    private final Direction rightDir;
    private final int numPortalBlocks;
    private final BlockPos bottomLeft;
    private final int height;
    private final int width;

    private PortalShape(Direction.Axis pAxis, int pNumPortalBlocks, Direction pRightDir, BlockPos pBottomLeft, int pWidth, int pHeight) {
        this.axis = pAxis;
        this.numPortalBlocks = pNumPortalBlocks;
        this.rightDir = pRightDir;
        this.bottomLeft = pBottomLeft;
        this.width = pWidth;
        this.height = pHeight;
    }

    public static Optional<PortalShape> findEmptyPortalShape(LevelAccessor pLevel, BlockPos pBottomLeft, Direction.Axis pAxis) {
        return findPortalShape(pLevel, pBottomLeft, p_77727_ -> p_77727_.isValid() && p_77727_.numPortalBlocks == 0, pAxis);
    }

    public static Optional<PortalShape> findPortalShape(LevelAccessor pLevel, BlockPos pBottomLeft, Predicate<PortalShape> pPredicate, Direction.Axis pAxis) {
        Optional<PortalShape> optional = Optional.of(findAnyShape(pLevel, pBottomLeft, pAxis)).filter(pPredicate);
        if (optional.isPresent()) {
            return optional;
        } else {
            Direction.Axis direction$axis = pAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
            return Optional.of(findAnyShape(pLevel, pBottomLeft, direction$axis)).filter(pPredicate);
        }
    }

    public static PortalShape findAnyShape(BlockGetter pLevel, BlockPos pBottomLeft, Direction.Axis pAxis) {
        Direction direction = pAxis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        BlockPos blockpos = calculateBottomLeft(pLevel, direction, pBottomLeft);
        if (blockpos == null) {
            return new PortalShape(pAxis, 0, direction, pBottomLeft, 0, 0);
        } else {
            int i = calculateWidth(pLevel, blockpos, direction);
            if (i == 0) {
                return new PortalShape(pAxis, 0, direction, blockpos, 0, 0);
            } else {
                MutableInt mutableint = new MutableInt();
                int j = calculateHeight(pLevel, blockpos, direction, i, mutableint);
                return new PortalShape(pAxis, mutableint.getValue(), direction, blockpos, i, j);
            }
        }
    }

    @Nullable
    private static BlockPos calculateBottomLeft(BlockGetter pLevel, Direction pDirection, BlockPos pPos) {
        int i = Math.max(pLevel.getMinY(), pPos.getY() - 21);

        while (pPos.getY() > i && isEmpty(pLevel.getBlockState(pPos.below()))) {
            pPos = pPos.below();
        }

        Direction direction = pDirection.getOpposite();
        int j = getDistanceUntilEdgeAboveFrame(pLevel, pPos, direction) - 1;
        return j < 0 ? null : pPos.relative(direction, j);
    }

    private static int calculateWidth(BlockGetter pLevel, BlockPos pBottomLeft, Direction pDirection) {
        int i = getDistanceUntilEdgeAboveFrame(pLevel, pBottomLeft, pDirection);
        return i >= 2 && i <= 21 ? i : 0;
    }

    private static int getDistanceUntilEdgeAboveFrame(BlockGetter pLevel, BlockPos pPos, Direction pDirection) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int i = 0; i <= 21; i++) {
            blockpos$mutableblockpos.set(pPos).move(pDirection, i);
            BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos);
            if (!isEmpty(blockstate)) {
                if (FRAME.test(blockstate, pLevel, blockpos$mutableblockpos)) {
                    return i;
                }
                break;
            }

            BlockState blockstate1 = pLevel.getBlockState(blockpos$mutableblockpos.move(Direction.DOWN));
            if (!FRAME.test(blockstate1, pLevel, blockpos$mutableblockpos)) {
                break;
            }
        }

        return 0;
    }

    private static int calculateHeight(BlockGetter pLevel, BlockPos pPos, Direction pDirection, int pWidth, MutableInt pPortalBlocks) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        int i = getDistanceUntilTop(pLevel, pPos, pDirection, blockpos$mutableblockpos, pWidth, pPortalBlocks);
        return i >= 3 && i <= 21 && hasTopFrame(pLevel, pPos, pDirection, blockpos$mutableblockpos, pWidth, i) ? i : 0;
    }

    private static boolean hasTopFrame(
        BlockGetter pLevel, BlockPos pPos, Direction pDirection, BlockPos.MutableBlockPos pCheckPos, int pWidth, int pDistanceUntilTop
    ) {
        for (int i = 0; i < pWidth; i++) {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = pCheckPos.set(pPos).move(Direction.UP, pDistanceUntilTop).move(pDirection, i);
            if (!FRAME.test(pLevel.getBlockState(blockpos$mutableblockpos), pLevel, blockpos$mutableblockpos)) {
                return false;
            }
        }

        return true;
    }

    private static int getDistanceUntilTop(
        BlockGetter pLevel, BlockPos pPos, Direction pDirection, BlockPos.MutableBlockPos pCheckPos, int pWidth, MutableInt pPortalBlocks
    ) {
        for (int i = 0; i < 21; i++) {
            pCheckPos.set(pPos).move(Direction.UP, i).move(pDirection, -1);
            if (!FRAME.test(pLevel.getBlockState(pCheckPos), pLevel, pCheckPos)) {
                return i;
            }

            pCheckPos.set(pPos).move(Direction.UP, i).move(pDirection, pWidth);
            if (!FRAME.test(pLevel.getBlockState(pCheckPos), pLevel, pCheckPos)) {
                return i;
            }

            for (int j = 0; j < pWidth; j++) {
                pCheckPos.set(pPos).move(Direction.UP, i).move(pDirection, j);
                BlockState blockstate = pLevel.getBlockState(pCheckPos);
                if (!isEmpty(blockstate)) {
                    return i;
                }

                if (blockstate.is(Blocks.NETHER_PORTAL)) {
                    pPortalBlocks.increment();
                }
            }
        }

        return 21;
    }

    private static boolean isEmpty(BlockState pState) {
        return pState.isAir() || pState.is(BlockTags.FIRE) || pState.is(Blocks.NETHER_PORTAL);
    }

    public boolean isValid() {
        return this.width >= 2 && this.width <= 21 && this.height >= 3 && this.height <= 21;
    }

    public void createPortalBlocks(LevelAccessor pLevel) {
        BlockState blockstate = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, this.axis);
        BlockPos.betweenClosed(this.bottomLeft, this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.rightDir, this.width - 1))
            .forEach(p_360642_ -> pLevel.setBlock(p_360642_, blockstate, 18));
    }

    public boolean isComplete() {
        return this.isValid() && this.numPortalBlocks == this.width * this.height;
    }

    public static Vec3 getRelativePosition(BlockUtil.FoundRectangle pFoundRectangle, Direction.Axis pAxis, Vec3 pPos, EntityDimensions pEntityDimensions) {
        double d0 = (double)pFoundRectangle.axis1Size - pEntityDimensions.width();
        double d1 = (double)pFoundRectangle.axis2Size - pEntityDimensions.height();
        BlockPos blockpos = pFoundRectangle.minCorner;
        double d2;
        if (d0 > 0.0) {
            double d3 = blockpos.get(pAxis) + pEntityDimensions.width() / 2.0;
            d2 = Mth.clamp(Mth.inverseLerp(pPos.get(pAxis) - d3, 0.0, d0), 0.0, 1.0);
        } else {
            d2 = 0.5;
        }

        double d5;
        if (d1 > 0.0) {
            Direction.Axis direction$axis = Direction.Axis.Y;
            d5 = Mth.clamp(Mth.inverseLerp(pPos.get(direction$axis) - blockpos.get(direction$axis), 0.0, d1), 0.0, 1.0);
        } else {
            d5 = 0.0;
        }

        Direction.Axis direction$axis1 = pAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        double d4 = pPos.get(direction$axis1) - (blockpos.get(direction$axis1) + 0.5);
        return new Vec3(d2, d5, d4);
    }

    public static Vec3 findCollisionFreePosition(Vec3 pPos, ServerLevel pLevel, Entity pEntity, EntityDimensions pDimensions) {
        if (!(pDimensions.width() > 4.0F) && !(pDimensions.height() > 4.0F)) {
            double d0 = pDimensions.height() / 2.0;
            Vec3 vec3 = pPos.add(0.0, d0, 0.0);
            VoxelShape voxelshape = Shapes.create(
                AABB.ofSize(vec3, pDimensions.width(), 0.0, pDimensions.width()).expandTowards(0.0, 1.0, 0.0).inflate(1.0E-6)
            );
            Optional<Vec3> optional = pLevel.findFreePosition(pEntity, voxelshape, vec3, pDimensions.width(), pDimensions.height(), pDimensions.width());
            Optional<Vec3> optional1 = optional.map(p_259019_ -> p_259019_.subtract(0.0, d0, 0.0));
            return optional1.orElse(pPos);
        } else {
            return pPos;
        }
    }
}
