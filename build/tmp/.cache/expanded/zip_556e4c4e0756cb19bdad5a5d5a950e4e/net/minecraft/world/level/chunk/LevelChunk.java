package net.minecraft.world.level.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gameevent.EuclideanGameEventListenerRegistry;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.TickContainerAccess;
import org.slf4j.Logger;

public class LevelChunk extends ChunkAccess implements net.minecraftforge.common.capabilities.ICapabilityProviderImpl<LevelChunk> {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final TickingBlockEntity NULL_TICKER = new TickingBlockEntity() {
        @Override
        public void tick() {
        }

        @Override
        public boolean isRemoved() {
            return true;
        }

        @Override
        public BlockPos getPos() {
            return BlockPos.ZERO;
        }

        @Override
        public String getType() {
            return "<null>";
        }
    };
    private final Map<BlockPos, LevelChunk.RebindableTickingBlockEntityWrapper> tickersInLevel = Maps.newHashMap();
    private boolean loaded;
    final Level level;
    @Nullable
    private Supplier<FullChunkStatus> fullStatus;
    @Nullable
    private LevelChunk.PostLoadProcessor postLoad;
    private final Int2ObjectMap<GameEventListenerRegistry> gameEventListenerRegistrySections;
    private final LevelChunkTicks<Block> blockTicks;
    private final LevelChunkTicks<Fluid> fluidTicks;
    private LevelChunk.UnsavedListener unsavedListener = p_360556_ -> {};

    public LevelChunk(Level pLevel, ChunkPos pPos) {
        this(pLevel, pPos, UpgradeData.EMPTY, new LevelChunkTicks<>(), new LevelChunkTicks<>(), 0L, null, null, null);
    }

    public LevelChunk(
        Level pLevel,
        ChunkPos pPos,
        UpgradeData pData,
        LevelChunkTicks<Block> pBlockTicks,
        LevelChunkTicks<Fluid> pFluidTicks,
        long pInhabitedTime,
        @Nullable LevelChunkSection[] pSections,
        @Nullable LevelChunk.PostLoadProcessor pPostLoad,
        @Nullable BlendingData pBlendingData
    ) {
        super(pPos, pData, pLevel, pLevel.registryAccess().lookupOrThrow(Registries.BIOME), pInhabitedTime, pSections, pBlendingData);
        this.level = pLevel;
        this.gameEventListenerRegistrySections = new Int2ObjectOpenHashMap<>();

        for (Heightmap.Types heightmap$types : Heightmap.Types.values()) {
            if (ChunkStatus.FULL.heightmapsAfter().contains(heightmap$types)) {
                this.heightmaps.put(heightmap$types, new Heightmap(this, heightmap$types));
            }
        }

        this.postLoad = pPostLoad;
        this.blockTicks = pBlockTicks;
        this.fluidTicks = pFluidTicks;
        this.capProvider.initInternal();
    }

    public LevelChunk(ServerLevel pLevel, ProtoChunk pChunk, @Nullable LevelChunk.PostLoadProcessor pPostLoad) {
        this(
            pLevel,
            pChunk.getPos(),
            pChunk.getUpgradeData(),
            pChunk.unpackBlockTicks(),
            pChunk.unpackFluidTicks(),
            pChunk.getInhabitedTime(),
            pChunk.getSections(),
            pPostLoad,
            pChunk.getBlendingData()
        );
        if (!Collections.disjoint(pChunk.pendingBlockEntities.keySet(), pChunk.blockEntities.keySet())) {
            LOGGER.error("Chunk at {} contains duplicated block entities", pChunk.getPos());
        }

        for (BlockEntity blockentity : pChunk.getBlockEntities().values()) {
            this.setBlockEntity(blockentity);
        }

        this.pendingBlockEntities.putAll(pChunk.getBlockEntityNbts());

        for (int i = 0; i < pChunk.getPostProcessing().length; i++) {
            this.postProcessing[i] = pChunk.getPostProcessing()[i];
        }

        this.setAllStarts(pChunk.getAllStarts());
        this.setAllReferences(pChunk.getAllReferences());

        for (Entry<Heightmap.Types, Heightmap> entry : pChunk.getHeightmaps()) {
            if (ChunkStatus.FULL.heightmapsAfter().contains(entry.getKey())) {
                this.setHeightmap(entry.getKey(), entry.getValue().getRawData());
            }
        }

        this.skyLightSources = pChunk.skyLightSources;
        this.setLightCorrect(pChunk.isLightCorrect());
        this.markUnsaved();
    }

