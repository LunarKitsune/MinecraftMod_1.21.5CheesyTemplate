package net.minecraft.world.level.material;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class FlowingFluid extends Fluid {
    public static final BooleanProperty FALLING = BlockStateProperties.FALLING;
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_FLOWING;
    private static final int CACHE_SIZE = 200;
    private static final ThreadLocal<Object2ByteLinkedOpenHashMap<FlowingFluid.BlockStatePairKey>> OCCLUSION_CACHE = ThreadLocal.withInitial(
        () -> {
            Object2ByteLinkedOpenHashMap<FlowingFluid.BlockStatePairKey> object2bytelinkedopenhashmap = new Object2ByteLinkedOpenHashMap<FlowingFluid.BlockStatePairKey>(
                200
            ) {
                @Override
                protected void rehash(int p_76102_) {
                }
            };
            object2bytelinkedopenhashmap.defaultReturnValue((byte)127);
            return object2bytelinkedopenhashmap;
        }
    );
    private final Map<FluidState, VoxelShape> shapes = Maps.newIdentityHashMap();

    @Override
    protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> pBuilder) {
        pBuilder.add(FALLING);
    }

    @Override
    public Vec3 getFlow(BlockGetter pBlockReader, BlockPos pPos, FluidState pFluidState) {
        double d0 = 0.0;
        double d1 = 0.0;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            blockpos$mutableblockpos.setWithOffset(pPos, direction);
            FluidState fluidstate = pBlockReader.getFluidState(blockpos$mutableblockpos);
            if (this.affectsFlow(fluidstate)) {
                float f = fluidstate.getOwnHeight();
                float f1 = 0.0F;
                if (f == 0.0F) {
                    if (!pBlockReader.getBlockState(blockpos$mutableblockpos).blocksMotion()) {
                        BlockPos blockpos = blockpos$mutableblockpos.below();
                        FluidState fluidstate1 = pBlockReader.getFluidState(blockpos);
                        if (this.affectsFlow(fluidstate1)) {
                            f = fluidstate1.getOwnHeight();
                            if (f > 0.0F) {
                                f1 = pFluidState.getOwnHeight() - (f - 0.8888889F);
                            }
                        }
                    }
                } else if (f > 0.0F) {
                    f1 = pFluidState.getOwnHeight() - f;
                }

                if (f1 != 0.0F) {
                    d0 += direction.getStepX() * f1;
                    d1 += direction.getStepZ() * f1;
                }
            }
        }

        Vec3 vec3 = new Vec3(d0, 0.0, d1);
        if (pFluidState.getValue(FALLING)) {
            for (Direction direction1 : Direction.Plane.HORIZONTAL) {
                blockpos$mutableblockpos.setWithOffset(pPos, direction1);
                if (this.isSolidFace(pBlockReader, blockpos$mutableblockpos, direction1) || this.isSolidFace(pBlockReader, blockpos$mutableblockpos.above(), direction1)) {
                    vec3 = vec3.normalize().add(0.0, -6.0, 0.0);
                    break;
                }
            }
        }

        return vec3.normalize();
    }

    private boolean affectsFlow(FluidState pState) {
        return pState.isEmpty() || pState.getType().isSame(this);
    }

    protected boolean isSolidFace(BlockGetter pLevel, BlockPos pNeighborPos, Direction pSide) {
        BlockState blockstate = pLevel.getBlockState(pNeighborPos);
        FluidState fluidstate = pLevel.getFluidState(pNeighborPos);
        if (fluidstate.getType().isSame(this)) {
            return false;
        } else if (pSide == Direction.UP) {
            return true;
        } else {
            return blockstate.getBlock() instanceof IceBlock ? false : blockstate.isFaceSturdy(pLevel, pNeighborPos, pSide);
        }
    }

    protected void spread(ServerLevel pLevel, BlockPos pPos, BlockState pBlockState, FluidState pFluidState) {
        if (!pFluidState.isEmpty()) {
            BlockPos blockpos = pPos.below();
            BlockState blockstate = pLevel.getBlockState(blockpos);
            FluidState fluidstate = blockstate.getFluidState();
            if (this.canMaybePassThrough(pLevel, pPos, pBlockState, Direction.DOWN, blockpos, blockstate, fluidstate)) {
                FluidState fluidstate1 = this.getNewLiquid(pLevel, blockpos, blockstate);
                Fluid fluid = fluidstate1.getType();
                if (fluidstate.canBeReplacedWith(pLevel, blockpos, fluid, Direction.DOWN) && canHoldSpecificFluid(pLevel, blockpos, blockstate, fluid)) {
                    this.spreadTo(pLevel, blockpos, blockstate, Direction.DOWN, fluidstate1);
                    if (this.sourceNeighborCount(pLevel, pPos) >= 3) {
                        this.spreadToSides(pLevel, pPos, pFluidState, pBlockState);
                    }

                    return;
                }
            }

            if (pFluidState.isSource() || !this.isWaterHole(pLevel, pPos, pBlockState, blockpos, blockstate)) {
                this.spreadToSides(pLevel, pPos, pFluidState, pBlockState);
            }
        }
    }

    private void spreadToSides(ServerLevel pLevel, BlockPos pPos, FluidState pFluidState, BlockState pBlockState) {
        int i = pFluidState.getAmount() - this.getDropOff(pLevel);
        if (pFluidState.getValue(FALLING)) {
            i = 7;
        }

        if (i > 0) {
            Map<Direction, FluidState> map = this.getSpread(pLevel, pPos, pBlockState);

            for (Entry<Direction, FluidState> entry : map.entrySet()) {
                Direction direction = entry.getKey();
                FluidState fluidstate = entry.getValue();
                BlockPos blockpos = pPos.relative(direction);
                this.spreadTo(pLevel, blockpos, pLevel.getBlockState(blockpos), direction, fluidstate);
            }
        }
    }

    protected FluidState getNewLiquid(ServerLevel pLevel, BlockPos pPos, BlockState pState) {
        int i = 0;
        int j = 0;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockpos = blockpos$mutableblockpos.setWithOffset(pPos, direction);
            BlockState blockstate = pLevel.getBlockState(blockpos);
            FluidState fluidstate = blockstate.getFluidState();
            if (fluidstate.getType().isSame(this) && canPassThroughWall(direction, pLevel, pPos, pState, blockpos, blockstate)) {
                if (fluidstate.isSource() && net.minecraftforge.event.ForgeEventFactory.canCreateFluidSource(pLevel, blockpos, blockstate, fluidstate.canConvertToSource(pLevel, blockpos))) {
                    j++;
                }

                i = Math.max(i, fluidstate.getAmount());
            }
        }

        if (j >= 2) {
            BlockState blockstate1 = pLevel.getBlockState(blockpos$mutableblockpos.setWithOffset(pPos, Direction.DOWN));
            FluidState fluidstate1 = blockstate1.getFluidState();
            if (blockstate1.isSolid() || this.isSourceBlockOfThisType(fluidstate1)) {
                return this.getSource(false);
            }
        }

        BlockPos blockpos1 = blockpos$mutableblockpos.setWithOffset(pPos, Direction.UP);
        BlockState blockstate2 = pLevel.getBlockState(blockpos1);
        FluidState fluidstate2 = blockstate2.getFluidState();
        if (!fluidstate2.isEmpty() && fluidstate2.getType().isSame(this) && canPassThroughWall(Direction.UP, pLevel, pPos, pState, blockpos1, blockstate2)) {
            return this.getFlowing(8, true);
        } else {
            int k = i - this.getDropOff(pLevel);
            return k <= 0 ? Fluids.EMPTY.defaultFluidState() : this.getFlowing(k, false);
        }
    }

    private static boolean canPassThroughWall(Direction pDirection, BlockGetter pLevel, BlockPos pPos, BlockState pState, BlockPos pSpreadPos, BlockState pSpreadState) {
        VoxelShape voxelshape = pSpreadState.getCollisionShape(pLevel, pSpreadPos);
        if (voxelshape == Shapes.block()) {
            return false;
        } else {
            VoxelShape voxelshape1 = pState.getCollisionShape(pLevel, pPos);
            if (voxelshape1 == Shapes.block()) {
                return false;
            } else if (voxelshape1 == Shapes.empty() && voxelshape == Shapes.empty()) {
                return true;
            } else {
                Object2ByteLinkedOpenHashMap<FlowingFluid.BlockStatePairKey> object2bytelinkedopenhashmap;
                if (!pState.getBlock().hasDynamicShape() && !pSpreadState.getBlock().hasDynamicShape()) {
                    object2bytelinkedopenhashmap = OCCLUSION_CACHE.get();
                } else {
                    object2bytelinkedopenhashmap = null;
                }

                FlowingFluid.BlockStatePairKey flowingfluid$blockstatepairkey;
                if (object2bytelinkedopenhashmap != null) {
                    flowingfluid$blockstatepairkey = new FlowingFluid.BlockStatePairKey(pState, pSpreadState, pDirection);
                    byte b0 = object2bytelinkedopenhashmap.getAndMoveToFirst(flowingfluid$blockstatepairkey);
                    if (b0 != 127) {
                        return b0 != 0;
                    }
                } else {
                    flowingfluid$blockstatepairkey = null;
                }

                boolean flag = !Shapes.mergedFaceOccludes(voxelshape1, voxelshape, pDirection);
                if (object2bytelinkedopenhashmap != null) {
                    if (object2bytelinkedopenhashmap.size() == 200) {
                        object2bytelinkedopenhashmap.removeLastByte();
                    }

                    object2bytelinkedopenhashmap.putAndMoveToFirst(flowingfluid$blockstatepairkey, (byte)(flag ? 1 : 0));
                }

                return flag;
            }
        }
    }

    public abstract Fluid getFlowing();

    public FluidState getFlowing(int pLevel, boolean pFalling) {
        return this.getFlowing().defaultFluidState().setValue(LEVEL, pLevel).setValue(FALLING, pFalling);
    }

    public abstract Fluid getSource();

    public FluidState getSource(boolean pFalling) {
        return this.getSource().defaultFluidState().setValue(FALLING, pFalling);
    }

    @Override
    public boolean canConvertToSource(FluidState state, ServerLevel level, BlockPos pos) {
       return this.canConvertToSource(level);
    }

    /**
     * @deprecated Forge: Use {@link #canConvertToSource(FluidState, Level, BlockPos)} instead.
     */
    @Deprecated
    protected abstract boolean canConvertToSource(ServerLevel pLevel);

    protected void spreadTo(LevelAccessor pLevel, BlockPos pPos, BlockState pBlockState, Direction pDirection, FluidState pFluidState) {
        if (pBlockState.getBlock() instanceof LiquidBlockContainer liquidblockcontainer) {
            liquidblockcontainer.placeLiquid(pLevel, pPos, pBlockState, pFluidState);
        } else {
            if (!pBlockState.isAir()) {
                this.beforeDestroyingBlock(pLevel, pPos, pBlockState);
            }

            pLevel.setBlock(pPos, pFluidState.createLegacyBlock(), 3);
        }
    }

    protected abstract void beforeDestroyingBlock(LevelAccessor pLevel, BlockPos pPos, BlockState pState);

    protected int getSlopeDistance(LevelReader pLevel, BlockPos pPos, int pDepth, Direction pDirection, BlockState pState, FlowingFluid.SpreadContext pSpreadContext) {
        int i = 1000;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction != pDirection) {
                BlockPos blockpos = pPos.relative(direction);
                BlockState blockstate = pSpreadContext.getBlockState(blockpos);
                FluidState fluidstate = blockstate.getFluidState();
                if (this.canPassThrough(pLevel, this.getFlowing(), pPos, pState, direction, blockpos, blockstate, fluidstate)) {
                    if (pSpreadContext.isHole(blockpos)) {
                        return pDepth;
                    }

                    if (pDepth < this.getSlopeFindDistance(pLevel)) {
                        int j = this.getSlopeDistance(pLevel, blockpos, pDepth + 1, direction.getOpposite(), blockstate, pSpreadContext);
                        if (j < i) {
                            i = j;
                        }
                    }
                }
            }
        }

        return i;
    }

    boolean isWaterHole(BlockGetter pLevel, BlockPos pPos, BlockState pState, BlockPos pBelowPos, BlockState pBelowState) {
        if (!canPassThroughWall(Direction.DOWN, pLevel, pPos, pState, pBelowPos, pBelowState)) {
            return false;
        } else {
            return pBelowState.getFluidState().getType().isSame(this) ? true : canHoldFluid(pLevel, pBelowPos, pBelowState, this.getFlowing());
        }
    }

    private boolean canPassThrough(
        BlockGetter pLevel,
        Fluid pFluid,
        BlockPos pPos,
        BlockState pState,
        Direction pDirection,
        BlockPos pSpreadPos,
        BlockState pSpreadState,
        FluidState pFluidState
    ) {
        return this.canMaybePassThrough(pLevel, pPos, pState, pDirection, pSpreadPos, pSpreadState, pFluidState) && canHoldSpecificFluid(pLevel, pSpreadPos, pSpreadState, pFluid);
    }

    private boolean canMaybePassThrough(
        BlockGetter pLevel, BlockPos pPos, BlockState pState, Direction pDirection, BlockPos pSpreadPos, BlockState pSpreadState, FluidState pFluidState
    ) {
        return !this.isSourceBlockOfThisType(pFluidState) && canHoldAnyFluid(pSpreadState) && canPassThroughWall(pDirection, pLevel, pPos, pState, pSpreadPos, pSpreadState);
    }

    private boolean isSourceBlockOfThisType(FluidState pState) {
        return pState.getType().isSame(this) && pState.isSource();
    }

    protected abstract int getSlopeFindDistance(LevelReader pLevel);

    private int sourceNeighborCount(LevelReader pLevel, BlockPos pPos) {
        int i = 0;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockpos = pPos.relative(direction);
            FluidState fluidstate = pLevel.getFluidState(blockpos);
            if (this.isSourceBlockOfThisType(fluidstate)) {
                i++;
            }
        }

        return i;
    }

    protected Map<Direction, FluidState> getSpread(ServerLevel pLevel, BlockPos pPos, BlockState pState) {
        int i = 1000;
        Map<Direction, FluidState> map = Maps.newEnumMap(Direction.class);
        FlowingFluid.SpreadContext flowingfluid$spreadcontext = null;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockpos = pPos.relative(direction);
            BlockState blockstate = pLevel.getBlockState(blockpos);
            FluidState fluidstate = blockstate.getFluidState();
            if (this.canMaybePassThrough(pLevel, pPos, pState, direction, blockpos, blockstate, fluidstate)) {
                FluidState fluidstate1 = this.getNewLiquid(pLevel, blockpos, blockstate);
                if (canHoldSpecificFluid(pLevel, blockpos, blockstate, fluidstate1.getType())) {
                    if (flowingfluid$spreadcontext == null) {
                        flowingfluid$spreadcontext = new FlowingFluid.SpreadContext(pLevel, pPos);
                    }

                    int j;
                    if (flowingfluid$spreadcontext.isHole(blockpos)) {
                        j = 0;
                    } else {
                        j = this.getSlopeDistance(pLevel, blockpos, 1, direction.getOpposite(), blockstate, flowingfluid$spreadcontext);
                    }

                    if (j < i) {
                        map.clear();
                    }

                    if (j <= i) {
                        if (fluidstate.canBeReplacedWith(pLevel, blockpos, fluidstate1.getType(), direction)) {
                            map.put(direction, fluidstate1);
                        }

                        i = j;
                    }
                }
            }
        }

        return map;
    }

    private static boolean canHoldAnyFluid(BlockState pState) {
        Block block = pState.getBlock();
        if (block instanceof LiquidBlockContainer) {
            return true;
        } else {
            return pState.blocksMotion()
                ? false
                : !(block instanceof DoorBlock)
                    && !pState.is(BlockTags.SIGNS)
                    && !pState.is(Blocks.LADDER)
                    && !pState.is(Blocks.SUGAR_CANE)
                    && !pState.is(Blocks.BUBBLE_COLUMN)
                    && !pState.is(Blocks.NETHER_PORTAL)
                    && !pState.is(Blocks.END_PORTAL)
                    && !pState.is(Blocks.END_GATEWAY)
                    && !pState.is(Blocks.STRUCTURE_VOID);
        }
    }

    private static boolean canHoldFluid(BlockGetter pLevel, BlockPos pPos, BlockState pState, Fluid pFluid) {
        return canHoldAnyFluid(pState) && canHoldSpecificFluid(pLevel, pPos, pState, pFluid);
    }

    private static boolean canHoldSpecificFluid(BlockGetter pLevel, BlockPos pPos, BlockState pState, Fluid pFluid) {
        return pState.getBlock() instanceof LiquidBlockContainer liquidblockcontainer
            ? liquidblockcontainer.canPlaceLiquid(null, pLevel, pPos, pState, pFluid)
            : true;
    }

    protected abstract int getDropOff(LevelReader pLevel);

    protected int getSpreadDelay(Level pLevel, BlockPos pPos, FluidState pCurrentState, FluidState pNewState) {
        return this.getTickDelay(pLevel);
    }

    @Override
    public void tick(ServerLevel p_362527_, BlockPos p_75996_, BlockState p_369266_, FluidState p_75997_) {
        if (!p_75997_.isSource()) {
            FluidState fluidstate = this.getNewLiquid(p_362527_, p_75996_, p_362527_.getBlockState(p_75996_));
            int i = this.getSpreadDelay(p_362527_, p_75996_, p_75997_, fluidstate);
            if (fluidstate.isEmpty()) {
                p_75997_ = fluidstate;
                p_369266_ = Blocks.AIR.defaultBlockState();
                p_362527_.setBlock(p_75996_, p_369266_, 3);
            } else if (fluidstate != p_75997_) {
                p_75997_ = fluidstate;
                p_369266_ = fluidstate.createLegacyBlock();
                p_362527_.setBlock(p_75996_, p_369266_, 3);
                p_362527_.scheduleTick(p_75996_, fluidstate.getType(), i);
            }
        }

        this.spread(p_362527_, p_75996_, p_369266_, p_75997_);
    }

    protected static int getLegacyLevel(FluidState pState) {
        return pState.isSource() ? 0 : 8 - Math.min(pState.getAmount(), 8) + (pState.getValue(FALLING) ? 8 : 0);
    }

    private static boolean hasSameAbove(FluidState pFluidState, BlockGetter pLevel, BlockPos pPos) {
        return pFluidState.getType().isSame(pLevel.getFluidState(pPos.above()).getType());
    }

    @Override
    public float getHeight(FluidState p_76050_, BlockGetter p_76051_, BlockPos p_76052_) {
        return hasSameAbove(p_76050_, p_76051_, p_76052_) ? 1.0F : p_76050_.getOwnHeight();
    }

    @Override
    public float getOwnHeight(FluidState p_76048_) {
        return p_76048_.getAmount() / 9.0F;
    }

    @Override
    public abstract int getAmount(FluidState p_164509_);

    @Override
    public VoxelShape getShape(FluidState p_76084_, BlockGetter p_76085_, BlockPos p_76086_) {
        return p_76084_.getAmount() == 9 && hasSameAbove(p_76084_, p_76085_, p_76086_)
            ? Shapes.block()
            : this.shapes.computeIfAbsent(p_76084_, p_76073_ -> Shapes.box(0.0, 0.0, 0.0, 1.0, p_76073_.getHeight(p_76085_, p_76086_), 1.0));
    }

    record BlockStatePairKey(BlockState first, BlockState second, Direction direction) {
        @Override
        public boolean equals(Object p_364864_) {
            return p_364864_ instanceof FlowingFluid.BlockStatePairKey flowingfluid$blockstatepairkey
                && this.first == flowingfluid$blockstatepairkey.first
                && this.second == flowingfluid$blockstatepairkey.second
                && this.direction == flowingfluid$blockstatepairkey.direction;
        }

        @Override
        public int hashCode() {
            int i = System.identityHashCode(this.first);
            i = 31 * i + System.identityHashCode(this.second);
            return 31 * i + this.direction.hashCode();
        }
    }

    protected class SpreadContext {
        private final BlockGetter level;
        private final BlockPos origin;
        private final Short2ObjectMap<BlockState> stateCache = new Short2ObjectOpenHashMap<>();
        private final Short2BooleanMap holeCache = new Short2BooleanOpenHashMap();

        SpreadContext(final BlockGetter pLevel, final BlockPos pOrigin) {
            this.level = pLevel;
            this.origin = pOrigin;
        }

        public BlockState getBlockState(BlockPos pPos) {
            return this.getBlockState(pPos, this.getCacheKey(pPos));
        }

        private BlockState getBlockState(BlockPos pPos, short pCacheKey) {
            return this.stateCache.computeIfAbsent(pCacheKey, p_365254_ -> this.level.getBlockState(pPos));
        }

        public boolean isHole(BlockPos pPos) {
            return this.holeCache.computeIfAbsent(this.getCacheKey(pPos), p_365811_ -> {
                BlockState blockstate = this.getBlockState(pPos, p_365811_);
                BlockPos blockpos = pPos.below();
                BlockState blockstate1 = this.level.getBlockState(blockpos);
                return FlowingFluid.this.isWaterHole(this.level, pPos, blockstate, blockpos, blockstate1);
            });
        }

        private short getCacheKey(BlockPos pPos) {
            int i = pPos.getX() - this.origin.getX();
            int j = pPos.getZ() - this.origin.getZ();
            return (short)((i + 128 & 0xFF) << 8 | j + 128 & 0xFF);
        }
    }
}
