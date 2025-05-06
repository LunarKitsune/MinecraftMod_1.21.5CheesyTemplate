package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.redstone.DefaultRedstoneWireEvaluator;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.ExperimentalRedstoneWireEvaluator;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.redstone.RedstoneWireEvaluator;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RedStoneWireBlock extends Block {
    public static final MapCodec<RedStoneWireBlock> CODEC = simpleCodec(RedStoneWireBlock::new);
    public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
    public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final Map<Direction, EnumProperty<RedstoneSide>> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(
        Maps.newEnumMap(Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST))
    );
    private static final int[] COLORS = Util.make(new int[16], p_360448_ -> {
        for (int i = 0; i <= 15; i++) {
            float f = i / 15.0F;
            float f1 = f * 0.6F + (f > 0.0F ? 0.4F : 0.3F);
            float f2 = Mth.clamp(f * f * 0.7F - 0.5F, 0.0F, 1.0F);
            float f3 = Mth.clamp(f * f * 0.6F - 0.7F, 0.0F, 1.0F);
            p_360448_[i] = ARGB.colorFromFloat(1.0F, f1, f2, f3);
        }
    });
    private static final float PARTICLE_DENSITY = 0.2F;
    private final Function<BlockState, VoxelShape> shapes;
    private final BlockState crossState;
    private final RedstoneWireEvaluator evaluator = new DefaultRedstoneWireEvaluator(this);
    private boolean shouldSignal = true;

    @Override
    public MapCodec<RedStoneWireBlock> codec() {
        return CODEC;
    }

    public RedStoneWireBlock(BlockBehaviour.Properties p_55511_) {
        super(p_55511_);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(NORTH, RedstoneSide.NONE)
                .setValue(EAST, RedstoneSide.NONE)
                .setValue(SOUTH, RedstoneSide.NONE)
                .setValue(WEST, RedstoneSide.NONE)
                .setValue(POWER, 0)
        );
        this.shapes = this.makeShapes();
        this.crossState = this.defaultBlockState()
            .setValue(NORTH, RedstoneSide.SIDE)
            .setValue(EAST, RedstoneSide.SIDE)
            .setValue(SOUTH, RedstoneSide.SIDE)
            .setValue(WEST, RedstoneSide.SIDE);
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        int i = 1;
        int j = 10;
        VoxelShape voxelshape = Block.column(10.0, 0.0, 1.0);
        Map<Direction, VoxelShape> map = Shapes.rotateHorizontal(Block.boxZ(10.0, 0.0, 1.0, 0.0, 8.0));
        Map<Direction, VoxelShape> map1 = Shapes.rotateHorizontal(Block.boxZ(10.0, 16.0, 0.0, 1.0));
        return this.getShapeForEachState(p_390953_ -> {
            VoxelShape voxelshape1 = voxelshape;

            for (Entry<Direction, EnumProperty<RedstoneSide>> entry : PROPERTY_BY_DIRECTION.entrySet()) {
                voxelshape1 = switch ((RedstoneSide)p_390953_.getValue(entry.getValue())) {
                    case UP -> Shapes.or(voxelshape1, map.get(entry.getKey()), map1.get(entry.getKey()));
                    case SIDE -> Shapes.or(voxelshape1, map.get(entry.getKey()));
                    case NONE -> voxelshape1;
                };
            }

            return voxelshape1;
        }, new Property[]{POWER});
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return this.shapes.apply(pState);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.getConnectionState(pContext.getLevel(), this.crossState, pContext.getClickedPos());
    }

    private BlockState getConnectionState(BlockGetter pLevel, BlockState pState, BlockPos pPos) {
        boolean flag = isDot(pState);
        pState = this.getMissingConnections(pLevel, this.defaultBlockState().setValue(POWER, pState.getValue(POWER)), pPos);
        if (flag && isDot(pState)) {
            return pState;
        } else {
            boolean flag1 = pState.getValue(NORTH).isConnected();
            boolean flag2 = pState.getValue(SOUTH).isConnected();
            boolean flag3 = pState.getValue(EAST).isConnected();
            boolean flag4 = pState.getValue(WEST).isConnected();
            boolean flag5 = !flag1 && !flag2;
            boolean flag6 = !flag3 && !flag4;
            if (!flag4 && flag5) {
                pState = pState.setValue(WEST, RedstoneSide.SIDE);
            }

            if (!flag3 && flag5) {
                pState = pState.setValue(EAST, RedstoneSide.SIDE);
            }

            if (!flag1 && flag6) {
                pState = pState.setValue(NORTH, RedstoneSide.SIDE);
            }

            if (!flag2 && flag6) {
                pState = pState.setValue(SOUTH, RedstoneSide.SIDE);
            }

            return pState;
        }
    }

    private BlockState getMissingConnections(BlockGetter pLevel, BlockState pState, BlockPos pPos) {
        boolean flag = !pLevel.getBlockState(pPos.above()).isRedstoneConductor(pLevel, pPos);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!pState.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected()) {
                RedstoneSide redstoneside = this.getConnectingSide(pLevel, pPos, direction, flag);
                pState = pState.setValue(PROPERTY_BY_DIRECTION.get(direction), redstoneside);
            }
        }

        return pState;
    }

    @Override
    protected BlockState updateShape(
        BlockState p_55598_,
        LevelReader p_364425_,
        ScheduledTickAccess p_364740_,
        BlockPos p_55602_,
        Direction p_55599_,
        BlockPos p_55603_,
        BlockState p_55600_,
        RandomSource p_363398_
    ) {
        if (p_55599_ == Direction.DOWN) {
            return !this.canSurviveOn(p_364425_, p_55603_, p_55600_) ? Blocks.AIR.defaultBlockState() : p_55598_;
        } else if (p_55599_ == Direction.UP) {
            return this.getConnectionState(p_364425_, p_55598_, p_55602_);
        } else {
            RedstoneSide redstoneside = this.getConnectingSide(p_364425_, p_55602_, p_55599_);
            return redstoneside.isConnected() == p_55598_.getValue(PROPERTY_BY_DIRECTION.get(p_55599_)).isConnected() && !isCross(p_55598_)
                ? p_55598_.setValue(PROPERTY_BY_DIRECTION.get(p_55599_), redstoneside)
                : this.getConnectionState(
                    p_364425_, this.crossState.setValue(POWER, p_55598_.getValue(POWER)).setValue(PROPERTY_BY_DIRECTION.get(p_55599_), redstoneside), p_55602_
                );
        }
    }

    private static boolean isCross(BlockState pState) {
        return pState.getValue(NORTH).isConnected()
            && pState.getValue(SOUTH).isConnected()
            && pState.getValue(EAST).isConnected()
            && pState.getValue(WEST).isConnected();
    }

    private static boolean isDot(BlockState pState) {
        return !pState.getValue(NORTH).isConnected()
            && !pState.getValue(SOUTH).isConnected()
            && !pState.getValue(EAST).isConnected()
            && !pState.getValue(WEST).isConnected();
    }

    @Override
    protected void updateIndirectNeighbourShapes(BlockState pState, LevelAccessor pLevel, BlockPos pPos, int pFlags, int pRecursionLeft) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            RedstoneSide redstoneside = pState.getValue(PROPERTY_BY_DIRECTION.get(direction));
            if (redstoneside != RedstoneSide.NONE && !pLevel.getBlockState(blockpos$mutableblockpos.setWithOffset(pPos, direction)).is(this)) {
                blockpos$mutableblockpos.move(Direction.DOWN);
                BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos);
                if (blockstate.is(this)) {
                    BlockPos blockpos = blockpos$mutableblockpos.relative(direction.getOpposite());
                    pLevel.neighborShapeChanged(direction.getOpposite(), blockpos$mutableblockpos, blockpos, pLevel.getBlockState(blockpos), pFlags, pRecursionLeft);
                }

                blockpos$mutableblockpos.setWithOffset(pPos, direction).move(Direction.UP);
                BlockState blockstate1 = pLevel.getBlockState(blockpos$mutableblockpos);
                if (blockstate1.is(this)) {
                    BlockPos blockpos1 = blockpos$mutableblockpos.relative(direction.getOpposite());
                    pLevel.neighborShapeChanged(direction.getOpposite(), blockpos$mutableblockpos, blockpos1, pLevel.getBlockState(blockpos1), pFlags, pRecursionLeft);
                }
            }
        }
    }

    private RedstoneSide getConnectingSide(BlockGetter pLevel, BlockPos pPos, Direction pFace) {
        return this.getConnectingSide(pLevel, pPos, pFace, !pLevel.getBlockState(pPos.above()).isRedstoneConductor(pLevel, pPos));
    }

    private RedstoneSide getConnectingSide(BlockGetter pLevel, BlockPos pPos, Direction pDirection, boolean pNonNormalCubeAbove) {
        BlockPos blockpos = pPos.relative(pDirection);
        BlockState blockstate = pLevel.getBlockState(blockpos);
        if (pNonNormalCubeAbove) {
            boolean flag = blockstate.getBlock() instanceof TrapDoorBlock || this.canSurviveOn(pLevel, blockpos, blockstate);
            if (flag && pLevel.getBlockState(blockpos.above()).canRedstoneConnectTo(pLevel, blockpos.above(), null)) {
                if (blockstate.isFaceSturdy(pLevel, blockpos, pDirection.getOpposite())) {
                    return RedstoneSide.UP;
                }

                return RedstoneSide.SIDE;
            }
        }

        if (blockstate.canRedstoneConnectTo(pLevel, blockpos, pDirection)) {
            return RedstoneSide.SIDE;
        } else if (blockstate.isRedstoneConductor(pLevel, blockpos)) {
            return RedstoneSide.NONE;
        } else {
            BlockPos blockPosBelow = blockpos.below();
            return pLevel.getBlockState(blockPosBelow).canRedstoneConnectTo(pLevel, blockPosBelow, null) ? RedstoneSide.SIDE : RedstoneSide.NONE;
        }
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.below();
        BlockState blockstate = pLevel.getBlockState(blockpos);
        return this.canSurviveOn(pLevel, blockpos, blockstate);
    }

    private boolean canSurviveOn(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
        return pState.isFaceSturdy(pLevel, pPos, Direction.UP) || pState.is(Blocks.HOPPER);
    }

    private void updatePowerStrength(Level pLevel, BlockPos pPos, BlockState pState, @Nullable Orientation pOrientation, boolean pUpdateShape) {
        if (useExperimentalEvaluator(pLevel)) {
            new ExperimentalRedstoneWireEvaluator(this).updatePowerStrength(pLevel, pPos, pState, pOrientation, pUpdateShape);
        } else {
            this.evaluator.updatePowerStrength(pLevel, pPos, pState, pOrientation, pUpdateShape);
        }
    }

    public int getBlockSignal(Level pLevel, BlockPos pPos) {
        this.shouldSignal = false;
        int i = pLevel.getBestNeighborSignal(pPos);
        this.shouldSignal = true;
        return i;
    }

    private void checkCornerChangeAt(Level pLevel, BlockPos pPos) {
        if (pLevel.getBlockState(pPos).is(this)) {
            pLevel.updateNeighborsAt(pPos, this);

            for (Direction direction : Direction.values()) {
                pLevel.updateNeighborsAt(pPos.relative(direction), this);
            }
        }
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (!pOldState.is(pState.getBlock()) && !pLevel.isClientSide) {
            this.updatePowerStrength(pLevel, pPos, pState, null, true);

            for (Direction direction : Direction.Plane.VERTICAL) {
                pLevel.updateNeighborsAt(pPos.relative(direction), this);
            }

            this.updateNeighborsOfNeighboringWires(pLevel, pPos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState p_396119_, ServerLevel p_392722_, BlockPos p_393494_, boolean p_392611_) {
        if (!p_392611_) {
            for (Direction direction : Direction.values()) {
                p_392722_.updateNeighborsAt(p_393494_.relative(direction), this);
            }

            this.updatePowerStrength(p_392722_, p_393494_, p_396119_, null, false);
            this.updateNeighborsOfNeighboringWires(p_392722_, p_393494_);
        }
    }

    private void updateNeighborsOfNeighboringWires(Level pLevel, BlockPos pPos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            this.checkCornerChangeAt(pLevel, pPos.relative(direction));
        }

        for (Direction direction1 : Direction.Plane.HORIZONTAL) {
            BlockPos blockpos = pPos.relative(direction1);
            if (pLevel.getBlockState(blockpos).isRedstoneConductor(pLevel, blockpos)) {
                this.checkCornerChangeAt(pLevel, blockpos.above());
            } else {
                this.checkCornerChangeAt(pLevel, blockpos.below());
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState p_55561_, Level p_55562_, BlockPos p_55563_, Block p_55564_, @Nullable Orientation p_369069_, boolean p_55566_) {
        if (!p_55562_.isClientSide) {
            if (p_55564_ != this || !useExperimentalEvaluator(p_55562_)) {
                if (p_55561_.canSurvive(p_55562_, p_55563_)) {
                    this.updatePowerStrength(p_55562_, p_55563_, p_55561_, p_369069_, false);
                } else {
                    dropResources(p_55561_, p_55562_, p_55563_);
                    p_55562_.removeBlock(p_55563_, false);
                }
            }
        }
    }

    private static boolean useExperimentalEvaluator(Level pLevel) {
        return pLevel.enabledFeatures().contains(FeatureFlags.REDSTONE_EXPERIMENTS);
    }

    @Override
    protected int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return !this.shouldSignal ? 0 : pBlockState.getSignal(pBlockAccess, pPos, pSide);
    }

    @Override
    protected int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        if (this.shouldSignal && pSide != Direction.DOWN) {
            int i = pBlockState.getValue(POWER);
            if (i == 0) {
                return 0;
            } else {
                return pSide != Direction.UP && !this.getConnectionState(pBlockAccess, pBlockState, pPos).getValue(PROPERTY_BY_DIRECTION.get(pSide.getOpposite())).isConnected() ? 0 : i;
            }
        } else {
            return 0;
        }
    }

    protected static boolean shouldConnectTo(BlockState pState) {
        return shouldConnectTo(pState, null);
    }

    protected static boolean shouldConnectTo(BlockState pState, @Nullable Direction pDirection) {
        if (pState.is(Blocks.REDSTONE_WIRE)) {
            return true;
        } else if (pState.is(Blocks.REPEATER)) {
            Direction direction = pState.getValue(RepeaterBlock.FACING);
            return direction == pDirection || direction.getOpposite() == pDirection;
        } else {
            return pState.is(Blocks.OBSERVER) ? pDirection == pState.getValue(ObserverBlock.FACING) : pState.isSignalSource() && pDirection != null;
        }
    }

    @Override
    protected boolean isSignalSource(BlockState pState) {
        return this.shouldSignal;
    }

    public static int getColorForPower(int pPower) {
        return COLORS[pPower];
    }

    private static void spawnParticlesAlongLine(
        Level pLevel, RandomSource pRandom, BlockPos pPos, int pColor, Direction pDirection, Direction pPerpendicularDirection, float pStart, float pEnd
    ) {
        float f = pEnd - pStart;
        if (!(pRandom.nextFloat() >= 0.2F * f)) {
            float f1 = 0.4375F;
            float f2 = pStart + f * pRandom.nextFloat();
            double d0 = 0.5 + 0.4375F * pDirection.getStepX() + f2 * pPerpendicularDirection.getStepX();
            double d1 = 0.5 + 0.4375F * pDirection.getStepY() + f2 * pPerpendicularDirection.getStepY();
            double d2 = 0.5 + 0.4375F * pDirection.getStepZ() + f2 * pPerpendicularDirection.getStepZ();
            pLevel.addParticle(
                new DustParticleOptions(pColor, 1.0F), pPos.getX() + d0, pPos.getY() + d1, pPos.getZ() + d2, 0.0, 0.0, 0.0
            );
        }
    }

    @Override
    public void animateTick(BlockState p_221932_, Level p_221933_, BlockPos p_221934_, RandomSource p_221935_) {
        int i = p_221932_.getValue(POWER);
        if (i != 0) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                RedstoneSide redstoneside = p_221932_.getValue(PROPERTY_BY_DIRECTION.get(direction));
                switch (redstoneside) {
                    case UP:
                        spawnParticlesAlongLine(p_221933_, p_221935_, p_221934_, COLORS[i], direction, Direction.UP, -0.5F, 0.5F);
                    case SIDE:
                        spawnParticlesAlongLine(p_221933_, p_221935_, p_221934_, COLORS[i], Direction.DOWN, direction, 0.0F, 0.5F);
                        break;
                    case NONE:
                    default:
                        spawnParticlesAlongLine(p_221933_, p_221935_, p_221934_, COLORS[i], Direction.DOWN, direction, 0.0F, 0.3F);
                }
            }
        }
    }

    @Override
    protected BlockState rotate(BlockState pState, Rotation pRotation) {
        switch (pRotation) {
            case CLOCKWISE_180:
                return pState.setValue(NORTH, pState.getValue(SOUTH))
                    .setValue(EAST, pState.getValue(WEST))
                    .setValue(SOUTH, pState.getValue(NORTH))
                    .setValue(WEST, pState.getValue(EAST));
            case COUNTERCLOCKWISE_90:
                return pState.setValue(NORTH, pState.getValue(EAST))
                    .setValue(EAST, pState.getValue(SOUTH))
                    .setValue(SOUTH, pState.getValue(WEST))
                    .setValue(WEST, pState.getValue(NORTH));
            case CLOCKWISE_90:
                return pState.setValue(NORTH, pState.getValue(WEST))
                    .setValue(EAST, pState.getValue(NORTH))
                    .setValue(SOUTH, pState.getValue(EAST))
                    .setValue(WEST, pState.getValue(SOUTH));
            default:
                return pState;
        }
    }

    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        switch (pMirror) {
            case LEFT_RIGHT:
                return pState.setValue(NORTH, pState.getValue(SOUTH)).setValue(SOUTH, pState.getValue(NORTH));
            case FRONT_BACK:
                return pState.setValue(EAST, pState.getValue(WEST)).setValue(WEST, pState.getValue(EAST));
            default:
                return super.mirror(pState, pMirror);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(NORTH, EAST, SOUTH, WEST, POWER);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_55554_, Level p_55555_, BlockPos p_55556_, Player p_55557_, BlockHitResult p_55559_) {
        if (!p_55557_.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        } else {
            if (isCross(p_55554_) || isDot(p_55554_)) {
                BlockState blockstate = isCross(p_55554_) ? this.defaultBlockState() : this.crossState;
                blockstate = blockstate.setValue(POWER, p_55554_.getValue(POWER));
                blockstate = this.getConnectionState(p_55555_, blockstate, p_55556_);
                if (blockstate != p_55554_) {
                    p_55555_.setBlock(p_55556_, blockstate, 3);
                    this.updatesOnShapeChange(p_55555_, p_55556_, p_55554_, blockstate);
                    return InteractionResult.SUCCESS;
                }
            }

            return InteractionResult.PASS;
        }
    }

    private void updatesOnShapeChange(Level pLevel, BlockPos pPos, BlockState pOldState, BlockState pNewState) {
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(pLevel, null, Direction.UP);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockpos = pPos.relative(direction);
            if (pOldState.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected() != pNewState.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected()
                && pLevel.getBlockState(blockpos).isRedstoneConductor(pLevel, blockpos)) {
                pLevel.updateNeighborsAtExceptFromFacing(blockpos, pNewState.getBlock(), direction.getOpposite(), ExperimentalRedstoneUtils.withFront(orientation, direction));
            }
        }
    }
}
