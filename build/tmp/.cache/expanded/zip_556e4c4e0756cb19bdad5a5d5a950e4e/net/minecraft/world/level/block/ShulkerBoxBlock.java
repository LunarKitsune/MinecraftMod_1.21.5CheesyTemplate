package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ShulkerBoxBlock extends BaseEntityBlock {
    public static final MapCodec<ShulkerBoxBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360450_ -> p_360450_.group(DyeColor.CODEC.optionalFieldOf("color").forGetter(p_309293_ -> Optional.ofNullable(p_309293_.color)), propertiesCodec())
            .apply(p_360450_, (p_309290_, p_309291_) -> new ShulkerBoxBlock(p_309290_.orElse(null), p_309291_))
    );
    public static final Map<Direction, VoxelShape> SHAPES_OPEN_SUPPORT = Shapes.rotateAll(Block.boxZ(16.0, 0.0, 1.0));
    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
    public static final ResourceLocation CONTENTS = ResourceLocation.withDefaultNamespace("contents");
    @Nullable
    private final DyeColor color;

    @Override
    public MapCodec<ShulkerBoxBlock> codec() {
        return CODEC;
    }

    public ShulkerBoxBlock(@Nullable DyeColor pColor, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.color = pColor;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_154552_, BlockState p_154553_) {
        return new ShulkerBoxBlockEntity(this.color, p_154552_, p_154553_);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_154543_, BlockState p_154544_, BlockEntityType<T> p_154545_) {
        return createTickerHelper(p_154545_, BlockEntityType.SHULKER_BOX, ShulkerBoxBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_56227_, Level p_56228_, BlockPos p_56229_, Player p_56230_, BlockHitResult p_56232_) {
        if (p_56228_ instanceof ServerLevel serverlevel
            && p_56228_.getBlockEntity(p_56229_) instanceof ShulkerBoxBlockEntity shulkerboxblockentity
            && canOpen(p_56227_, p_56228_, p_56229_, shulkerboxblockentity)) {
            p_56230_.openMenu(shulkerboxblockentity);
            p_56230_.awardStat(Stats.OPEN_SHULKER_BOX);
            PiglinAi.angerNearbyPiglins(serverlevel, p_56230_, true);
        }

        return InteractionResult.SUCCESS;
    }

    private static boolean canOpen(BlockState pState, Level pLevel, BlockPos pPos, ShulkerBoxBlockEntity pBlockEntity) {
        if (pBlockEntity.getAnimationStatus() != ShulkerBoxBlockEntity.AnimationStatus.CLOSED) {
            return true;
        } else {
            AABB aabb = Shulker.getProgressDeltaAabb(1.0F, pState.getValue(FACING), 0.0F, 0.5F, pPos.getBottomCenter()).deflate(1.0E-6);
            return pLevel.noCollision(aabb);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getClickedFace());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

    @Override
    public BlockState playerWillDestroy(Level p_56212_, BlockPos p_56213_, BlockState p_56214_, Player p_56215_) {
        BlockEntity blockentity = p_56212_.getBlockEntity(p_56213_);
        if (blockentity instanceof ShulkerBoxBlockEntity shulkerboxblockentity) {
            if (!p_56212_.isClientSide && p_56215_.preventsBlockDrops() && !shulkerboxblockentity.isEmpty()) {
                ItemStack itemstack = getColoredItemStack(this.getColor());
                itemstack.applyComponents(blockentity.collectComponents());
                ItemEntity itementity = new ItemEntity(p_56212_, p_56213_.getX() + 0.5, p_56213_.getY() + 0.5, p_56213_.getZ() + 0.5, itemstack);
                itementity.setDefaultPickUpDelay();
                p_56212_.addFreshEntity(itementity);
            } else {
                shulkerboxblockentity.unpackLootTable(p_56215_);
            }
        }

        return super.playerWillDestroy(p_56212_, p_56213_, p_56214_, p_56215_);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState p_287632_, LootParams.Builder p_287691_) {
        BlockEntity blockentity = p_287691_.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockentity instanceof ShulkerBoxBlockEntity shulkerboxblockentity) {
            p_287691_ = p_287691_.withDynamicDrop(CONTENTS, p_56219_ -> {
                for (int i = 0; i < shulkerboxblockentity.getContainerSize(); i++) {
                    p_56219_.accept(shulkerboxblockentity.getItem(i));
                }
            });
        }

        return super.getDrops(p_287632_, p_287691_);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState p_393214_, ServerLevel p_391858_, BlockPos p_393972_, boolean p_396098_) {
        Containers.updateNeighboursAfterDestroy(p_393214_, p_391858_, p_393972_);
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState p_259177_, BlockGetter p_260305_, BlockPos p_259168_) {
        return p_260305_.getBlockEntity(p_259168_) instanceof ShulkerBoxBlockEntity shulkerboxblockentity && !shulkerboxblockentity.isClosed()
            ? SHAPES_OPEN_SUPPORT.get(p_259177_.getValue(FACING).getOpposite())
            : Shapes.block();
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return pLevel.getBlockEntity(pPos) instanceof ShulkerBoxBlockEntity shulkerboxblockentity
            ? Shapes.create(shulkerboxblockentity.getBoundingBox(pState))
            : Shapes.block();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState p_330948_) {
        return false;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState pState) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(pLevel.getBlockEntity(pPos));
    }

    public static Block getBlockByColor(@Nullable DyeColor pColor) {
        if (pColor == null) {
            return Blocks.SHULKER_BOX;
        } else {
            return switch (pColor) {
                case WHITE -> Blocks.WHITE_SHULKER_BOX;
                case ORANGE -> Blocks.ORANGE_SHULKER_BOX;
                case MAGENTA -> Blocks.MAGENTA_SHULKER_BOX;
                case LIGHT_BLUE -> Blocks.LIGHT_BLUE_SHULKER_BOX;
                case YELLOW -> Blocks.YELLOW_SHULKER_BOX;
                case LIME -> Blocks.LIME_SHULKER_BOX;
                case PINK -> Blocks.PINK_SHULKER_BOX;
                case GRAY -> Blocks.GRAY_SHULKER_BOX;
                case LIGHT_GRAY -> Blocks.LIGHT_GRAY_SHULKER_BOX;
                case CYAN -> Blocks.CYAN_SHULKER_BOX;
                case BLUE -> Blocks.BLUE_SHULKER_BOX;
                case BROWN -> Blocks.BROWN_SHULKER_BOX;
                case GREEN -> Blocks.GREEN_SHULKER_BOX;
                case RED -> Blocks.RED_SHULKER_BOX;
                case BLACK -> Blocks.BLACK_SHULKER_BOX;
                case PURPLE -> Blocks.PURPLE_SHULKER_BOX;
            };
        }
    }

    @Nullable
    public DyeColor getColor() {
        return this.color;
    }

    public static ItemStack getColoredItemStack(@Nullable DyeColor pColor) {
        return new ItemStack(getBlockByColor(pColor));
    }

    @Override
    protected BlockState rotate(BlockState pState, Rotation pRot) {
        return pState.setValue(FACING, pRot.rotate(pState.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }
}