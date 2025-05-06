package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeCache;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class CrafterBlock extends BaseEntityBlock {
    public static final MapCodec<CrafterBlock> CODEC = simpleCodec(CrafterBlock::new);
    public static final BooleanProperty CRAFTING = BlockStateProperties.CRAFTING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    private static final EnumProperty<FrontAndTop> ORIENTATION = BlockStateProperties.ORIENTATION;
    private static final int MAX_CRAFTING_TICKS = 6;
    private static final int CRAFTING_TICK_DELAY = 4;
    private static final RecipeCache RECIPE_CACHE = new RecipeCache(10);
    private static final int CRAFTER_ADVANCEMENT_DIAMETER = 17;

    public CrafterBlock(BlockBehaviour.Properties p_310228_) {
        super(p_310228_);
        this.registerDefaultState(this.stateDefinition.any().setValue(ORIENTATION, FrontAndTop.NORTH_UP).setValue(TRIGGERED, false).setValue(CRAFTING, false));
    }

    @Override
    protected MapCodec<CrafterBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState p_309929_) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState p_311332_, Level p_310277_, BlockPos p_312038_) {
        return p_310277_.getBlockEntity(p_312038_) instanceof CrafterBlockEntity crafterblockentity ? crafterblockentity.getRedstoneSignal() : 0;
    }

    @Override
    protected void neighborChanged(BlockState p_309741_, Level p_312714_, BlockPos p_310958_, Block p_313237_, @Nullable Orientation p_364282_, boolean p_309615_) {
        boolean flag = p_312714_.hasNeighborSignal(p_310958_);
        boolean flag1 = p_309741_.getValue(TRIGGERED);
        BlockEntity blockentity = p_312714_.getBlockEntity(p_310958_);
        if (flag && !flag1) {
            p_312714_.scheduleTick(p_310958_, this, 4);
            p_312714_.setBlock(p_310958_, p_309741_.setValue(TRIGGERED, true), 2);
            this.setBlockEntityTriggered(blockentity, true);
        } else if (!flag && flag1) {
            p_312714_.setBlock(p_310958_, p_309741_.setValue(TRIGGERED, false).setValue(CRAFTING, false), 2);
            this.setBlockEntityTriggered(blockentity, false);
        }
    }

    @Override
    protected void tick(BlockState p_310321_, ServerLevel p_312701_, BlockPos p_311281_, RandomSource p_311092_) {
        this.dispenseFrom(p_310321_, p_312701_, p_311281_);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_310928_, BlockState p_311648_, BlockEntityType<T> p_310343_) {
        return p_310928_.isClientSide ? null : createTickerHelper(p_310343_, BlockEntityType.CRAFTER, CrafterBlockEntity::serverTick);
    }

    private void setBlockEntityTriggered(@Nullable BlockEntity pBlockEntity, boolean pTriggered) {
        if (pBlockEntity instanceof CrafterBlockEntity crafterblockentity) {
            crafterblockentity.setTriggered(pTriggered);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_311818_, BlockState p_310225_) {
        CrafterBlockEntity crafterblockentity = new CrafterBlockEntity(p_311818_, p_310225_);
        crafterblockentity.setTriggered(p_310225_.hasProperty(TRIGGERED) && p_310225_.getValue(TRIGGERED));
        return crafterblockentity;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_311294_) {
        Direction direction = p_311294_.getNearestLookingDirection().getOpposite();

        Direction direction1 = switch (direction) {
            case DOWN -> p_311294_.getHorizontalDirection().getOpposite();
            case UP -> p_311294_.getHorizontalDirection();
            case NORTH, SOUTH, WEST, EAST -> Direction.UP;
        };
        return this.defaultBlockState()
            .setValue(ORIENTATION, FrontAndTop.fromFrontAndTop(direction, direction1))
            .setValue(TRIGGERED, p_311294_.getLevel().hasNeighborSignal(p_311294_.getClickedPos()));
    }

    @Override
    public void setPlacedBy(Level p_311617_, BlockPos p_313069_, BlockState p_310230_, LivingEntity p_310379_, ItemStack p_311227_) {
        if (p_310230_.getValue(TRIGGERED)) {
            p_311617_.scheduleTick(p_313069_, this, 4);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState p_395375_, ServerLevel p_396974_, BlockPos p_391725_, boolean p_392912_) {
        Containers.updateNeighboursAfterDestroy(p_395375_, p_396974_, p_391725_);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_309704_, Level p_312700_, BlockPos p_310945_, Player p_312953_, BlockHitResult p_309965_) {
        if (!p_312700_.isClientSide && p_312700_.getBlockEntity(p_310945_) instanceof CrafterBlockEntity crafterblockentity) {
            p_312953_.openMenu(crafterblockentity);
        }

        return InteractionResult.SUCCESS;
    }

    protected void dispenseFrom(BlockState pState, ServerLevel pLevel, BlockPos pPos) {
        if (pLevel.getBlockEntity(pPos) instanceof CrafterBlockEntity crafterblockentity) {
            CraftingInput craftinginput = crafterblockentity.asCraftInput();
            Optional<RecipeHolder<CraftingRecipe>> optional = getPotentialResults(pLevel, craftinginput);
            if (optional.isEmpty()) {
                pLevel.levelEvent(1050, pPos, 0);
            } else {
                RecipeHolder<CraftingRecipe> recipeholder = optional.get();
                ItemStack itemstack = recipeholder.value().assemble(craftinginput, pLevel.registryAccess());
                if (itemstack.isEmpty()) {
                    pLevel.levelEvent(1050, pPos, 0);
                } else {
                    crafterblockentity.setCraftingTicksRemaining(6);
                    pLevel.setBlock(pPos, pState.setValue(CRAFTING, true), 2);
                    itemstack.onCraftedBySystem(pLevel);
                    this.dispenseItem(pLevel, pPos, crafterblockentity, itemstack, pState, recipeholder);

                    for (ItemStack itemstack1 : recipeholder.value().getRemainingItems(craftinginput)) {
                        if (!itemstack1.isEmpty()) {
                            this.dispenseItem(pLevel, pPos, crafterblockentity, itemstack1, pState, recipeholder);
                        }
                    }

                    crafterblockentity.getItems().forEach(p_312802_ -> {
                        if (!p_312802_.isEmpty()) {
                            p_312802_.shrink(1);
                        }
                    });
                    crafterblockentity.setChanged();
                }
            }
        }
    }

    public static Optional<RecipeHolder<CraftingRecipe>> getPotentialResults(ServerLevel pLevel, CraftingInput pCraftingInput) {
        return RECIPE_CACHE.get(pLevel, pCraftingInput);
    }

    private void dispenseItem(
        ServerLevel pLevel, BlockPos pPos, CrafterBlockEntity pCrafter, ItemStack pStack, BlockState pState, RecipeHolder<?> pRecipe
    ) {
        Direction direction = pState.getValue(ORIENTATION).front();
        Container container = HopperBlockEntity.getContainerAt(pLevel, pPos.relative(direction));
        ItemStack itemstack = pStack.copy();
        if (container != null && (container instanceof CrafterBlockEntity || pStack.getCount() > container.getMaxStackSize(pStack))) {
            while (!itemstack.isEmpty()) {
                ItemStack itemstack2 = itemstack.copyWithCount(1);
                ItemStack itemstack1 = HopperBlockEntity.addItem(pCrafter, container, itemstack2, direction.getOpposite());
                if (!itemstack1.isEmpty()) {
                    break;
                }

                itemstack.shrink(1);
            }
        } else if (container != null) {
            while (!itemstack.isEmpty()) {
                int i = itemstack.getCount();
                itemstack = HopperBlockEntity.addItem(pCrafter, container, itemstack, direction.getOpposite());
                if (i == itemstack.getCount()) {
                    break;
                }
            }
        }

        if (!itemstack.isEmpty()) {
            Vec3 vec3 = Vec3.atCenterOf(pPos);
            Vec3 vec31 = vec3.relative(direction, 0.7);
            DefaultDispenseItemBehavior.spawnItem(pLevel, itemstack, 6, direction, vec31);

            for (ServerPlayer serverplayer : pLevel.getEntitiesOfClass(ServerPlayer.class, AABB.ofSize(vec3, 17.0, 17.0, 17.0))) {
                CriteriaTriggers.CRAFTER_RECIPE_CRAFTED.trigger(serverplayer, pRecipe.id(), pCrafter.getItems());
            }

            pLevel.levelEvent(1049, pPos, 0);
            pLevel.levelEvent(2010, pPos, direction.get3DDataValue());
        }
    }

    @Override
    protected BlockState rotate(BlockState p_312403_, Rotation p_309910_) {
        return p_312403_.setValue(ORIENTATION, p_309910_.rotation().rotate(p_312403_.getValue(ORIENTATION)));
    }

    @Override
    protected BlockState mirror(BlockState p_310178_, Mirror p_311418_) {
        return p_310178_.setValue(ORIENTATION, p_311418_.rotation().rotate(p_310178_.getValue(ORIENTATION)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_310076_) {
        p_310076_.add(ORIENTATION, TRIGGERED, CRAFTING);
    }
}