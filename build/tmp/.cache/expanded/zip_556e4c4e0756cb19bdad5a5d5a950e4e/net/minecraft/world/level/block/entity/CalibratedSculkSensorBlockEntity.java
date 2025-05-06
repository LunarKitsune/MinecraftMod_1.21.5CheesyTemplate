package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CalibratedSculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;

public class CalibratedSculkSensorBlockEntity extends SculkSensorBlockEntity {
    public CalibratedSculkSensorBlockEntity(BlockPos p_277459_, BlockState p_278100_) {
        super(BlockEntityType.CALIBRATED_SCULK_SENSOR, p_277459_, p_278100_);
    }

    @Override
    public VibrationSystem.User createVibrationUser() {
        return new CalibratedSculkSensorBlockEntity.VibrationUser(this.getBlockPos());
    }

    protected class VibrationUser extends SculkSensorBlockEntity.VibrationUser {
        public VibrationUser(final BlockPos pPos) {
            super(pPos);
        }

        @Override
        public int getListenerRadius() {
            return 16;
        }

        @Override
        public boolean canReceiveVibration(ServerLevel p_282061_, BlockPos p_282550_, Holder<GameEvent> p_329832_, @Nullable GameEvent.Context p_281456_) {
            int i = this.getBackSignal(p_282061_, this.blockPos, CalibratedSculkSensorBlockEntity.this.getBlockState());
            return i != 0 && VibrationSystem.getGameEventFrequency(p_329832_) != i ? false : super.canReceiveVibration(p_282061_, p_282550_, p_329832_, p_281456_);
        }

        private int getBackSignal(Level pLevel, BlockPos pPos, BlockState pState) {
            Direction direction = pState.getValue(CalibratedSculkSensorBlock.FACING).getOpposite();
            return pLevel.getSignal(pPos.relative(direction), direction);
        }
    }
}