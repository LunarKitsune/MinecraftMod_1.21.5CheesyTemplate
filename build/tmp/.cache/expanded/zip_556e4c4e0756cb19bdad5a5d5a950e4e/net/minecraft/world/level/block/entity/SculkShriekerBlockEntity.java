package net.minecraft.world.level.block.entity;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;

public class SculkShriekerBlockEntity extends BlockEntity implements GameEventListener.Provider<VibrationSystem.Listener>, VibrationSystem {
    private static final int WARNING_SOUND_RADIUS = 10;
    private static final int WARDEN_SPAWN_ATTEMPTS = 20;
    private static final int WARDEN_SPAWN_RANGE_XZ = 5;
    private static final int WARDEN_SPAWN_RANGE_Y = 6;
    private static final int DARKNESS_RADIUS = 40;
    private static final int SHRIEKING_TICKS = 90;
    private static final Int2ObjectMap<SoundEvent> SOUND_BY_LEVEL = Util.make(new Int2ObjectOpenHashMap<>(), p_222866_ -> {
        p_222866_.put(1, SoundEvents.WARDEN_NEARBY_CLOSE);
        p_222866_.put(2, SoundEvents.WARDEN_NEARBY_CLOSER);
        p_222866_.put(3, SoundEvents.WARDEN_NEARBY_CLOSEST);
        p_222866_.put(4, SoundEvents.WARDEN_LISTENING_ANGRY);
    });
    private static final int DEFAULT_WARNING_LEVEL = 0;
    private int warningLevel = 0;
    private final VibrationSystem.User vibrationUser = new SculkShriekerBlockEntity.VibrationUser();
    private VibrationSystem.Data vibrationData = new VibrationSystem.Data();
    private final VibrationSystem.Listener vibrationListener = new VibrationSystem.Listener(this);

