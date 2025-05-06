package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CampfireBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<CampfireBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360413_ -> p_360413_.group(
                Codec.BOOL.fieldOf("spawn_particles").forGetter(p_309275_ -> p_309275_.spawnParticles),
                Codec.intRange(0, 1000).fieldOf("fire_damage").forGetter(p_309277_ -> p_309277_.fireDamage),
                propertiesCodec()
            )
            .apply(p_360413_, CampfireBlock::new)
    );
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty SIGNAL_FIRE = BlockStateProperties.SIGNAL_FIRE;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 7.0);
    private static final VoxelShape SHAPE_VIRTUAL_POST = Block.column(4.0, 0.0, 16.0);
    private static final int SMOKE_DISTANCE = 5;
    private final boolean spawnParticles;
    private final int fireDamage;

    @Override
    public MapCodec<CampfireBlock> codec() {
        return CODEC;
    }

    public CampfireBlock(boolean pSpawnParticles, int pFireDamage, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.spawnParticles = pSpawnParticles;
        this.fireDamage = pFireDamage;
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, true).setValue(SIGNAL_FIRE, false).setValue(WATERLOGGED, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_334288_, BlockState p_51274_, Level p_51275_, BlockPos p_51276_, Player p_51277_, InteractionHand p_51278_, BlockHitResult p_51279_
    ) {
        if (p_51275_.getBlockEntity(p_51276_) instanceof CampfireBlockEntity campfireblockentity) {
            ItemStack itemstack = p_51277_.getItemInHand(p_51278_);
            if (p_51275_.recipeAccess().propertySet(RecipePropertySet.CAMPFIRE_INPUT).test(itemstack)) {
                if (p_51275_ instanceof ServerLevel serverlevel && campfireblockentity.placeFood(serverlevel, p_51277_, itemstack)) {
                    p_51277_.awardStat(Stats.INTERACT_WITH_CAMPFIRE);
                    return InteractionResult.SUCCESS_SERVER;
                }

                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected void entityInside(BlockState p_51269_, Level p_51270_, BlockPos p_51271_, Entity p_51272_, InsideBlockEffectApplier p_395061_) {
        if (p_51269_.getValue(LIT) && p_51272_ instanceof LivingEntity) {
            p_51272_.hurt(p_51270_.damageSources().campfire(), this.fireDamage);
        }

        super.entityInside(p_51269_, p_51270_, p_51271_, p_51272_, p_395061_);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        LevelAccessor levelaccessor = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        boolean flag = levelaccessor.getFluidState(blockpos).getType() == Fluids.WATER;
        return this.defaultBlockState()
            .setValue(WATERLOGGED, flag)
            .setValue(SIGNAL_FIRE, this.isSmokeSource(levelaccessor.getBlockState(blockpos.below())))
            .setValue(LIT, !flag)
            .setValue(FACING, pContext.getHorizontalDirection());
    }

    @Override
    protected BlockState updateShape(
        BlockState p_51298_,
        LevelReader p_368205_,
        ScheduledTickAccess p_365108_,
        BlockPos p_51302_,
        Direction p_51299_,
        BlockPos p_51303_,
        BlockState p_51300_,
        RandomSource p_366447_
    ) {
        if (p_51298_.getValue(WATERLOGGED)) {
            p_365108_.scheduleTick(p_51302_, Fluids.WATER, Fluids.WATER.getTickDelay(p_368205_));
        }

        return p_51299_ == Direction.DOWN
            ? p_51298_.setValue(SIGNAL_FIRE, this.isSmokeSource(p_51300_))
            : super.updateShape(p_51298_, p_368205_, p_365108_, p_51302_, p_51299_, p_51303_, p_51300_, p_366447_);
    }

    private boolean isSmokeSource(BlockState pState) {
        return pState.is(Blocks.HAY_BLOCK);
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState p_220918_, Level p_220919_, BlockPos p_220920_, RandomSource p_220921_) {
        if (p_220918_.getValue(LIT)) {
            if (p_220921_.nextInt(10) == 0) {
                p_220919_.playLocalSound(
                    p_220920_.getX() + 0.5,
                    p_220920_.getY() + 0.5,
                    p_220920_.getZ() + 0.5,
                    SoundEvents.CAMPFIRE_CRACKLE,
                    SoundSource.BLOCKS,
                    0.5F + p_220921_.nextFloat(),
                    p_220921_.nextFloat() * 0.7F + 0.6F,
                    false
                );
            }

            if (this.spawnParticles && p_220921_.nextInt(5) == 0) {
                for (int i = 0; i < p_220921_.nextInt(1) + 1; i++) {
                    p_220919_.addParticle(
                        ParticleTypes.LAVA,
                        p_220920_.getX() + 0.5,
                        p_220920_.getY() + 0.5,
                        p_220920_.getZ() + 0.5,
                        p_220921_.nextFloat() / 2.0F,
                        5.0E-5,
                        p_220921_.nextFloat() / 2.0F
                    );
                }
            }
        }
    }

    public static void dowse(@Nullable Entity pEntity, LevelAccessor pLevel, BlockPos pPos, BlockState pState) {
        if (pLevel.isClientSide()) {
            for (int i = 0; i < 20; i++) {
                makeParticles((Level)pLevel, pPos, pState.getValue(SIGNAL_FIRE), true);
            }
        }

        pLevel.gameEvent(pEntity, GameEvent.BLOCK_CHANGE, pPos);
    }

    @Override
    public boolean placeLiquid(LevelAccessor pLevel, BlockPos pPos, BlockState pState, FluidState pFluidState) {
        if (!pState.getValue(BlockStateProperties.WATERLOGGED) && pFluidState.getType() == Fluids.WATER) {
            boolean flag = pState.getValue(LIT);
            if (flag) {
                if (!pLevel.isClientSide()) {
                    pLevel.playSound(null, pPos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0F, 1.0F);
                }

                dowse(null, pLevel, pPos, pState);
            }

            pLevel.setBlock(pPos, pState.setValue(WATERLOGGED, true).setValue(LIT, false), 3);
            pLevel.scheduleTick(pPos, pFluidState.getType(), pFluidState.getType().getTickDelay(pLevel));
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onProjectileHit(Level pLevel, BlockState pState, BlockHitResult pHit, Projectile pProjectile) {
        BlockPos blockpos = pHit.getBlockPos();
        if (pLevel instanceof ServerLevel serverlevel
            && pProjectile.isOnFire()
            && pProjectile.mayInteract(serverlevel, blockpos)
            && !pState.getValue(LIT)
            && !pState.getValue(WATERLOGGED)) {
            pLevel.setBlock(blockpos, pState.setValue(BlockStateProperties.LIT, true), 11);
        }
    }

    public static void makeParticles(Level pLevel, BlockPos pPos, boolean pIsSignalFire, boolean pSpawnExtraSmoke) {
        RandomSource randomsource = pLevel.getRandom();
        SimpleParticleType simpleparticletype = pIsSignalFire ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE;
        pLevel.addAlwaysVisibleParticle(
            simpleparticletype,
            true,
            pPos.getX() + 0.5 + randomsource.nextDouble() / 3.0 * (randomsource.nextBoolean() ? 1 : -1),
            pPos.getY() + randomsource.nextDouble() + randomsource.nextDouble(),
            pPos.getZ() + 0.5 + randomsource.nextDouble() / 3.0 * (randomsource.nextBoolean() ? 1 : -1),
            0.0,
            0.07,
            0.0
        );
        if (pSpawnExtraSmoke) {
            pLevel.addParticle(
                ParticleTypes.SMOKE,
                pPos.getX() + 0.5 + randomsource.nextDouble() / 4.0 * (randomsource.nextBoolean() ? 1 : -1),
                pPos.getY() + 0.4,
                pPos.getZ() + 0.5 + randomsource.nextDouble() / 4.0 * (randomsource.nextBoolean() ? 1 : -1),
                0.0,
                0.005,
                0.0
            );
        }
    }

    public static boolean isSmokeyPos(Level pLevel, BlockPos pPos) {
        for (int i = 1; i <= 5; i++) {
            BlockPos blockpos = pPos.below(i);
            BlockState blockstate = pLevel.getBlockState(blockpos);
            if (isLitCampfire(blockstate)) {
                return true;
            }

            boolean flag = Shapes.joinIsNotEmpty(SHAPE_VIRTUAL_POST, blockstate.getCollisionShape(pLevel, blockpos, CollisionContext.empty()), BooleanOp.AND); // FORGE: Fix MC-201374
            if (flag) {
                BlockState blockstate1 = pLevel.getBlockState(blockpos.below());
                return isLitCampfire(blockstate1);
            }
        }

        return false;
    }

    public static boolean isLitCampfire(BlockState pState) {
        return pState.hasProperty(LIT) && pState.is(BlockTags.CAMPFIRES) && pState.getValue(LIT);
    }

    @Override
    protected FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Override
    protected BlockState rotate(BlockState pState, Rotation pRot) {
        return pState.setValue(FACING, pRot.rotate(pState.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(LIT, SIGNAL_FIRE, WATERLOGGED, FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_152759_, BlockState p_152760_) {
        return new CampfireBlockEntity(p_152759_, p_152760_);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_152755_, BlockState p_152756_, BlockEntityType<T> p_152757_) {
        if (p_152755_ instanceof ServerLevel serverlevel) {
            if (p_152756_.getValue(LIT)) {
                RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> cachedcheck = RecipeManager.createCheck(RecipeType.CAMPFIRE_COOKING);
                return createTickerHelper(
                    p_152757_,
                    BlockEntityType.CAMPFIRE,
                    (p_360409_, p_360410_, p_360411_, p_360412_) -> CampfireBlockEntity.cookTick(serverlevel, p_360410_, p_360411_, p_360412_, cachedcheck)
                );
            } else {
                return createTickerHelper(p_152757_, BlockEntityType.CAMPFIRE, CampfireBlockEntity::cooldownTick);
            }
        } else {
            return p_152756_.getValue(LIT) ? createTickerHelper(p_152757_, BlockEntityType.CAMPFIRE, CampfireBlockEntity::particleTick) : null;
        }
    }

    @Override
    protected boolean isPathfindable(BlockState p_51264_, PathComputationType p_51267_) {
        return false;
    }

    public static boolean canLight(BlockState pState) {
        return pState.is(BlockTags.CAMPFIRES, p_51262_ -> p_51262_.hasProperty(WATERLOGGED) && p_51262_.hasProperty(LIT))
            && !pState.getValue(WATERLOGGED)
            && !pState.getValue(LIT);
    }
}
