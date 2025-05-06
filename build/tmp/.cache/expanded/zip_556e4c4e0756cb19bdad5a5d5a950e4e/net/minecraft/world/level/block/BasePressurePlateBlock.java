package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BasePressurePlateBlock extends Block {
    private static final VoxelShape SHAPE_PRESSED = Block.column(14.0, 0.0, 0.5);
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 1.0);
    protected static final AABB TOUCH_AABB = Block.column(14.0, 0.0, 4.0).toAabbs().getFirst();
    protected final BlockSetType type;

    protected BasePressurePlateBlock(BlockBehaviour.Properties pProperties, BlockSetType pType) {
        super(pProperties.sound(pType.soundType()));
        this.type = pType;
    }

    @Override
    protected abstract MapCodec<? extends BasePressurePlateBlock> codec();

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return this.getSignalForState(pState) > 0 ? SHAPE_PRESSED : SHAPE;
    }

    protected int getPressedTime() {
        return 20;
    }

    @Override
    public boolean isPossibleToRespawnInThis(BlockState p_279155_) {
        return true;
    }

    @Override
    protected BlockState updateShape(
        BlockState p_49329_,
        LevelReader p_366632_,
        ScheduledTickAccess p_363595_,
        BlockPos p_49333_,
        Direction p_49330_,
        BlockPos p_49334_,
        BlockState p_49331_,
        RandomSource p_370163_
    ) {
        return p_49330_ == Direction.DOWN && !p_49329_.canSurvive(p_366632_, p_49333_)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(p_49329_, p_366632_, p_363595_, p_49333_, p_49330_, p_49334_, p_49331_, p_370163_);
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.below();
        return canSupportRigidBlock(pLevel, blockpos) || canSupportCenter(pLevel, blockpos, Direction.UP);
    }

    @Override
    protected void tick(BlockState p_220768_, ServerLevel p_220769_, BlockPos p_220770_, RandomSource p_220771_) {
        int i = this.getSignalForState(p_220768_);
        if (i > 0) {
            this.checkPressed(null, p_220769_, p_220770_, p_220768_, i);
        }
    }

    @Override
    protected void entityInside(BlockState p_49314_, Level p_49315_, BlockPos p_49316_, Entity p_49317_, InsideBlockEffectApplier p_395804_) {
        if (!p_49315_.isClientSide) {
            int i = this.getSignalForState(p_49314_);
            if (i == 0) {
                this.checkPressed(p_49317_, p_49315_, p_49316_, p_49314_, i);
            }
        }
    }

    private void checkPressed(@Nullable Entity pEntity, Level pLevel, BlockPos pPos, BlockState pState, int pCurrentSignal) {
        int i = this.getSignalStrength(pLevel, pPos);
        boolean flag = pCurrentSignal > 0;
        boolean flag1 = i > 0;
        if (pCurrentSignal != i) {
            BlockState blockstate = this.setSignalForState(pState, i);
            pLevel.setBlock(pPos, blockstate, 2);
            this.updateNeighbours(pLevel, pPos);
            pLevel.setBlocksDirty(pPos, pState, blockstate);
        }

        if (!flag1 && flag) {
            pLevel.playSound(null, pPos, this.type.pressurePlateClickOff(), SoundSource.BLOCKS);
            pLevel.gameEvent(pEntity, GameEvent.BLOCK_DEACTIVATE, pPos);
        } else if (flag1 && !flag) {
            pLevel.playSound(null, pPos, this.type.pressurePlateClickOn(), SoundSource.BLOCKS);
            pLevel.gameEvent(pEntity, GameEvent.BLOCK_ACTIVATE, pPos);
        }

        if (flag1) {
            pLevel.scheduleTick(new BlockPos(pPos), this, this.getPressedTime());
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState p_393640_, ServerLevel p_396048_, BlockPos p_394765_, boolean p_397293_) {
        if (!p_397293_ && this.getSignalForState(p_393640_) > 0) {
            this.updateNeighbours(p_396048_, p_394765_);
        }
    }

    protected void updateNeighbours(Level pLevel, BlockPos pPos) {
        pLevel.updateNeighborsAt(pPos, this);
        pLevel.updateNeighborsAt(pPos.below(), this);
    }

    @Override
    protected int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return this.getSignalForState(pBlockState);
    }

    @Override
    protected int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pSide == Direction.UP ? this.getSignalForState(pBlockState) : 0;
    }

    @Override
    protected boolean isSignalSource(BlockState pState) {
        return true;
    }

    protected static int getEntityCount(Level pLevel, AABB pBox, Class<? extends Entity> pEntityClass) {
        return pLevel.getEntitiesOfClass(pEntityClass, pBox, EntitySelector.NO_SPECTATORS.and(p_289691_ -> !p_289691_.isIgnoringBlockTriggers())).size();
    }

    protected abstract int getSignalStrength(Level pLevel, BlockPos pPos);

    protected abstract int getSignalForState(BlockState pState);

    protected abstract BlockState setSignalForState(BlockState pState, int pSignal);
}