    public void setUnsavedListener(LevelChunk.UnsavedListener pUnsavedListener) {
        this.unsavedListener = pUnsavedListener;
        if (this.isUnsaved()) {
            pUnsavedListener.setUnsaved(this.chunkPos);
        }
    }

    @Override
    public void markUnsaved() {
        boolean flag = this.isUnsaved();
        super.markUnsaved();
        if (!flag) {
            this.unsavedListener.setUnsaved(this.chunkPos);
        }
    }

    @Override
    public TickContainerAccess<Block> getBlockTicks() {
        return this.blockTicks;
    }

    @Override
    public TickContainerAccess<Fluid> getFluidTicks() {
        return this.fluidTicks;
    }

    @Override
    public ChunkAccess.PackedTicks getTicksForSerialization(long p_361110_) {
        return new ChunkAccess.PackedTicks(this.blockTicks.pack(p_361110_), this.fluidTicks.pack(p_361110_));
    }

    @Override
    public GameEventListenerRegistry getListenerRegistry(int p_251193_) {
        return this.level instanceof ServerLevel serverlevel
            ? this.gameEventListenerRegistrySections.computeIfAbsent(p_251193_, p_281221_ -> new EuclideanGameEventListenerRegistry(serverlevel, p_251193_, this::removeGameEventListenerRegistry))
            : super.getListenerRegistry(p_251193_);
    }

