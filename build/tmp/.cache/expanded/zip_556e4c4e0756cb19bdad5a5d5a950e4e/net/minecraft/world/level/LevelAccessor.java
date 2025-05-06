package net.minecraft.world.level;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.NeighborUpdater;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;

public interface LevelAccessor extends CommonLevelAccessor, LevelTimeAccess, ScheduledTickAccess {
    @Override
    default long dayTime() {
        return this.getLevelData().getDayTime();
    }

    long nextSubTickCount();

    @Override
    default <T> ScheduledTick<T> createTick(BlockPos pPos, T pType, int pDelay, TickPriority pPriority) {
        return new ScheduledTick<>(pType, pPos, this.getLevelData().getGameTime() + pDelay, pPriority, this.nextSubTickCount());
    }

    @Override
    default <T> ScheduledTick<T> createTick(BlockPos pPos, T pType, int pDelay) {
        return new ScheduledTick<>(pType, pPos, this.getLevelData().getGameTime() + pDelay, this.nextSubTickCount());
    }

    LevelData getLevelData();

    DifficultyInstance getCurrentDifficultyAt(BlockPos pPos);

    @Nullable
    MinecraftServer getServer();

    default Difficulty getDifficulty() {
        return this.getLevelData().getDifficulty();
    }

    ChunkSource getChunkSource();

    @Override
    default boolean hasChunk(int pChunkX, int pChunkZ) {
        return this.getChunkSource().hasChunk(pChunkX, pChunkZ);
    }

    RandomSource getRandom();

    default void updateNeighborsAt(BlockPos pPos, Block pBlock) {
    }

    default void neighborShapeChanged(Direction pDirection, BlockPos pPos, BlockPos pNeighborPos, BlockState pNeighborState, int pFlags, int pRecursionLeft) {
        NeighborUpdater.executeShapeUpdate(this, pDirection, pPos, pNeighborPos, pNeighborState, pFlags, pRecursionLeft - 1);
    }

    default void playSound(@Nullable Entity pEntity, BlockPos pPos, SoundEvent pSound, SoundSource pSource) {
        this.playSound(pEntity, pPos, pSound, pSource, 1.0F, 1.0F);
    }

    void playSound(@Nullable Entity pEntity, BlockPos pPos, SoundEvent pSound, SoundSource pSource, float pVolume, float pPitch);

    void addParticle(ParticleOptions pParticle, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed);

    void levelEvent(@Nullable Entity pEntity, int pType, BlockPos pPos, int pData);

    default void levelEvent(int pType, BlockPos pPos, int pData) {
        this.levelEvent(null, pType, pPos, pData);
    }

    void gameEvent(Holder<GameEvent> pGameEvent, Vec3 pPos, GameEvent.Context pContext);

    default void gameEvent(@Nullable Entity pEntity, Holder<GameEvent> pGameEvent, Vec3 pPos) {
        this.gameEvent(pGameEvent, pPos, new GameEvent.Context(pEntity, null));
    }

    default void gameEvent(@Nullable Entity pEntity, Holder<GameEvent> pGameEvent, BlockPos pPos) {
        this.gameEvent(pGameEvent, pPos, new GameEvent.Context(pEntity, null));
    }

    default void gameEvent(Holder<GameEvent> pGameEvent, BlockPos pPos, GameEvent.Context pContext) {
        this.gameEvent(pGameEvent, Vec3.atCenterOf(pPos), pContext);
    }

    default void gameEvent(ResourceKey<GameEvent> pGameEvent, BlockPos pPos, GameEvent.Context pContext) {
        this.gameEvent(this.registryAccess().lookupOrThrow(Registries.GAME_EVENT).getOrThrow(pGameEvent), pPos, pContext);
    }
}