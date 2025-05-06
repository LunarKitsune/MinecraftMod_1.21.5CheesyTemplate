package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BaseFireBlock extends Block {
    private static final int SECONDS_ON_FIRE = 8;
    private static final int MIN_FIRE_TICKS_TO_ADD = 1;
    private static final int MAX_FIRE_TICKS_TO_ADD = 3;
    private final float fireDamage;
    protected static final VoxelShape SHAPE = Block.column(16.0, 0.0, 1.0);

    public BaseFireBlock(BlockBehaviour.Properties pProperties, float pFireDamage) {
        super(pProperties);
        this.fireDamage = pFireDamage;
    }

    @Override
    protected abstract MapCodec<? extends BaseFireBlock> codec();

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return getState(pContext.getLevel(), pContext.getClickedPos());
    }

    public static BlockState getState(BlockGetter pReader, BlockPos pPos) {
        BlockPos blockpos = pPos.below();
        BlockState blockstate = pReader.getBlockState(blockpos);
        return SoulFireBlock.canSurviveOnBlock(blockstate) ? Blocks.SOUL_FIRE.defaultBlockState() : ((FireBlock)Blocks.FIRE).getStateForPlacement(pReader, pPos);
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState p_220763_, Level p_220764_, BlockPos p_220765_, RandomSource p_220766_) {
        if (p_220766_.nextInt(24) == 0) {
            p_220764_.playLocalSound(
                p_220765_.getX() + 0.5,
                p_220765_.getY() + 0.5,
                p_220765_.getZ() + 0.5,
                SoundEvents.FIRE_AMBIENT,
                SoundSource.BLOCKS,
                1.0F + p_220766_.nextFloat(),
                p_220766_.nextFloat() * 0.7F + 0.3F,
                false
            );
        }

        BlockPos blockpos = p_220765_.below();
        BlockState blockstate = p_220764_.getBlockState(blockpos);
        if (!this.canBurn(blockstate) && !blockstate.isFaceSturdy(p_220764_, blockpos, Direction.UP)) {
            if (this.canBurn(p_220764_.getBlockState(p_220765_.west()))) {
                for (int j = 0; j < 2; j++) {
                    double d3 = p_220765_.getX() + p_220766_.nextDouble() * 0.1F;
                    double d8 = p_220765_.getY() + p_220766_.nextDouble();
                    double d13 = p_220765_.getZ() + p_220766_.nextDouble();
                    p_220764_.addParticle(ParticleTypes.LARGE_SMOKE, d3, d8, d13, 0.0, 0.0, 0.0);
                }
            }

            if (this.canBurn(p_220764_.getBlockState(p_220765_.east()))) {
                for (int k = 0; k < 2; k++) {
                    double d4 = p_220765_.getX() + 1 - p_220766_.nextDouble() * 0.1F;
                    double d9 = p_220765_.getY() + p_220766_.nextDouble();
                    double d14 = p_220765_.getZ() + p_220766_.nextDouble();
                    p_220764_.addParticle(ParticleTypes.LARGE_SMOKE, d4, d9, d14, 0.0, 0.0, 0.0);
                }
            }

            if (this.canBurn(p_220764_.getBlockState(p_220765_.north()))) {
                for (int l = 0; l < 2; l++) {
                    double d5 = p_220765_.getX() + p_220766_.nextDouble();
                    double d10 = p_220765_.getY() + p_220766_.nextDouble();
                    double d15 = p_220765_.getZ() + p_220766_.nextDouble() * 0.1F;
                    p_220764_.addParticle(ParticleTypes.LARGE_SMOKE, d5, d10, d15, 0.0, 0.0, 0.0);
                }
            }

            if (this.canBurn(p_220764_.getBlockState(p_220765_.south()))) {
                for (int i1 = 0; i1 < 2; i1++) {
                    double d6 = p_220765_.getX() + p_220766_.nextDouble();
                    double d11 = p_220765_.getY() + p_220766_.nextDouble();
                    double d16 = p_220765_.getZ() + 1 - p_220766_.nextDouble() * 0.1F;
                    p_220764_.addParticle(ParticleTypes.LARGE_SMOKE, d6, d11, d16, 0.0, 0.0, 0.0);
                }
            }

            if (this.canBurn(p_220764_.getBlockState(p_220765_.above()))) {
                for (int j1 = 0; j1 < 2; j1++) {
                    double d7 = p_220765_.getX() + p_220766_.nextDouble();
                    double d12 = p_220765_.getY() + 1 - p_220766_.nextDouble() * 0.1F;
                    double d17 = p_220765_.getZ() + p_220766_.nextDouble();
                    p_220764_.addParticle(ParticleTypes.LARGE_SMOKE, d7, d12, d17, 0.0, 0.0, 0.0);
                }
            }
        } else {
            for (int i = 0; i < 3; i++) {
                double d0 = p_220765_.getX() + p_220766_.nextDouble();
                double d1 = p_220765_.getY() + p_220766_.nextDouble() * 0.5 + 0.5;
                double d2 = p_220765_.getZ() + p_220766_.nextDouble();
                p_220764_.addParticle(ParticleTypes.LARGE_SMOKE, d0, d1, d2, 0.0, 0.0, 0.0);
            }
        }
    }

    protected abstract boolean canBurn(BlockState pState);

    @Override
    protected void entityInside(BlockState p_49260_, Level p_49261_, BlockPos p_49262_, Entity p_49263_, InsideBlockEffectApplier p_397865_) {
        p_397865_.apply(InsideBlockEffectType.FIRE_IGNITE);
        p_397865_.runAfter(InsideBlockEffectType.FIRE_IGNITE, p_394328_ -> p_394328_.hurt(p_394328_.level().damageSources().inFire(), this.fireDamage));
    }

    public static void fireIgnite(Entity pEntity) {
        if (!pEntity.fireImmune()) {
            if (pEntity.getRemainingFireTicks() < 0) {
                pEntity.setRemainingFireTicks(pEntity.getRemainingFireTicks() + 1);
            } else if (pEntity instanceof ServerPlayer) {
                int i = pEntity.level().getRandom().nextInt(1, 3);
                pEntity.setRemainingFireTicks(pEntity.getRemainingFireTicks() + i);
            }

            if (pEntity.getRemainingFireTicks() >= 0) {
                pEntity.igniteForSeconds(8.0F);
            }
        }
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (!pOldState.is(pState.getBlock())) {
            if (inPortalDimension(pLevel)) {
                Optional<PortalShape> optional = PortalShape.findEmptyPortalShape(pLevel, pPos, Direction.Axis.X);
                optional = net.minecraftforge.event.ForgeEventFactory.onTrySpawnPortal(pLevel, pPos, optional);
                if (optional.isPresent()) {
                    optional.get().createPortalBlocks(pLevel);
                    return;
                }
            }

            if (!pState.canSurvive(pLevel, pPos)) {
                pLevel.removeBlock(pPos, false);
            }
        }
    }

    private static boolean inPortalDimension(Level pLevel) {
        return pLevel.dimension() == Level.OVERWORLD || pLevel.dimension() == Level.NETHER;
    }

    @Override
    protected void spawnDestroyParticles(Level p_152139_, Player p_152140_, BlockPos p_152141_, BlockState p_152142_) {
    }

    @Override
    public BlockState playerWillDestroy(Level p_49251_, BlockPos p_49252_, BlockState p_49253_, Player p_49254_) {
        if (!p_49251_.isClientSide()) {
            p_49251_.levelEvent(null, 1009, p_49252_, 0);
        }

        return super.playerWillDestroy(p_49251_, p_49252_, p_49253_, p_49254_);
    }

    public static boolean canBePlacedAt(Level pLevel, BlockPos pPos, Direction pDirection) {
        BlockState blockstate = pLevel.getBlockState(pPos);
        return !blockstate.isAir() ? false : getState(pLevel, pPos).canSurvive(pLevel, pPos) || isPortal(pLevel, pPos, pDirection);
    }

    private static boolean isPortal(Level pLevel, BlockPos pPos, Direction pDirection) {
        if (!inPortalDimension(pLevel)) {
            return false;
        } else {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable();
            boolean flag = false;

            for (Direction direction : Direction.values()) {
                if (pLevel.getBlockState(blockpos$mutableblockpos.set(pPos).move(direction)).isPortalFrame(pLevel, blockpos$mutableblockpos)) {
                    flag = true;
                    break;
                }
            }

            if (!flag) {
                return false;
            } else {
                Direction.Axis direction$axis = pDirection.getAxis().isHorizontal()
                    ? pDirection.getCounterClockWise().getAxis()
                    : Direction.Plane.HORIZONTAL.getRandomAxis(pLevel.random);
                return PortalShape.findEmptyPortalShape(pLevel, pPos, direction$axis).isPresent();
            }
        }
    }
}
