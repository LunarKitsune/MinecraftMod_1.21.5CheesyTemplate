package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;

public class SculkSensorBlockEntity extends BlockEntity implements GameEventListener.Provider<VibrationSystem.Listener>, VibrationSystem {
    private static final int DEFAULT_LAST_VIBRATION_FREQUENCY = 0;
    private VibrationSystem.Data vibrationData;
    private final VibrationSystem.Listener vibrationListener;
    private final VibrationSystem.User vibrationUser;
    private int lastVibrationFrequency = 0;

    protected SculkSensorBlockEntity(BlockEntityType<?> p_277405_, BlockPos p_277502_, BlockState p_277699_) {
        super(p_277405_, p_277502_, p_277699_);
        this.vibrationUser = this.createVibrationUser();
        this.vibrationData = new VibrationSystem.Data();
        this.vibrationListener = new VibrationSystem.Listener(this);
    }

    public SculkSensorBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(BlockEntityType.SCULK_SENSOR, pPos, pBlockState);
    }

    public VibrationSystem.User createVibrationUser() {
        return new SculkSensorBlockEntity.VibrationUser(this.getBlockPos());
    }

    @Override
    protected void loadAdditional(CompoundTag p_334658_, HolderLookup.Provider p_335301_) {
        super.loadAdditional(p_334658_, p_335301_);
        this.lastVibrationFrequency = p_334658_.getIntOr("last_vibration_frequency", 0);
        RegistryOps<Tag> registryops = p_335301_.createSerializationContext(NbtOps.INSTANCE);
        this.vibrationData = p_334658_.read("listener", VibrationSystem.Data.CODEC, registryops).orElseGet(VibrationSystem.Data::new);
    }

    @Override
    protected void saveAdditional(CompoundTag p_187511_, HolderLookup.Provider p_327837_) {
        super.saveAdditional(p_187511_, p_327837_);
        p_187511_.putInt("last_vibration_frequency", this.lastVibrationFrequency);
        RegistryOps<Tag> registryops = p_327837_.createSerializationContext(NbtOps.INSTANCE);
        p_187511_.store("listener", VibrationSystem.Data.CODEC, registryops, this.vibrationData);
    }

    @Override
    public VibrationSystem.Data getVibrationData() {
        return this.vibrationData;
    }

    @Override
    public VibrationSystem.User getVibrationUser() {
        return this.vibrationUser;
    }

    public int getLastVibrationFrequency() {
        return this.lastVibrationFrequency;
    }

    public void setLastVibrationFrequency(int pLastVibrationFrequency) {
        this.lastVibrationFrequency = pLastVibrationFrequency;
    }

    public VibrationSystem.Listener getListener() {
        return this.vibrationListener;
    }

    protected class VibrationUser implements VibrationSystem.User {
        public static final int LISTENER_RANGE = 8;
        protected final BlockPos blockPos;
        private final PositionSource positionSource;

        public VibrationUser(final BlockPos pPos) {
            this.blockPos = pPos;
            this.positionSource = new BlockPositionSource(pPos);
        }

        @Override
        public int getListenerRadius() {
            return 8;
        }

        @Override
        public PositionSource getPositionSource() {
            return this.positionSource;
        }

        @Override
        public boolean canTriggerAvoidVibration() {
            return true;
        }

        @Override
        public boolean canReceiveVibration(ServerLevel p_282127_, BlockPos p_283268_, Holder<GameEvent> p_329159_, @Nullable GameEvent.Context p_282856_) {
            if (!p_283268_.equals(this.blockPos) || !p_329159_.is(GameEvent.BLOCK_DESTROY) && !p_329159_.is(GameEvent.BLOCK_PLACE)) {
                return VibrationSystem.getGameEventFrequency(p_329159_) == 0 ? false : SculkSensorBlock.canActivate(SculkSensorBlockEntity.this.getBlockState());
            } else {
                return false;
            }
        }

        @Override
        public void onReceiveVibration(
            ServerLevel p_282851_, BlockPos p_281608_, Holder<GameEvent> p_331761_, @Nullable Entity p_282123_, @Nullable Entity p_283090_, float p_283130_
        ) {
            BlockState blockstate = SculkSensorBlockEntity.this.getBlockState();
            if (SculkSensorBlock.canActivate(blockstate)) {
                int i = VibrationSystem.getGameEventFrequency(p_331761_);
                SculkSensorBlockEntity.this.setLastVibrationFrequency(i);
                int j = VibrationSystem.getRedstoneStrengthForDistance(p_283130_, this.getListenerRadius());
                if (blockstate.getBlock() instanceof SculkSensorBlock sculksensorblock) {
                    sculksensorblock.activate(p_282123_, p_282851_, this.blockPos, blockstate, j, i);
                }
            }
        }

        @Override
        public void onDataChanged() {
            SculkSensorBlockEntity.this.setChanged();
        }

        @Override
        public boolean requiresAdjacentChunksToBeTicking() {
            return true;
        }
    }
}