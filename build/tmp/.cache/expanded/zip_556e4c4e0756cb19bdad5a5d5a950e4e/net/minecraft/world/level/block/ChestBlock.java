package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChestBlock extends AbstractChestBlock<ChestBlockEntity> implements SimpleWaterloggedBlock {
    public static final MapCodec<ChestBlock> CODEC = simpleCodec(p_360418_ -> new ChestBlock(() -> BlockEntityType.CHEST, p_360418_));
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<ChestType> TYPE = BlockStateProperties.CHEST_TYPE;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final int EVENT_SET_OPEN_COUNT = 1;
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 14.0);
    private static final Map<Direction, VoxelShape> HALF_SHAPES = Shapes.rotateHorizontal(Block.boxZ(14.0, 0.0, 14.0, 0.0, 15.0));
    private static final DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<Container>> CHEST_COMBINER = new DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<Container>>() {
        public Optional<Container> acceptDouble(ChestBlockEntity p_51591_, ChestBlockEntity p_51592_) {
            return Optional.of(new CompoundContainer(p_51591_, p_51592_));
        }

        public Optional<Container> acceptSingle(ChestBlockEntity p_51589_) {
            return Optional.of(p_51589_);
        }

        public Optional<Container> acceptNone() {
            return Optional.empty();
        }
    };
    private static final DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<MenuProvider>> MENU_PROVIDER_COMBINER = new DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<MenuProvider>>() {
        public Optional<MenuProvider> acceptDouble(final ChestBlockEntity p_51604_, final ChestBlockEntity p_51605_) {
            final Container container = new CompoundContainer(p_51604_, p_51605_);
            return Optional.of(new MenuProvider() {
                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int p_51622_, Inventory p_51623_, Player p_51624_) {
                    if (p_51604_.canOpen(p_51624_) && p_51605_.canOpen(p_51624_)) {
                        p_51604_.unpackLootTable(p_51623_.player);
                        p_51605_.unpackLootTable(p_51623_.player);
                        return ChestMenu.sixRows(p_51622_, p_51623_, container);
                    } else {
                        return null;
                    }
                }

                @Override
                public Component getDisplayName() {
                    if (p_51604_.hasCustomName()) {
                        return p_51604_.getDisplayName();
                    } else {
                        return (Component)(p_51605_.hasCustomName() ? p_51605_.getDisplayName() : Component.translatable("container.chestDouble"));
                    }
                }
            });
        }

        public Optional<MenuProvider> acceptSingle(ChestBlockEntity p_51602_) {
            return Optional.of(p_51602_);
        }

        public Optional<MenuProvider> acceptNone() {
            return Optional.empty();
        }
    };

    @Override
    public MapCodec<? extends ChestBlock> codec() {
        return CODEC;
    }

    public ChestBlock(Supplier<BlockEntityType<? extends ChestBlockEntity>> pBlockEntityType, BlockBehaviour.Properties pProperties) {
        super(pProperties, pBlockEntityType);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, ChestType.SINGLE).setValue(WATERLOGGED, false));
    }

    public static DoubleBlockCombiner.BlockType getBlockType(BlockState pState) {
        ChestType chesttype = pState.getValue(TYPE);
        if (chesttype == ChestType.SINGLE) {
            return DoubleBlockCombiner.BlockType.SINGLE;
        } else {
            return chesttype == ChestType.RIGHT ? DoubleBlockCombiner.BlockType.FIRST : DoubleBlockCombiner.BlockType.SECOND;
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_51555_,
        LevelReader p_362695_,
        ScheduledTickAccess p_362061_,
        BlockPos p_51559_,
        Direction p_51556_,
        BlockPos p_51560_,
        BlockState p_51557_,
        RandomSource p_368899_
    ) {
        if (p_51555_.getValue(WATERLOGGED)) {
            p_362061_.scheduleTick(p_51559_, Fluids.WATER, Fluids.WATER.getTickDelay(p_362695_));
        }

        if (p_51557_.is(this) && p_51556_.getAxis().isHorizontal()) {
            ChestType chesttype = p_51557_.getValue(TYPE);
            if (p_51555_.getValue(TYPE) == ChestType.SINGLE
                && chesttype != ChestType.SINGLE
                && p_51555_.getValue(FACING) == p_51557_.getValue(FACING)
                && getConnectedDirection(p_51557_) == p_51556_.getOpposite()) {
                return p_51555_.setValue(TYPE, chesttype.getOpposite());
            }
        } else if (getConnectedDirection(p_51555_) == p_51556_) {
            return p_51555_.setValue(TYPE, ChestType.SINGLE);
        }

        return super.updateShape(p_51555_, p_362695_, p_362061_, p_51559_, p_51556_, p_51560_, p_51557_, p_368899_);
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return switch ((ChestType)pState.getValue(TYPE)) {
            case SINGLE -> SHAPE;
            case LEFT, RIGHT -> (VoxelShape)HALF_SHAPES.get(getConnectedDirection(pState));
        };
    }

    public static Direction getConnectedDirection(BlockState pState) {
        Direction direction = pState.getValue(FACING);
        return pState.getValue(TYPE) == ChestType.LEFT ? direction.getClockWise() : direction.getCounterClockWise();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        ChestType chesttype = ChestType.SINGLE;
        Direction direction = pContext.getHorizontalDirection().getOpposite();
        FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
        boolean flag = pContext.isSecondaryUseActive();
        Direction direction1 = pContext.getClickedFace();
        if (direction1.getAxis().isHorizontal() && flag) {
            Direction direction2 = this.candidatePartnerFacing(pContext, direction1.getOpposite());
            if (direction2 != null && direction2.getAxis() != direction1.getAxis()) {
                direction = direction2;
                chesttype = direction2.getCounterClockWise() == direction1.getOpposite() ? ChestType.RIGHT : ChestType.LEFT;
            }
        }

        if (chesttype == ChestType.SINGLE && !flag) {
            if (direction == this.candidatePartnerFacing(pContext, direction.getClockWise())) {
                chesttype = ChestType.LEFT;
            } else if (direction == this.candidatePartnerFacing(pContext, direction.getCounterClockWise())) {
                chesttype = ChestType.RIGHT;
            }
        }

        return this.defaultBlockState().setValue(FACING, direction).setValue(TYPE, chesttype).setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
    }

    @Override
    protected FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Nullable
    private Direction candidatePartnerFacing(BlockPlaceContext pContext, Direction pDirection) {
        BlockState blockstate = pContext.getLevel().getBlockState(pContext.getClickedPos().relative(pDirection));
        return blockstate.is(this) && blockstate.getValue(TYPE) == ChestType.SINGLE ? blockstate.getValue(FACING) : null;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState p_397064_, ServerLevel p_396255_, BlockPos p_393735_, boolean p_392386_) {
        Containers.updateNeighboursAfterDestroy(p_397064_, p_396255_, p_393735_);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_51531_, Level p_51532_, BlockPos p_51533_, Player p_51534_, BlockHitResult p_51536_) {
        if (p_51532_ instanceof ServerLevel serverlevel) {
            MenuProvider menuprovider = this.getMenuProvider(p_51531_, p_51532_, p_51533_);
            if (menuprovider != null) {
                p_51534_.openMenu(menuprovider);
                p_51534_.awardStat(this.getOpenChestStat());
                PiglinAi.angerNearbyPiglins(serverlevel, p_51534_, true);
            }
        }

        return InteractionResult.SUCCESS;
    }

    protected Stat<ResourceLocation> getOpenChestStat() {
        return Stats.CUSTOM.get(Stats.OPEN_CHEST);
    }

    public BlockEntityType<? extends ChestBlockEntity> blockEntityType() {
        return this.blockEntityType.get();
    }

    @Nullable
    public static Container getContainer(ChestBlock pChest, BlockState pState, Level pLevel, BlockPos pPos, boolean pOverride) {
        return pChest.combine(pState, pLevel, pPos, pOverride).apply(CHEST_COMBINER).orElse(null);
    }

    @Override
    public DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combine(
        BlockState pState, Level pLevel, BlockPos pPos, boolean pOverride
    ) {
        BiPredicate<LevelAccessor, BlockPos> bipredicate;
        if (pOverride) {
            bipredicate = (p_51578_, p_51579_) -> false;
        } else {
            bipredicate = ChestBlock::isChestBlockedAt;
        }

        return DoubleBlockCombiner.combineWithNeigbour(
            this.blockEntityType.get(), ChestBlock::getBlockType, ChestBlock::getConnectedDirection, FACING, pState, pLevel, pPos, bipredicate
        );
    }

    @Nullable
    @Override
    protected MenuProvider getMenuProvider(BlockState pState, Level pLevel, BlockPos pPos) {
        return this.combine(pState, pLevel, pPos, false).apply(MENU_PROVIDER_COMBINER).orElse(null);
    }

    public static DoubleBlockCombiner.Combiner<ChestBlockEntity, Float2FloatFunction> opennessCombiner(final LidBlockEntity pLid) {
        return new DoubleBlockCombiner.Combiner<ChestBlockEntity, Float2FloatFunction>() {
            public Float2FloatFunction acceptDouble(ChestBlockEntity p_51633_, ChestBlockEntity p_51634_) {
                return p_51638_ -> Math.max(p_51633_.getOpenNess(p_51638_), p_51634_.getOpenNess(p_51638_));
            }

            public Float2FloatFunction acceptSingle(ChestBlockEntity p_51631_) {
                return p_51631_::getOpenNess;
            }

            public Float2FloatFunction acceptNone() {
                return pLid::getOpenNess;
            }
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_153064_, BlockState p_153065_) {
        return new ChestBlockEntity(p_153064_, p_153065_);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_153055_, BlockState p_153056_, BlockEntityType<T> p_153057_) {
        return p_153055_.isClientSide ? createTickerHelper(p_153057_, this.blockEntityType(), ChestBlockEntity::lidAnimateTick) : null;
    }

    public static boolean isChestBlockedAt(LevelAccessor pLevel, BlockPos pPos) {
        return isBlockedChestByBlock(pLevel, pPos) || isCatSittingOnChest(pLevel, pPos);
    }

    private static boolean isBlockedChestByBlock(BlockGetter pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.above();
        return pLevel.getBlockState(blockpos).isRedstoneConductor(pLevel, blockpos);
    }

    private static boolean isCatSittingOnChest(LevelAccessor pLevel, BlockPos pPos) {
        List<Cat> list = pLevel.getEntitiesOfClass(
            Cat.class,
            new AABB(
                pPos.getX(),
                pPos.getY() + 1,
                pPos.getZ(),
                pPos.getX() + 1,
                pPos.getY() + 2,
                pPos.getZ() + 1
            )
        );
        if (!list.isEmpty()) {
            for (Cat cat : list) {
                if (cat.isInSittingPose()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState pState) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos) {
        return AbstractContainerMenu.getRedstoneSignalFromContainer(getContainer(this, pBlockState, pLevel, pPos, false));
    }

    @Override
    protected BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        BlockState rotated = pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
        return pMirror == Mirror.NONE ? rotated : rotated.setValue(TYPE, rotated.getValue(TYPE).getOpposite());  // Forge: Fixed MC-134110 Structure mirroring breaking apart double chests
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, TYPE, WATERLOGGED);
    }

    @Override
    protected boolean isPathfindable(BlockState p_51522_, PathComputationType p_51525_) {
        return false;
    }

    @Override
    protected void tick(BlockState p_220958_, ServerLevel p_220959_, BlockPos p_220960_, RandomSource p_220961_) {
        BlockEntity blockentity = p_220959_.getBlockEntity(p_220960_);
        if (blockentity instanceof ChestBlockEntity) {
            ((ChestBlockEntity)blockentity).recheckOpen();
        }
    }
}