    @Override
    public BlockState getBlockState(BlockPos p_62923_) {
        int i = p_62923_.getX();
        int j = p_62923_.getY();
        int k = p_62923_.getZ();
        if (this.level.isDebug()) {
            BlockState blockstate = null;
            if (j == 60) {
                blockstate = Blocks.BARRIER.defaultBlockState();
            }

            if (j == 70) {
                blockstate = DebugLevelSource.getBlockStateFor(i, k);
            }

            return blockstate == null ? Blocks.AIR.defaultBlockState() : blockstate;
        } else {
            try {
                int l = this.getSectionIndex(j);
                if (l >= 0 && l < this.sections.length) {
                    LevelChunkSection levelchunksection = this.sections[l];
                    if (!levelchunksection.hasOnlyAir()) {
                        return levelchunksection.getBlockState(i & 15, j & 15, k & 15);
                    }
                }

                return Blocks.AIR.defaultBlockState();
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting block state");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Block being got");
                crashreportcategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this, i, j, k));
                throw new ReportedException(crashreport);
            }
        }
    }

    @Override
    public FluidState getFluidState(BlockPos p_62895_) {
        return this.getFluidState(p_62895_.getX(), p_62895_.getY(), p_62895_.getZ());
    }

    public FluidState getFluidState(int pX, int pY, int pZ) {
        try {
            int i = this.getSectionIndex(pY);
            if (i >= 0 && i < this.sections.length) {
                LevelChunkSection levelchunksection = this.sections[i];
                if (!levelchunksection.hasOnlyAir()) {
                    return levelchunksection.getFluidState(pX & 15, pY & 15, pZ & 15);
                }
            }

            return Fluids.EMPTY.defaultFluidState();
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting fluid state");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being got");
            crashreportcategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this, pX, pY, pZ));
            throw new ReportedException(crashreport);
        }
    }

    @Nullable
    @Override
    public BlockState setBlockState(BlockPos p_62865_, BlockState p_62866_, int p_393998_) {
        int i = p_62865_.getY();
        LevelChunkSection levelchunksection = this.getSection(this.getSectionIndex(i));
        boolean flag = levelchunksection.hasOnlyAir();
        if (flag && p_62866_.isAir()) {
            return null;
        } else {
            int j = p_62865_.getX() & 15;
            int k = i & 15;
            int l = p_62865_.getZ() & 15;
            BlockState blockstate = levelchunksection.setBlockState(j, k, l, p_62866_);
            if (blockstate == p_62866_) {
                return null;
            } else {
                Block block = p_62866_.getBlock();
                this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING).update(j, i, l, p_62866_);
                this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES).update(j, i, l, p_62866_);
                this.heightmaps.get(Heightmap.Types.OCEAN_FLOOR).update(j, i, l, p_62866_);
                this.heightmaps.get(Heightmap.Types.WORLD_SURFACE).update(j, i, l, p_62866_);
                boolean flag1 = levelchunksection.hasOnlyAir();
                if (flag != flag1) {
                    this.level.getChunkSource().getLightEngine().updateSectionStatus(p_62865_, flag1);
                    this.level.getChunkSource().onSectionEmptinessChanged(this.chunkPos.x, SectionPos.blockToSectionCoord(i), this.chunkPos.z, flag1);
                }

                if (LightEngine.hasDifferentLightProperties(blockstate, p_62866_)) {
                    ProfilerFiller profilerfiller = Profiler.get();
                    profilerfiller.push("updateSkyLightSources");
                    this.skyLightSources.update(this, j, i, l);
                    profilerfiller.popPush("queueCheckLight");
                    this.level.getChunkSource().getLightEngine().checkBlock(p_62865_);
                    profilerfiller.pop();
                }

                boolean flag4 = !blockstate.is(block) || !p_62866_.hasBlockEntity();
                boolean flag2 = (p_393998_ & 64) != 0;
                boolean flag3 = (p_393998_ & 256) == 0;
                if (flag4 && blockstate.hasBlockEntity()) {
                    if (!this.level.isClientSide && flag3) {
                        BlockEntity blockentity = this.level.getBlockEntity(p_62865_);
                        if (blockentity != null) {
                            blockentity.preRemoveSideEffects(p_62865_, blockstate);
                        }
                    }

                    this.removeBlockEntity(p_62865_);
                }

                if ((flag4 || block instanceof BaseRailBlock) && this.level instanceof ServerLevel serverlevel && ((p_393998_ & 1) != 0 || flag2)) {
                    blockstate.affectNeighborsAfterRemoval(serverlevel, p_62865_, flag2);
                }

                if (!levelchunksection.getBlockState(j, k, l).is(block)) {
                    return null;
                } else {
                    if (!this.level.isClientSide && (p_393998_ & 512) == 0 && !this.level.captureBlockSnapshots) {
                        p_62866_.onPlace(this.level, p_62865_, blockstate, flag2);
                    }

                    if (p_62866_.hasBlockEntity()) {
                        BlockEntity blockentity1 = this.getBlockEntity(p_62865_, LevelChunk.EntityCreationType.CHECK);
                        if (blockentity1 != null && !blockentity1.isValidBlockState(p_62866_)) {
                            LOGGER.warn(
                                "Found mismatched block entity @ {}: type = {}, state = {}",
                                p_62865_,
                                blockentity1.getType().builtInRegistryHolder().key().location(),
                                p_62866_
                            );
                            this.removeBlockEntity(p_62865_);
                            blockentity1 = null;
                        }

                        if (blockentity1 == null) {
                            blockentity1 = ((EntityBlock)block).newBlockEntity(p_62865_, p_62866_);
                            if (blockentity1 != null) {
                                this.addAndRegisterBlockEntity(blockentity1);
                            }
                        } else {
                            blockentity1.setBlockState(p_62866_);
                            this.updateBlockEntityTicker(blockentity1);
                        }
                    }

                    this.markUnsaved();
                    return blockstate;
                }
            }
        }
    }

    @Deprecated
    @Override
    public void addEntity(Entity p_62826_) {
    }

    @Nullable
    private BlockEntity createBlockEntity(BlockPos pPos) {
        BlockState blockstate = this.getBlockState(pPos);
        return !blockstate.hasBlockEntity() ? null : ((EntityBlock)blockstate.getBlock()).newBlockEntity(pPos, blockstate);
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos p_62912_) {
        return this.getBlockEntity(p_62912_, LevelChunk.EntityCreationType.CHECK);
    }

    @Nullable
    public BlockEntity getBlockEntity(BlockPos pPos, LevelChunk.EntityCreationType pCreationType) {
        BlockEntity blockentity = this.blockEntities.get(pPos);

        if (blockentity != null && blockentity.isRemoved()) {
           blockEntities.remove(pPos);
           blockentity = null;
        }

        if (blockentity == null) {
            CompoundTag compoundtag = this.pendingBlockEntities.remove(pPos);
            if (compoundtag != null) {
                BlockEntity blockentity1 = this.promotePendingBlockEntity(pPos, compoundtag);
                if (blockentity1 != null) {
                    return blockentity1;
                }
            }
        }

        if (blockentity == null) {
            if (pCreationType == LevelChunk.EntityCreationType.IMMEDIATE) {
                blockentity = this.createBlockEntity(pPos);
                if (blockentity != null) {
                    this.addAndRegisterBlockEntity(blockentity);
                }
            }
        }

        return blockentity;
    }

    public void addAndRegisterBlockEntity(BlockEntity pBlockEntity) {
        this.setBlockEntity(pBlockEntity);
        if (this.isInLevel()) {
            if (this.level instanceof ServerLevel serverlevel) {
                this.addGameEventListener(pBlockEntity, serverlevel);
            }

            this.updateBlockEntityTicker(pBlockEntity);
            pBlockEntity.onLoad();
        }
    }

    private boolean isInLevel() {
        return this.loaded || this.level.isClientSide();
    }

    boolean isTicking(BlockPos pPos) {
        if (!this.level.getWorldBorder().isWithinBounds(pPos)) {
            return false;
        } else {
            return !(this.level instanceof ServerLevel serverlevel)
                ? true
                : this.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING) && serverlevel.areEntitiesLoaded(ChunkPos.asLong(pPos));
        }
    }

    @Override
    public void setBlockEntity(BlockEntity p_156374_) {
        BlockPos blockpos = p_156374_.getBlockPos();
        BlockState blockstate = this.getBlockState(blockpos);
        if (!blockstate.hasBlockEntity()) {
            LOGGER.warn("Trying to set block entity {} at position {}, but state {} does not allow it", p_156374_, blockpos, blockstate);
        } else {
            BlockState blockstate1 = p_156374_.getBlockState();
            if (blockstate != blockstate1) {
                if (!p_156374_.getType().isValid(blockstate)) {
                    LOGGER.warn("Trying to set block entity {} at position {}, but state {} does not allow it", p_156374_, blockpos, blockstate);
                    return;
                }

                if (blockstate.getBlock() != blockstate1.getBlock()) {
                    LOGGER.warn("Block state mismatch on block entity {} in position {}, {} != {}, updating", p_156374_, blockpos, blockstate, blockstate1);
                }

                p_156374_.setBlockState(blockstate);
            }

            p_156374_.setLevel(this.level);
            p_156374_.clearRemoved();
            BlockEntity blockentity = this.blockEntities.put(blockpos.immutable(), p_156374_);
            if (blockentity != null && blockentity != p_156374_) {
                blockentity.setRemoved();
            }
        }
    }

    @Nullable
    @Override
    public CompoundTag getBlockEntityNbtForSaving(BlockPos p_62932_, HolderLookup.Provider p_329605_) {
        BlockEntity blockentity = this.getBlockEntity(p_62932_);
        if (blockentity != null && !blockentity.isRemoved()) {
            try {
            CompoundTag compoundtag1 = blockentity.saveWithFullMetadata(this.level.registryAccess());
            compoundtag1.putBoolean("keepPacked", false);
            return compoundtag1;
            } catch (Exception e) {
                LOGGER.error("A BlockEntity type {} has thrown an exception trying to write state. It will not persist, Report this to the mod author", blockentity.getClass().getName(), e);
                return null;
            }
        } else {
            CompoundTag compoundtag = this.pendingBlockEntities.get(p_62932_);
            if (compoundtag != null) {
                compoundtag = compoundtag.copy();
                compoundtag.putBoolean("keepPacked", true);
            }

            return compoundtag;
        }
    }

    @Override
    public void removeBlockEntity(BlockPos p_62919_) {
        if (this.isInLevel()) {
            BlockEntity blockentity = this.blockEntities.remove(p_62919_);
            if (blockentity != null) {
                if (this.level instanceof ServerLevel serverlevel) {
                    this.removeGameEventListener(blockentity, serverlevel);
                }

                blockentity.setRemoved();
            }
        }

        this.removeBlockEntityTicker(p_62919_);
    }

    private <T extends BlockEntity> void removeGameEventListener(T pBlockEntity, ServerLevel pLevel) {
        Block block = pBlockEntity.getBlockState().getBlock();
        if (block instanceof EntityBlock) {
            GameEventListener gameeventlistener = ((EntityBlock)block).getListener(pLevel, pBlockEntity);
            if (gameeventlistener != null) {
                int i = SectionPos.blockToSectionCoord(pBlockEntity.getBlockPos().getY());
                GameEventListenerRegistry gameeventlistenerregistry = this.getListenerRegistry(i);
                gameeventlistenerregistry.unregister(gameeventlistener);
            }
        }
    }

    private void removeGameEventListenerRegistry(int pSectionY) {
        this.gameEventListenerRegistrySections.remove(pSectionY);
    }

    private void removeBlockEntityTicker(BlockPos pPos) {
        LevelChunk.RebindableTickingBlockEntityWrapper levelchunk$rebindabletickingblockentitywrapper = this.tickersInLevel.remove(pPos);
        if (levelchunk$rebindabletickingblockentitywrapper != null) {
            levelchunk$rebindabletickingblockentitywrapper.rebind(NULL_TICKER);
        }
    }

    public void runPostLoad() {
        if (this.postLoad != null) {
            this.postLoad.run(this);
            this.postLoad = null;
        }
    }

    public boolean isEmpty() {
        return false;
    }

    public void replaceWithPacketData(
        FriendlyByteBuf pBuffer, Map<Heightmap.Types, long[]> pHeightmaps, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> pOutputTagConsumer
    ) {
        this.clearAllBlockEntities();

        for (LevelChunkSection levelchunksection : this.sections) {
            levelchunksection.read(pBuffer);
        }

        pHeightmaps.forEach(this::setHeightmap);
        this.initializeLightSources();
        pOutputTagConsumer.accept((p_327409_, p_327410_, p_327411_) -> {
            BlockEntity blockentity = this.getBlockEntity(p_327409_, LevelChunk.EntityCreationType.IMMEDIATE);
            if (blockentity != null && p_327411_ != null && blockentity.getType() == p_327410_) {
                blockentity.handleUpdateTag(p_327411_, this.level.registryAccess());
            }
        });
    }

    public void replaceBiomes(FriendlyByteBuf pBuffer) {
        for (LevelChunkSection levelchunksection : this.sections) {
            levelchunksection.readBiomes(pBuffer);
        }
    }

    public void setLoaded(boolean pLoaded) {
        this.loaded = pLoaded;
    }

    public Level getLevel() {
        return this.level;
    }

    public Map<BlockPos, BlockEntity> getBlockEntities() {
        return this.blockEntities;
    }

    public void postProcessGeneration(ServerLevel pLevel) {
        ChunkPos chunkpos = this.getPos();

        for (int i = 0; i < this.postProcessing.length; i++) {
            if (this.postProcessing[i] != null) {
                for (Short oshort : this.postProcessing[i]) {
                    BlockPos blockpos = ProtoChunk.unpackOffsetCoordinates(oshort, this.getSectionYFromSectionIndex(i), chunkpos);
                    BlockState blockstate = this.getBlockState(blockpos);
                    FluidState fluidstate = blockstate.getFluidState();
                    if (!fluidstate.isEmpty()) {
                        fluidstate.tick(pLevel, blockpos, blockstate);
                    }

                    if (!(blockstate.getBlock() instanceof LiquidBlock)) {
                        BlockState blockstate1 = Block.updateFromNeighbourShapes(blockstate, pLevel, blockpos);
                        if (blockstate1 != blockstate) {
                            pLevel.setBlock(blockpos, blockstate1, 276);
                        }
                    }
                }

                this.postProcessing[i].clear();
            }
        }

        for (BlockPos blockpos1 : ImmutableList.copyOf(this.pendingBlockEntities.keySet())) {
            this.getBlockEntity(blockpos1);
        }

        this.pendingBlockEntities.clear();
        this.upgradeData.upgrade(this);
    }

    @Nullable
    private BlockEntity promotePendingBlockEntity(BlockPos pPos, CompoundTag pTag) {
        BlockState blockstate = this.getBlockState(pPos);
        BlockEntity blockentity;
        if ("DUMMY".equals(pTag.getStringOr("id", ""))) {
            if (blockstate.hasBlockEntity()) {
                blockentity = ((EntityBlock)blockstate.getBlock()).newBlockEntity(pPos, blockstate);
            } else {
                blockentity = null;
                LOGGER.warn("Tried to load a DUMMY block entity @ {} but found not block entity block {} at location", pPos, blockstate);
            }
        } else {
            blockentity = BlockEntity.loadStatic(pPos, blockstate, pTag, this.level.registryAccess());
        }

        if (blockentity != null) {
            blockentity.setLevel(this.level);
            this.addAndRegisterBlockEntity(blockentity);
        } else {
            LOGGER.warn("Tried to load a block entity for block {} but failed at location {}", blockstate, pPos);
        }

        return blockentity;
    }

    public void unpackTicks(long pPos) {
        this.blockTicks.unpack(pPos);
        this.fluidTicks.unpack(pPos);
    }

    public void registerTickContainerInLevel(ServerLevel pLevel) {
        pLevel.getBlockTicks().addContainer(this.chunkPos, this.blockTicks);
        pLevel.getFluidTicks().addContainer(this.chunkPos, this.fluidTicks);
    }

    public void unregisterTickContainerFromLevel(ServerLevel pLevel) {
        pLevel.getBlockTicks().removeContainer(this.chunkPos);
        pLevel.getFluidTicks().removeContainer(this.chunkPos);
    }

    @Override
    public ChunkStatus getPersistedStatus() {
        return ChunkStatus.FULL;
    }

    public FullChunkStatus getFullStatus() {
        return this.fullStatus == null ? FullChunkStatus.FULL : this.fullStatus.get();
    }

    public void setFullStatus(Supplier<FullChunkStatus> pFullStatus) {
        this.fullStatus = pFullStatus;
    }

    public void clearAllBlockEntities() {
        this.blockEntities.values().forEach(BlockEntity::onChunkUnloaded);
        this.blockEntities.values().forEach(BlockEntity::setRemoved);
        this.blockEntities.clear();
        this.tickersInLevel.values().forEach(p_187966_ -> p_187966_.rebind(NULL_TICKER));
        this.tickersInLevel.clear();
    }

    public void registerAllBlockEntitiesAfterLevelLoad() {
        this.level.addFreshBlockEntities(this.blockEntities.values());
        this.blockEntities.values().forEach(p_187988_ -> {
            if (this.level instanceof ServerLevel serverlevel) {
                this.addGameEventListener(p_187988_, serverlevel);
            }

            this.updateBlockEntityTicker(p_187988_);
        });
    }

    private <T extends BlockEntity> void addGameEventListener(T pBlockEntity, ServerLevel pLevel) {
        Block block = pBlockEntity.getBlockState().getBlock();
        if (block instanceof EntityBlock) {
            GameEventListener gameeventlistener = ((EntityBlock)block).getListener(pLevel, pBlockEntity);
            if (gameeventlistener != null) {
                this.getListenerRegistry(SectionPos.blockToSectionCoord(pBlockEntity.getBlockPos().getY())).register(gameeventlistener);
            }
        }
    }

    private <T extends BlockEntity> void updateBlockEntityTicker(T pBlockEntity) {
        BlockState blockstate = pBlockEntity.getBlockState();
        BlockEntityTicker<T> blockentityticker = blockstate.getTicker(this.level, (BlockEntityType<T>)pBlockEntity.getType());
        if (blockentityticker == null) {
            this.removeBlockEntityTicker(pBlockEntity.getBlockPos());
        } else {
            this.tickersInLevel
                .compute(
                    pBlockEntity.getBlockPos(),
                    (p_375350_, p_375351_) -> {
                        TickingBlockEntity tickingblockentity = this.createTicker(pBlockEntity, blockentityticker);
                        if (p_375351_ != null) {
                            p_375351_.rebind(tickingblockentity);
                            return (LevelChunk.RebindableTickingBlockEntityWrapper)p_375351_;
                        } else if (this.isInLevel()) {
                            LevelChunk.RebindableTickingBlockEntityWrapper levelchunk$rebindabletickingblockentitywrapper = new LevelChunk.RebindableTickingBlockEntityWrapper(
                                tickingblockentity
                            );
                            this.level.addBlockEntityTicker(levelchunk$rebindabletickingblockentitywrapper);
                            return levelchunk$rebindabletickingblockentitywrapper;
                        } else {
                            return null;
                        }
                    }
                );
        }
    }

    private <T extends BlockEntity> TickingBlockEntity createTicker(T pBlockEntity, BlockEntityTicker<T> pTicker) {
        return new LevelChunk.BoundTickingBlockEntity<>(pBlockEntity, pTicker);
    }

    private final net.minecraftforge.common.capabilities.CapabilityProvider.AsField<LevelChunk> capProvider = new net.minecraftforge.common.capabilities.CapabilityProvider.AsField<>(LevelChunk.class, this);

    @org.jetbrains.annotations.NotNull
    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@org.jetbrains.annotations.NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
       return capProvider.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        capProvider.invalidateCaps();
    }

    @Override
    public void reviveCaps() {
        capProvider.reviveCaps();
    }

    class BoundTickingBlockEntity<T extends BlockEntity> implements TickingBlockEntity {
        private final T blockEntity;
        private final BlockEntityTicker<T> ticker;
        private boolean loggedInvalidBlockState;

        BoundTickingBlockEntity(final T pBlockEntity, final BlockEntityTicker<T> pTicker) {
            this.blockEntity = pBlockEntity;
            this.ticker = pTicker;
        }

        @Override
        public void tick() {
            if (!this.blockEntity.isRemoved() && this.blockEntity.hasLevel()) {
                BlockPos blockpos = this.blockEntity.getBlockPos();
                if (LevelChunk.this.isTicking(blockpos)) {
                    try {
                        ProfilerFiller profilerfiller = Profiler.get();
                        net.minecraftforge.server.timings.TimeTracker.BLOCK_ENTITY_UPDATE.trackStart(blockEntity);
                        profilerfiller.push(this::getType);
                        BlockState blockstate = LevelChunk.this.getBlockState(blockpos);
                        if (this.blockEntity.getType().isValid(blockstate)) {
                            this.ticker.tick(LevelChunk.this.level, this.blockEntity.getBlockPos(), blockstate, this.blockEntity);
                            this.loggedInvalidBlockState = false;
                        } else if (!this.loggedInvalidBlockState) {
                            this.loggedInvalidBlockState = true;
                            LevelChunk.LOGGER
                                .warn(
                                    "Block entity {} @ {} state {} invalid for ticking:",
                                    LogUtils.defer(this::getType),
                                    LogUtils.defer(this::getPos),
                                    blockstate
                                );
                        }

                        profilerfiller.pop();
                    } catch (Throwable throwable) {
                        CrashReport crashreport = CrashReport.forThrowable(throwable, "Ticking block entity");
                        CrashReportCategory crashreportcategory = crashreport.addCategory("Block entity being ticked");
                        this.blockEntity.fillCrashReportCategory(crashreportcategory);
                        if (net.minecraftforge.common.ForgeConfig.SERVER.removeErroringBlockEntities.get()) {
                            LOGGER.error("{}", crashreport.getFriendlyReport(net.minecraft.ReportType.CRASH));
                            blockEntity.setRemoved();
                            LevelChunk.this.removeBlockEntity(blockEntity.getBlockPos());
                        } else
                        throw new ReportedException(crashreport);
                    }
                }
            }
        }

        @Override
        public boolean isRemoved() {
            return this.blockEntity.isRemoved();
        }

        @Override
        public BlockPos getPos() {
            return this.blockEntity.getBlockPos();
        }

        @Override
        public String getType() {
            return BlockEntityType.getKey(this.blockEntity.getType()).toString();
        }

        @Override
        public String toString() {
            return "Level ticker for " + this.getType() + "@" + this.getPos();
        }
    }

    public static enum EntityCreationType {
        IMMEDIATE,
        QUEUED,
        CHECK;
    }

    @FunctionalInterface
    public interface PostLoadProcessor {
        void run(LevelChunk pChunk);
    }

    static class RebindableTickingBlockEntityWrapper implements TickingBlockEntity {
        private TickingBlockEntity ticker;

        RebindableTickingBlockEntityWrapper(TickingBlockEntity pTicker) {
            this.ticker = pTicker;
        }

        void rebind(TickingBlockEntity pTicker) {
            this.ticker = pTicker;
        }

        @Override
        public void tick() {
            this.ticker.tick();
        }

        @Override
        public boolean isRemoved() {
            return this.ticker.isRemoved();
        }

        @Override
        public BlockPos getPos() {
            return this.ticker.getPos();
        }

        @Override
        public String getType() {
            return this.ticker.getType();
        }

        @Override
        public String toString() {
            return this.ticker + " <wrapped>";
        }
    }

    /**
     * <strong>FOR INTERNAL USE ONLY</strong>
     * <p>
     * Only public for use in {@link net.minecraft.world.level.chunk.storage.ChunkSerializer}.
     */
    @java.lang.Deprecated
    @org.jetbrains.annotations.Nullable
    public final CompoundTag writeCapsToNBT(HolderLookup.Provider registryAccess) {
       return capProvider.serializeInternal(registryAccess);
    }

    /**
     * <strong>FOR INTERNAL USE ONLY</strong>
     * <p>
     * Only public for use in {@link net.minecraft.world.level.chunk.storage.ChunkSerializer}.
     *
     */
    @java.lang.Deprecated
    public final void readCapsFromNBT(HolderLookup.Provider registryAccess, CompoundTag tag) {
        capProvider.deserializeInternal(registryAccess, tag);
    }

    @Override
    public Level getWorldForge() {
        return getLevel();
    }

    @FunctionalInterface
    public interface UnsavedListener {
        void setUnsaved(ChunkPos pChunkPos);
    }
}
