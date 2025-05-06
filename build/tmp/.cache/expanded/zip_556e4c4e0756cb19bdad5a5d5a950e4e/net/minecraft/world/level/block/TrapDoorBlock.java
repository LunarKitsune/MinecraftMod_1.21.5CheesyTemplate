package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TrapDoorBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<TrapDoorBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360458_ -> p_360458_.group(BlockSetType.CODEC.fieldOf("block_set_type").forGetter(p_311609_ -> p_311609_.type), propertiesCodec())
            .apply(p_360458_, TrapDoorBlock::new)
    );
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateAll(Block.boxZ(16.0, 13.0, 16.0));
    private final BlockSetType type;

    @Override
    public MapCodec<? extends TrapDoorBlock> codec() {
        return CODEC;
    }

    public TrapDoorBlock(BlockSetType pType, BlockBehaviour.Properties pProperties) {
        super(pProperties.sound(pType.soundType()));
        this.type = pType;
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(HALF, Half.BOTTOM)
                .setValue(POWERED, false)
                .setValue(WATERLOGGED, false)
        );
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES.get(
            pState.getValue(OPEN) ? pState.getValue(FACING) : (pState.getValue(HALF) == Half.TOP ? Direction.DOWN : Direction.UP)
        );
    }

    @Override
    protected boolean isPathfindable(BlockState p_57535_, PathComputationType p_57538_) {
        switch (p_57538_) {
            case LAND:
                return p_57535_.getValue(OPEN);
            case WATER:
                return p_57535_.getValue(WATERLOGGED);
            case AIR:
                return p_57535_.getValue(OPEN);
            default:
                return false;
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_57540_, Level p_57541_, BlockPos p_57542_, Player p_57543_, BlockHitResult p_57545_) {
        if (!this.type.canOpenByHand()) {
            return InteractionResult.PASS;
        } else {
            this.toggle(p_57540_, p_57541_, p_57542_, p_57543_);
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    protected void onExplosionHit(BlockState p_312876_, ServerLevel p_365086_, BlockPos p_312697_, Explosion p_312889_, BiConsumer<ItemStack, BlockPos> p_312223_) {
        if (p_312889_.canTriggerBlocks() && this.type.canOpenByWindCharge() && !p_312876_.getValue(POWERED)) {
            this.toggle(p_312876_, p_365086_, p_312697_, null);
        }

        super.onExplosionHit(p_312876_, p_365086_, p_312697_, p_312889_, p_312223_);
    }

    private void toggle(BlockState pState, Level pLevel, BlockPos pPos, @Nullable Player pPlayer) {
        BlockState blockstate = pState.cycle(OPEN);
        pLevel.setBlock(pPos, blockstate, 2);
        if (blockstate.getValue(WATERLOGGED)) {
            pLevel.scheduleTick(pPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
        }

        this.playSound(pPlayer, pLevel, pPos, blockstate.getValue(OPEN));
    }

    protected void playSound(@Nullable Player pPlayer, Level pLevel, BlockPos pPos, boolean pIsOpened) {
        pLevel.playSound(
            pPlayer,
            pPos,
            pIsOpened ? this.type.trapdoorOpen() : this.type.trapdoorClose(),
            SoundSource.BLOCKS,
            1.0F,
            pLevel.getRandom().nextFloat() * 0.1F + 0.9F
        );
        pLevel.gameEvent(pPlayer, pIsOpened ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pPos);
    }

    @Override
    protected void neighborChanged(BlockState p_57547_, Level p_57548_, BlockPos p_57549_, Block p_57550_, @Nullable Orientation p_363180_, boolean p_57552_) {
        if (!p_57548_.isClientSide) {
            boolean flag = p_57548_.hasNeighborSignal(p_57549_);
            if (flag != p_57547_.getValue(POWERED)) {
                if (p_57547_.getValue(OPEN) != flag) {
                    p_57547_ = p_57547_.setValue(OPEN, flag);
                    this.playSound(null, p_57548_, p_57549_, flag);
                }

                p_57548_.setBlock(p_57549_, p_57547_.setValue(POWERED, flag), 2);
                if (p_57547_.getValue(WATERLOGGED)) {
                    p_57548_.scheduleTick(p_57549_, Fluids.WATER, Fluids.WATER.getTickDelay(p_57548_));
                }
            }
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockState blockstate = this.defaultBlockState();
        FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
        Direction direction = pContext.getClickedFace();
        if (!pContext.replacingClickedOnBlock() && direction.getAxis().isHorizontal()) {
            blockstate = blockstate.setValue(FACING, direction)
                .setValue(HALF, pContext.getClickLocation().y - pContext.getClickedPos().getY() > 0.5 ? Half.TOP : Half.BOTTOM);
        } else {
            blockstate = blockstate.setValue(FACING, pContext.getHorizontalDirection().getOpposite()).setValue(HALF, direction == Direction.UP ? Half.BOTTOM : Half.TOP);
        }

        if (pContext.getLevel().hasNeighborSignal(pContext.getClickedPos())) {
            blockstate = blockstate.setValue(OPEN, true).setValue(POWERED, true);
        }

        return blockstate.setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, OPEN, HALF, POWERED, WATERLOGGED);
    }

    @Override
    protected FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_57554_,
        LevelReader p_365791_,
        ScheduledTickAccess p_368436_,
        BlockPos p_57558_,
        Direction p_57555_,
        BlockPos p_57559_,
        BlockState p_57556_,
        RandomSource p_367698_
    ) {
        if (p_57554_.getValue(WATERLOGGED)) {
            p_368436_.scheduleTick(p_57558_, Fluids.WATER, Fluids.WATER.getTickDelay(p_365791_));
        }

        return super.updateShape(p_57554_, p_365791_, p_368436_, p_57558_, p_57555_, p_57559_, p_57556_, p_367698_);
    }

    protected BlockSetType getType() {
        return this.type;
    }

    @Override
    public boolean isLadder(BlockState state, net.minecraft.world.level.LevelReader world, BlockPos pos, net.minecraft.world.entity.LivingEntity entity) {
        if (state.getValue(OPEN)) {
            BlockPos downPos = pos.below();
            BlockState down = world.getBlockState(downPos);
            return down.getBlock().makesOpenTrapdoorAboveClimbable(down, world, downPos, state);
        }
        return false;
    }
}