    public SculkShriekerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.SCULK_SHRIEKER, pPos, pBlockState);
    }

    @Override
    public VibrationSystem.Data getVibrationData() {
        return this.vibrationData;
    }

    @Override
    public VibrationSystem.User getVibrationUser() {
        return this.vibrationUser;
    }

    @Override
    protected void loadAdditional(CompoundTag p_327746_, HolderLookup.Provider p_335650_) {
        super.loadAdditional(p_327746_, p_335650_);
        this.warningLevel = p_327746_.getIntOr("warning_level", 0);
        RegistryOps<Tag> registryops = p_335650_.createSerializationContext(NbtOps.INSTANCE);
        this.vibrationData = p_327746_.read("listener", VibrationSystem.Data.CODEC, registryops).orElseGet(VibrationSystem.Data::new);
    }

    @Override
    protected void saveAdditional(CompoundTag p_222878_, HolderLookup.Provider p_330845_) {
        super.saveAdditional(p_222878_, p_330845_);
        p_222878_.putInt("warning_level", this.warningLevel);
        RegistryOps<Tag> registryops = p_330845_.createSerializationContext(NbtOps.INSTANCE);
        p_222878_.store("listener", VibrationSystem.Data.CODEC, registryops, this.vibrationData);
    }

    @Nullable
    public static ServerPlayer tryGetPlayer(@Nullable Entity pEntity) {
        if (pEntity instanceof ServerPlayer serverplayer1) {
            return serverplayer1;
        } else if (pEntity != null && pEntity.getControllingPassenger() instanceof ServerPlayer serverplayer) {
            return serverplayer;
        } else if (pEntity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer serverplayer3) {
            return serverplayer3;
        } else {
            return pEntity instanceof ItemEntity itementity && itementity.getOwner() instanceof ServerPlayer serverplayer2 ? serverplayer2 : null;
        }
    }

    public void tryShriek(ServerLevel pLevel, @Nullable ServerPlayer pPlayer) {
        if (pPlayer != null) {
            BlockState blockstate = this.getBlockState();
            if (!blockstate.getValue(SculkShriekerBlock.SHRIEKING)) {
                this.warningLevel = 0;
                if (!this.canRespond(pLevel) || this.tryToWarn(pLevel, pPlayer)) {
                    this.shriek(pLevel, pPlayer);
                }
            }
        }
    }

    private boolean tryToWarn(ServerLevel pLevel, ServerPlayer pPlayer) {
        OptionalInt optionalint = WardenSpawnTracker.tryWarn(pLevel, this.getBlockPos(), pPlayer);
        optionalint.ifPresent(p_222838_ -> this.warningLevel = p_222838_);
        return optionalint.isPresent();
    }

    private void shriek(ServerLevel pLevel, @Nullable Entity pSourceEntity) {
        BlockPos blockpos = this.getBlockPos();
        BlockState blockstate = this.getBlockState();
        pLevel.setBlock(blockpos, blockstate.setValue(SculkShriekerBlock.SHRIEKING, true), 2);
        pLevel.scheduleTick(blockpos, blockstate.getBlock(), 90);
        pLevel.levelEvent(3007, blockpos, 0);
        pLevel.gameEvent(GameEvent.SHRIEK, blockpos, GameEvent.Context.of(pSourceEntity));
    }

    private boolean canRespond(ServerLevel pLevel) {
        return this.getBlockState().getValue(SculkShriekerBlock.CAN_SUMMON)
            && pLevel.getDifficulty() != Difficulty.PEACEFUL
            && pLevel.getGameRules().getBoolean(GameRules.RULE_DO_WARDEN_SPAWNING);
    }

    @Override
    public void preRemoveSideEffects(BlockPos p_392349_, BlockState p_398015_) {
        if (p_398015_.getValue(SculkShriekerBlock.SHRIEKING) && this.level instanceof ServerLevel serverlevel) {
            this.tryRespond(serverlevel);
        }
    }

    public void tryRespond(ServerLevel pLevel) {
        if (this.canRespond(pLevel) && this.warningLevel > 0) {
            if (!this.trySummonWarden(pLevel)) {
                this.playWardenReplySound(pLevel);
            }

            Warden.applyDarknessAround(pLevel, Vec3.atCenterOf(this.getBlockPos()), null, 40);
        }
    }

    private void playWardenReplySound(Level pLevel) {
        SoundEvent soundevent = SOUND_BY_LEVEL.get(this.warningLevel);
        if (soundevent != null) {
            BlockPos blockpos = this.getBlockPos();
            int i = blockpos.getX() + Mth.randomBetweenInclusive(pLevel.random, -10, 10);
            int j = blockpos.getY() + Mth.randomBetweenInclusive(pLevel.random, -10, 10);
            int k = blockpos.getZ() + Mth.randomBetweenInclusive(pLevel.random, -10, 10);
            pLevel.playSound(null, i, j, k, soundevent, SoundSource.HOSTILE, 5.0F, 1.0F);
        }
    }

    private boolean trySummonWarden(ServerLevel pLevel) {
        return this.warningLevel < 4
            ? false
            : SpawnUtil.trySpawnMob(EntityType.WARDEN, EntitySpawnReason.TRIGGERED, pLevel, this.getBlockPos(), 20, 5, 6, SpawnUtil.Strategy.ON_TOP_OF_COLLIDER, false)
                .isPresent();
    }

    public VibrationSystem.Listener getListener() {
        return this.vibrationListener;
    }

    class VibrationUser implements VibrationSystem.User {
        private static final int LISTENER_RADIUS = 8;
        private final PositionSource positionSource = new BlockPositionSource(SculkShriekerBlockEntity.this.worldPosition);

        public VibrationUser() {
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
        public TagKey<GameEvent> getListenableEvents() {
            return GameEventTags.SHRIEKER_CAN_LISTEN;
        }

        @Override
        public boolean canReceiveVibration(ServerLevel p_281256_, BlockPos p_281528_, Holder<GameEvent> p_335342_, GameEvent.Context p_282914_) {
            return !SculkShriekerBlockEntity.this.getBlockState().getValue(SculkShriekerBlock.SHRIEKING)
                && SculkShriekerBlockEntity.tryGetPlayer(p_282914_.sourceEntity()) != null;
        }

        @Override
        public void onReceiveVibration(
            ServerLevel p_283372_, BlockPos p_281679_, Holder<GameEvent> p_330622_, @Nullable Entity p_282286_, @Nullable Entity p_281384_, float p_283119_
        ) {
            SculkShriekerBlockEntity.this.tryShriek(p_283372_, SculkShriekerBlockEntity.tryGetPlayer(p_281384_ != null ? p_281384_ : p_282286_));
        }

        @Override
        public void onDataChanged() {
            SculkShriekerBlockEntity.this.setChanged();
        }

        @Override
        public boolean requiresAdjacentChunksToBeTicking() {
            return true;
        }
    }
}