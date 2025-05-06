package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BellBlock extends BaseEntityBlock {
    public static final MapCodec<BellBlock> CODEC = simpleCodec(BellBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<BellAttachType> ATTACHMENT = BlockStateProperties.BELL_ATTACHMENT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape BELL_SHAPE = Shapes.or(Block.column(6.0, 6.0, 13.0), Block.column(8.0, 4.0, 6.0));
    private static final VoxelShape SHAPE_CEILING = Shapes.or(BELL_SHAPE, Block.column(2.0, 13.0, 16.0));
    private static final Map<Direction.Axis, VoxelShape> SHAPE_FLOOR = Shapes.rotateHorizontalAxis(Block.cube(16.0, 16.0, 8.0));
    private static final Map<Direction.Axis, VoxelShape> SHAPE_DOUBLE_WALL = Shapes.rotateHorizontalAxis(Shapes.or(BELL_SHAPE, Block.column(2.0, 16.0, 13.0, 15.0)));
    private static final Map<Direction, VoxelShape> SHAPE_SINGLE_WALL = Shapes.rotateHorizontal(Shapes.or(BELL_SHAPE, Block.boxZ(2.0, 13.0, 15.0, 0.0, 13.0)));
    public static final int EVENT_BELL_RING = 1;

    @Override
    public MapCodec<BellBlock> codec() {
        return CODEC;
    }

    public BellBlock(BlockBehaviour.Properties p_49696_) {
        super(p_49696_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ATTACHMENT, BellAttachType.FLOOR).setValue(POWERED, false));
    }

    @Override
    protected void neighborChanged(BlockState p_49729_, Level p_49730_, BlockPos p_49731_, Block p_49732_, @Nullable Orientation p_365293_, boolean p_49734_) {
        boolean flag = p_49730_.hasNeighborSignal(p_49731_);
        if (flag != p_49729_.getValue(POWERED)) {
            if (flag) {
                this.attemptToRing(p_49730_, p_49731_, null);
            }

            p_49730_.setBlock(p_49731_, p_49729_.setValue(POWERED, flag), 3);
        }
    }

    @Override
    protected void onProjectileHit(Level pLevel, BlockState pState, BlockHitResult pHit, Projectile pProjectile) {
        Player player = pProjectile.getOwner() instanceof Player player1 ? player1 : null;
        this.onHit(pLevel, pState, pHit, player, true);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_49722_, Level p_49723_, BlockPos p_49724_, Player p_49725_, BlockHitResult p_49727_) {
        return (InteractionResult)(this.onHit(p_49723_, p_49722_, p_49727_, p_49725_, true) ? InteractionResult.SUCCESS : InteractionResult.PASS);
    }

    public boolean onHit(Level pLevel, BlockState pState, BlockHitResult pResult, @Nullable Player pPlayer, boolean pCanRingBell) {
        Direction direction = pResult.getDirection();
        BlockPos blockpos = pResult.getBlockPos();
        boolean flag = !pCanRingBell || this.isProperHit(pState, direction, pResult.getLocation().y - blockpos.getY());
        if (flag) {
            boolean flag1 = this.attemptToRing(pPlayer, pLevel, blockpos, direction);
            if (flag1 && pPlayer != null) {
                pPlayer.awardStat(Stats.BELL_RING);
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean isProperHit(BlockState pPos, Direction pDirection, double pDistanceY) {
        if (pDirection.getAxis() != Direction.Axis.Y && !(pDistanceY > 0.8124F)) {
            Direction direction = pPos.getValue(FACING);
            BellAttachType bellattachtype = pPos.getValue(ATTACHMENT);
            switch (bellattachtype) {
                case FLOOR:
                    return direction.getAxis() == pDirection.getAxis();
                case SINGLE_WALL:
                case DOUBLE_WALL:
                    return direction.getAxis() != pDirection.getAxis();
                case CEILING:
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    public boolean attemptToRing(Level pLevel, BlockPos pPos, @Nullable Direction pDirection) {
        return this.attemptToRing(null, pLevel, pPos, pDirection);
    }

    public boolean attemptToRing(@Nullable Entity pEntity, Level pLevel, BlockPos pPos, @Nullable Direction pDirection) {
        BlockEntity blockentity = pLevel.getBlockEntity(pPos);
        if (!pLevel.isClientSide && blockentity instanceof BellBlockEntity) {
            if (pDirection == null) {
                pDirection = pLevel.getBlockState(pPos).getValue(FACING);
            }

            ((BellBlockEntity)blockentity).onHit(pDirection);
            pLevel.playSound(null, pPos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 1.0F);
            pLevel.gameEvent(pEntity, GameEvent.BLOCK_CHANGE, pPos);
            return true;
        } else {
            return false;
        }
    }

    private VoxelShape getVoxelShape(BlockState pState) {
        Direction direction = pState.getValue(FACING);

        return switch ((BellAttachType)pState.getValue(ATTACHMENT)) {
            case FLOOR -> (VoxelShape)SHAPE_FLOOR.get(direction.getAxis());
            case SINGLE_WALL -> (VoxelShape)SHAPE_SINGLE_WALL.get(direction);
            case DOUBLE_WALL -> (VoxelShape)SHAPE_DOUBLE_WALL.get(direction.getAxis());
            case CEILING -> SHAPE_CEILING;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return this.getVoxelShape(pState);
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return this.getVoxelShape(pState);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        Direction direction = pContext.getClickedFace();
        BlockPos blockpos = pContext.getClickedPos();
        Level level = pContext.getLevel();
        Direction.Axis direction$axis = direction.getAxis();
        if (direction$axis == Direction.Axis.Y) {
            BlockState blockstate = this.defaultBlockState()
                .setValue(ATTACHMENT, direction == Direction.DOWN ? BellAttachType.CEILING : BellAttachType.FLOOR)
                .setValue(FACING, pContext.getHorizontalDirection());
            if (blockstate.canSurvive(pContext.getLevel(), blockpos)) {
                return blockstate;
            }
        } else {
            boolean flag = direction$axis == Direction.Axis.X
                    && level.getBlockState(blockpos.west()).isFaceSturdy(level, blockpos.west(), Direction.EAST)
                    && level.getBlockState(blockpos.east()).isFaceSturdy(level, blockpos.east(), Direction.WEST)
                || direction$axis == Direction.Axis.Z
                    && level.getBlockState(blockpos.north()).isFaceSturdy(level, blockpos.north(), Direction.SOUTH)
                    && level.getBlockState(blockpos.south()).isFaceSturdy(level, blockpos.south(), Direction.NORTH);
            BlockState blockstate1 = this.defaultBlockState()
                .setValue(FACING, direction.getOpposite())
                .setValue(ATTACHMENT, flag ? BellAttachType.DOUBLE_WALL : BellAttachType.SINGLE_WALL);
            if (blockstate1.canSurvive(pContext.getLevel(), pContext.getClickedPos())) {
                return blockstate1;
            }

            boolean flag1 = level.getBlockState(blockpos.below()).isFaceSturdy(level, blockpos.below(), Direction.UP);
            blockstate1 = blockstate1.setValue(ATTACHMENT, flag1 ? BellAttachType.FLOOR : BellAttachType.CEILING);
            if (blockstate1.canSurvive(pContext.getLevel(), pContext.getClickedPos())) {
                return blockstate1;
            }
        }

        return null;
    }

    @Override
    protected void onExplosionHit(BlockState p_311155_, ServerLevel p_370069_, BlockPos p_311109_, Explosion p_312563_, BiConsumer<ItemStack, BlockPos> p_311850_) {
        if (p_312563_.canTriggerBlocks()) {
            this.attemptToRing(p_370069_, p_311109_, null);
        }

        super.onExplosionHit(p_311155_, p_370069_, p_311109_, p_312563_, p_311850_);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_49744_,
        LevelReader p_361159_,
        ScheduledTickAccess p_361050_,
        BlockPos p_49748_,
        Direction p_49745_,
        BlockPos p_49749_,
        BlockState p_49746_,
        RandomSource p_362217_
    ) {
        BellAttachType bellattachtype = p_49744_.getValue(ATTACHMENT);
        Direction direction = getConnectedDirection(p_49744_).getOpposite();
        if (direction == p_49745_ && !p_49744_.canSurvive(p_361159_, p_49748_) && bellattachtype != BellAttachType.DOUBLE_WALL) {
            return Blocks.AIR.defaultBlockState();
        } else {
            if (p_49745_.getAxis() == p_49744_.getValue(FACING).getAxis()) {
                if (bellattachtype == BellAttachType.DOUBLE_WALL && !p_49746_.isFaceSturdy(p_361159_, p_49749_, p_49745_)) {
                    return p_49744_.setValue(ATTACHMENT, BellAttachType.SINGLE_WALL).setValue(FACING, p_49745_.getOpposite());
                }

                if (bellattachtype == BellAttachType.SINGLE_WALL
                    && direction.getOpposite() == p_49745_
                    && p_49746_.isFaceSturdy(p_361159_, p_49749_, p_49744_.getValue(FACING))) {
                    return p_49744_.setValue(ATTACHMENT, BellAttachType.DOUBLE_WALL);
                }
            }

            return super.updateShape(p_49744_, p_361159_, p_361050_, p_49748_, p_49745_, p_49749_, p_49746_, p_362217_);
        }
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        Direction direction = getConnectedDirection(pState).getOpposite();
        return direction == Direction.UP
            ? Block.canSupportCenter(pLevel, pPos.above(), Direction.DOWN)
            : FaceAttachedHorizontalDirectionalBlock.canAttach(pLevel, pPos, direction);
    }

    private static Direction getConnectedDirection(BlockState pState) {
        switch ((BellAttachType)pState.getValue(ATTACHMENT)) {
            case FLOOR:
                return Direction.UP;
            case CEILING:
                return Direction.DOWN;
            default:
                return pState.getValue(FACING).getOpposite();
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, ATTACHMENT, POWERED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos p_152198_, BlockState p_152199_) {
        return new BellBlockEntity(p_152198_, p_152199_);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_152194_, BlockState p_152195_, BlockEntityType<T> p_152196_) {
        return createTickerHelper(p_152196_, BlockEntityType.BELL, p_152194_.isClientSide ? BellBlockEntity::clientTick : BellBlockEntity::serverTick);
    }

    @Override
    protected boolean isPathfindable(BlockState p_49717_, PathComputationType p_49720_) {
        return false;
    }

    @Override
    public BlockState rotate(BlockState p_311584_, Rotation p_311968_) {
        return p_311584_.setValue(FACING, p_311968_.rotate(p_311584_.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState p_311443_, Mirror p_309746_) {
        return p_311443_.rotate(p_309746_.getRotation(p_311443_.getValue(FACING)));
    }
}