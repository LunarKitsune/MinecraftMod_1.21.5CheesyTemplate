package net.minecraft.client.multiplayer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.Difficulty;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientLevel extends Level implements CacheSlot.Cleaner<ClientLevel> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double FLUID_PARTICLE_SPAWN_OFFSET = 0.05;
    private static final int NORMAL_LIGHT_UPDATES_PER_FRAME = 10;
    private static final int LIGHT_UPDATE_QUEUE_SIZE_THRESHOLD = 1000;
    final EntityTickList tickingEntities = new EntityTickList();
    private final TransientEntitySectionManager<Entity> entityStorage = new TransientEntitySectionManager<>(Entity.class, new ClientLevel.EntityCallbacks());
    private final ClientPacketListener connection;
    private final LevelRenderer levelRenderer;
    private final LevelEventHandler levelEventHandler;
    private final ClientLevel.ClientLevelData clientLevelData;
    private final DimensionSpecialEffects effects;
    private final TickRateManager tickRateManager;
    private final Minecraft minecraft = Minecraft.getInstance();
    final List<AbstractClientPlayer> players = Lists.newArrayList();
    final List<EnderDragonPart> dragonParts = Lists.newArrayList();
    private final Map<MapId, MapItemSavedData> mapData = Maps.newHashMap();
    private static final int CLOUD_COLOR = -1;
    private int skyFlashTime;
    private final Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches = Util.make(new Object2ObjectArrayMap<>(3), p_389331_ -> {
        p_389331_.put(BiomeColors.GRASS_COLOR_RESOLVER, new BlockTintCache(p_194181_ -> this.calculateBlockTint(p_194181_, BiomeColors.GRASS_COLOR_RESOLVER)));
        p_389331_.put(BiomeColors.FOLIAGE_COLOR_RESOLVER, new BlockTintCache(p_194177_ -> this.calculateBlockTint(p_194177_, BiomeColors.FOLIAGE_COLOR_RESOLVER)));
        p_389331_.put(BiomeColors.DRY_FOLIAGE_COLOR_RESOLVER, new BlockTintCache(p_389335_ -> this.calculateBlockTint(p_389335_, BiomeColors.DRY_FOLIAGE_COLOR_RESOLVER)));
        p_389331_.put(BiomeColors.WATER_COLOR_RESOLVER, new BlockTintCache(p_194168_ -> this.calculateBlockTint(p_194168_, BiomeColors.WATER_COLOR_RESOLVER)));
        net.minecraftforge.client.ColorResolverManager.registerBlockTintCaches(ClientLevel.this, p_389331_);
    });
    private final ClientChunkCache chunkSource;
    private final Deque<Runnable> lightUpdateQueue = Queues.newArrayDeque();
    private int serverSimulationDistance;
    private final BlockStatePredictionHandler blockStatePredictionHandler = new BlockStatePredictionHandler();
    private final int seaLevel;
    private boolean tickDayTime;
    private static final Set<Item> MARKER_PARTICLE_ITEMS = Set.of(Items.BARRIER, Items.LIGHT);
    private final it.unimi.dsi.fastutil.ints.Int2ObjectMap<net.minecraftforge.entity.PartEntity<?>> partEntities = new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();
    private final net.minecraftforge.client.model.data.ModelDataManager modelDataManager = new net.minecraftforge.client.model.data.ModelDataManager(this);

    public void handleBlockChangedAck(int pSequence) {
        this.blockStatePredictionHandler.endPredictionsUpTo(pSequence, this);
    }

    public void setServerVerifiedBlockState(BlockPos pPos, BlockState pState, int pFlags) {
        if (!this.blockStatePredictionHandler.updateKnownServerState(pPos, pState)) {
            super.setBlock(pPos, pState, pFlags, 512);
        }
    }

    public void syncBlockState(BlockPos pPos, BlockState pState, Vec3 pPlayerPos) {
        BlockState blockstate = this.getBlockState(pPos);
        if (blockstate != pState) {
            this.setBlock(pPos, pState, 19);
            Player player = this.minecraft.player;
            if (this == player.level() && player.isColliding(pPos, pState)) {
                player.absSnapTo(pPlayerPos.x, pPlayerPos.y, pPlayerPos.z);
            }
        }
    }

    BlockStatePredictionHandler getBlockStatePredictionHandler() {
        return this.blockStatePredictionHandler;
    }

    @Override
    public boolean setBlock(BlockPos p_233643_, BlockState p_233644_, int p_233645_, int p_233646_) {
        if (this.blockStatePredictionHandler.isPredicting()) {
            BlockState blockstate = this.getBlockState(p_233643_);
            boolean flag = super.setBlock(p_233643_, p_233644_, p_233645_, p_233646_);
            if (flag) {
                this.blockStatePredictionHandler.retainKnownServerState(p_233643_, blockstate, this.minecraft.player);
            }

            return flag;
        } else {
            return super.setBlock(p_233643_, p_233644_, p_233645_, p_233646_);
        }
    }

    public ClientLevel(
        ClientPacketListener pConnection,
        ClientLevel.ClientLevelData pLevelData,
        ResourceKey<Level> pDimension,
        Holder<DimensionType> pDimensionTypeRegistration,
        int pViewDistance,
        int pServerSimulationDistance,
        LevelRenderer pLevelRenderer,
        boolean pIsDebug,
        long pBiomeZoomSeed,
        int pSeaLevel
    ) {
        super(pLevelData, pDimension, pConnection.registryAccess(), pDimensionTypeRegistration, true, pIsDebug, pBiomeZoomSeed, 1000000);
        this.connection = pConnection;
        this.chunkSource = new ClientChunkCache(this, pViewDistance);
        this.tickRateManager = new TickRateManager();
        this.clientLevelData = pLevelData;
        this.levelRenderer = pLevelRenderer;
        this.seaLevel = pSeaLevel;
        this.levelEventHandler = new LevelEventHandler(this.minecraft, this, pLevelRenderer);
        this.effects = DimensionSpecialEffects.forType(pDimensionTypeRegistration.value());
        this.setDefaultSpawnPos(new BlockPos(8, 64, 8), 0.0F);
        this.serverSimulationDistance = pServerSimulationDistance;
        this.updateSkyBrightness();
        this.prepareWeather();
        this.gatherCapabilities();
        net.minecraftforge.event.ForgeEventFactory.onLevelLoad(this);
    }

    public void queueLightUpdate(Runnable pTask) {
        this.lightUpdateQueue.add(pTask);
    }

    public void pollLightUpdates() {
        int i = this.lightUpdateQueue.size();
        int j = i < 1000 ? Math.max(10, i / 10) : i;

        for (int k = 0; k < j; k++) {
            Runnable runnable = this.lightUpdateQueue.poll();
            if (runnable == null) {
                break;
            }

            runnable.run();
        }
    }

    public DimensionSpecialEffects effects() {
        return this.effects;
    }

    public void tick(BooleanSupplier pHasTimeLeft) {
        this.getWorldBorder().tick();
        this.updateSkyBrightness();
        if (this.tickRateManager().runsNormally()) {
            this.tickTime();
        }

        if (this.skyFlashTime > 0) {
            this.setSkyFlashTime(this.skyFlashTime - 1);
        }

        try (Zone zone = Profiler.get().zone("blocks")) {
            this.chunkSource.tick(pHasTimeLeft, true);
        }
    }

    private void tickTime() {
        this.clientLevelData.setGameTime(this.clientLevelData.getGameTime() + 1L);
        if (this.tickDayTime) {
            this.clientLevelData.setDayTime(this.clientLevelData.getDayTime() + 1L);
        }
    }

    public void setTimeFromServer(long pGameTime, long pDayTime, boolean pTickDayTime) {
        this.clientLevelData.setGameTime(pGameTime);
        this.clientLevelData.setDayTime(pDayTime);
        this.tickDayTime = pTickDayTime;
    }

    public Iterable<Entity> entitiesForRendering() {
        return this.getEntities().getAll();
    }

    public void tickEntities() {
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("entities");
        this.tickingEntities.forEach(p_308278_ -> {
            if (!p_308278_.isRemoved() && !p_308278_.isPassenger() && !this.tickRateManager.isEntityFrozen(p_308278_)) {
                this.guardEntityTick(this::tickNonPassenger, p_308278_);
            }
        });
        profilerfiller.pop();
        this.tickBlockEntities();
    }

    public boolean isTickingEntity(Entity pEntity) {
        return this.tickingEntities.contains(pEntity);
    }

    @Override
    public boolean shouldTickDeath(Entity p_194185_) {
        return p_194185_.chunkPosition().getChessboardDistance(this.minecraft.player.chunkPosition()) <= this.serverSimulationDistance;
    }

    public void tickNonPassenger(Entity pEntity) {
        pEntity.setOldPosAndRot();
        pEntity.tickCount++;
        Profiler.get().push(() -> BuiltInRegistries.ENTITY_TYPE.getKey(pEntity.getType()).toString());
        if (pEntity.canUpdate())
        pEntity.tick();
        Profiler.get().pop();

        for (Entity entity : pEntity.getPassengers()) {
            this.tickPassenger(pEntity, entity);
        }
    }

    private void tickPassenger(Entity pMount, Entity pRider) {
        if (pRider.isRemoved() || pRider.getVehicle() != pMount) {
            pRider.stopRiding();
        } else if (pRider instanceof Player || this.tickingEntities.contains(pRider)) {
            pRider.setOldPosAndRot();
            pRider.tickCount++;
            pRider.rideTick();

            for (Entity entity : pRider.getPassengers()) {
                this.tickPassenger(pRider, entity);
            }
        }
    }

    public void unload(LevelChunk pChunk) {
        pChunk.clearAllBlockEntities();
        this.chunkSource.getLightEngine().setLightEnabled(pChunk.getPos(), false);
        this.entityStorage.stopTicking(pChunk.getPos());
    }

    public void onChunkLoaded(ChunkPos pChunkPos) {
        this.tintCaches.forEach((p_194154_, p_194155_) -> p_194155_.invalidateForChunk(pChunkPos.x, pChunkPos.z));
        this.entityStorage.startTicking(pChunkPos);
    }

    public void onSectionBecomingNonEmpty(long pSectionPos) {
        this.levelRenderer.onSectionBecomingNonEmpty(pSectionPos);
    }

    public void clearTintCaches() {
        this.tintCaches.forEach((p_194157_, p_194158_) -> p_194158_.invalidateAll());
    }

    @Override
    public boolean hasChunk(int pChunkX, int pChunkZ) {
        return true;
    }

    public int getEntityCount() {
        return this.entityStorage.count();
    }

    public void addEntity(Entity pEntity) {
        if (net.minecraftforge.event.ForgeEventFactory.onEntityJoinLevel(pEntity, this)) return;
        this.removeEntity(pEntity.getId(), Entity.RemovalReason.DISCARDED);
        this.entityStorage.addEntity(pEntity);
        pEntity.onAddedToWorld();
    }

    public void removeEntity(int pEntityId, Entity.RemovalReason pReason) {
        Entity entity = this.getEntities().get(pEntityId);
        if (entity != null) {
            entity.setRemoved(pReason);
            entity.onClientRemoval();
        }
    }

    @Override
    public List<Entity> getPushableEntities(Entity p_395155_, AABB p_394378_) {
        LocalPlayer localplayer = this.minecraft.player;
        return localplayer != null
                && localplayer != p_395155_
                && localplayer.getBoundingBox().intersects(p_394378_)
                && EntitySelector.pushableBy(p_395155_).test(localplayer)
            ? List.of(localplayer)
            : List.of();
    }

    @Nullable
    @Override
    public Entity getEntity(int pId) {
        return this.getEntities().get(pId);
    }

    @Override
    public void disconnect() {
        this.connection.getConnection().disconnect(Component.translatable("multiplayer.status.quitting"));
    }

    public void animateTick(int pPosX, int pPosY, int pPosZ) {
        int i = 32;
        RandomSource randomsource = RandomSource.create();
        Block block = this.getMarkerParticleTarget();
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int j = 0; j < 667; j++) {
            this.doAnimateTick(pPosX, pPosY, pPosZ, 16, randomsource, block, blockpos$mutableblockpos);
            this.doAnimateTick(pPosX, pPosY, pPosZ, 32, randomsource, block, blockpos$mutableblockpos);
        }
    }

    @Nullable
    private Block getMarkerParticleTarget() {
        if (this.minecraft.gameMode.getPlayerMode() == GameType.CREATIVE) {
            ItemStack itemstack = this.minecraft.player.getMainHandItem();
            Item item = itemstack.getItem();
            if (MARKER_PARTICLE_ITEMS.contains(item) && item instanceof BlockItem blockitem) {
                return blockitem.getBlock();
            }
        }

        return null;
    }

    public void doAnimateTick(
        int pPosX, int pPosY, int pPosZ, int pRange, RandomSource pRandom, @Nullable Block pBlock, BlockPos.MutableBlockPos pBlockPos
    ) {
        int i = pPosX + this.random.nextInt(pRange) - this.random.nextInt(pRange);
        int j = pPosY + this.random.nextInt(pRange) - this.random.nextInt(pRange);
        int k = pPosZ + this.random.nextInt(pRange) - this.random.nextInt(pRange);
        pBlockPos.set(i, j, k);
        BlockState blockstate = this.getBlockState(pBlockPos);
        blockstate.getBlock().animateTick(blockstate, this, pBlockPos, pRandom);
        FluidState fluidstate = this.getFluidState(pBlockPos);
        if (!fluidstate.isEmpty()) {
            fluidstate.animateTick(this, pBlockPos, pRandom);
            ParticleOptions particleoptions = fluidstate.getDripParticle();
            if (particleoptions != null && this.random.nextInt(10) == 0) {
                boolean flag = blockstate.isFaceSturdy(this, pBlockPos, Direction.DOWN);
                BlockPos blockpos = pBlockPos.below();
                this.trySpawnDripParticles(blockpos, this.getBlockState(blockpos), particleoptions, flag);
            }
        }

        if (pBlock == blockstate.getBlock()) {
            this.addParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, blockstate), i + 0.5, j + 0.5, k + 0.5, 0.0, 0.0, 0.0);
        }

        if (!blockstate.isCollisionShapeFullBlock(this, pBlockPos)) {
            this.getBiome(pBlockPos)
                .value()
                .getAmbientParticle()
                .ifPresent(
                    p_264703_ -> {
                        if (p_264703_.canSpawn(this.random)) {
                            this.addParticle(
                                p_264703_.getOptions(),
                                pBlockPos.getX() + this.random.nextDouble(),
                                pBlockPos.getY() + this.random.nextDouble(),
                                pBlockPos.getZ() + this.random.nextDouble(),
                                0.0,
                                0.0,
                                0.0
                            );
                        }
                    }
                );
        }
    }

    private void trySpawnDripParticles(BlockPos pBlockPos, BlockState pBlockState, ParticleOptions pParticleData, boolean pShapeDownSolid) {
        if (pBlockState.getFluidState().isEmpty()) {
            VoxelShape voxelshape = pBlockState.getCollisionShape(this, pBlockPos);
            double d0 = voxelshape.max(Direction.Axis.Y);
            if (d0 < 1.0) {
                if (pShapeDownSolid) {
                    this.spawnFluidParticle(
                        pBlockPos.getX(),
                        pBlockPos.getX() + 1,
                        pBlockPos.getZ(),
                        pBlockPos.getZ() + 1,
                        pBlockPos.getY() + 1 - 0.05,
                        pParticleData
                    );
                }
            } else if (!pBlockState.is(BlockTags.IMPERMEABLE)) {
                double d1 = voxelshape.min(Direction.Axis.Y);
                if (d1 > 0.0) {
                    this.spawnParticle(pBlockPos, pParticleData, voxelshape, pBlockPos.getY() + d1 - 0.05);
                } else {
                    BlockPos blockpos = pBlockPos.below();
                    BlockState blockstate = this.getBlockState(blockpos);
                    VoxelShape voxelshape1 = blockstate.getCollisionShape(this, blockpos);
                    double d2 = voxelshape1.max(Direction.Axis.Y);
                    if (d2 < 1.0 && blockstate.getFluidState().isEmpty()) {
                        this.spawnParticle(pBlockPos, pParticleData, voxelshape, pBlockPos.getY() - 0.05);
                    }
                }
            }
        }
    }

    private void spawnParticle(BlockPos pPos, ParticleOptions pParticleData, VoxelShape pVoxelShape, double pY) {
        this.spawnFluidParticle(
            pPos.getX() + pVoxelShape.min(Direction.Axis.X),
            pPos.getX() + pVoxelShape.max(Direction.Axis.X),
            pPos.getZ() + pVoxelShape.min(Direction.Axis.Z),
            pPos.getZ() + pVoxelShape.max(Direction.Axis.Z),
            pY,
            pParticleData
        );
    }

    private void spawnFluidParticle(double pXStart, double pXEnd, double pZStart, double pZEnd, double pY, ParticleOptions pParticleData) {
        this.addParticle(
            pParticleData,
            Mth.lerp(this.random.nextDouble(), pXStart, pXEnd),
            pY,
            Mth.lerp(this.random.nextDouble(), pZStart, pZEnd),
            0.0,
            0.0,
            0.0
        );
    }

    @Override
    public CrashReportCategory fillReportDetails(CrashReport pReport) {
        CrashReportCategory crashreportcategory = super.fillReportDetails(pReport);
        crashreportcategory.setDetail("Server brand", () -> this.minecraft.player.connection.serverBrand());
        crashreportcategory.setDetail(
            "Server type", () -> this.minecraft.getSingleplayerServer() == null ? "Non-integrated multiplayer server" : "Integrated singleplayer server"
        );
        crashreportcategory.setDetail("Tracked entity count", () -> String.valueOf(this.getEntityCount()));
        return crashreportcategory;
    }

    @Override
    public void playSeededSound(
        @Nullable Entity p_394722_,
        double p_263372_,
        double p_263404_,
        double p_263365_,
        Holder<SoundEvent> p_263335_,
        SoundSource p_263417_,
        float p_263416_,
        float p_263349_,
        long p_263408_
    ) {
        var event = net.minecraftforge.event.ForgeEventFactory.onPlaySoundAtPosition(this, p_263372_, p_263404_, p_263365_, p_263335_, p_263417_, p_263416_, p_263349_);
        if (event.isCanceled() || event.getSound() == null) return;
        p_263335_ = event.getSound();
        p_263417_ = event.getSource();
        p_263416_ = event.getNewVolume();
        p_263349_ = event.getNewPitch();
        if (p_394722_ == this.minecraft.player) {
            this.playSound(p_263372_, p_263404_, p_263365_, p_263335_.value(), p_263417_, p_263416_, p_263349_, false, p_263408_);
        }
    }

    @Override
    public void playSeededSound(
        @Nullable Entity p_263536_, Entity p_396496_, Holder<SoundEvent> p_263518_, SoundSource p_263487_, float p_263538_, float p_263524_, long p_263509_
    ) {
        var event = net.minecraftforge.event.ForgeEventFactory.onPlaySoundAtEntity(this.minecraft.level, p_263536_, p_263518_, p_263487_, p_263538_, p_263524_);
        if (event.isCanceled() || event.getSound() == null) return;
        p_263518_ = event.getSound();
        p_263487_ = event.getSource();
        p_263538_ = event.getNewVolume();
        p_263524_ = event.getNewPitch();
        if (p_263536_ == this.minecraft.player) {
            this.minecraft.getSoundManager().play(new EntityBoundSoundInstance(p_263518_.value(), p_263487_, p_263538_, p_263524_, p_396496_, p_263509_));
        }
    }

    @Override
    public void playLocalSound(Entity p_311985_, SoundEvent p_309564_, SoundSource p_310311_, float p_309468_, float p_312364_) {
        this.minecraft.getSoundManager().play(new EntityBoundSoundInstance(p_309564_, p_310311_, p_309468_, p_312364_, p_311985_, this.random.nextLong()));
    }

    @Override
    public void playPlayerSound(SoundEvent p_395408_, SoundSource p_394651_, float p_394931_, float p_393353_) {
        if (this.minecraft.player != null) {
            this.minecraft
                .getSoundManager()
                .play(new EntityBoundSoundInstance(p_395408_, p_394651_, p_394931_, p_393353_, this.minecraft.player, this.random.nextLong()));
        }
    }

    @Override
    public void playLocalSound(
        double pX, double pY, double pZ, SoundEvent pSound, SoundSource pCategory, float pVolume, float pPitch, boolean pDistanceDelay
    ) {
        this.playSound(pX, pY, pZ, pSound, pCategory, pVolume, pPitch, pDistanceDelay, this.random.nextLong());
    }

    private void playSound(
        double pX,
        double pY,
        double pZ,
        SoundEvent pSoundEvent,
        SoundSource pSource,
        float pVolume,
        float pPitch,
        boolean pDistanceDelay,
        long pSeed
    ) {
        double d0 = this.minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(pX, pY, pZ);
        SimpleSoundInstance simplesoundinstance = new SimpleSoundInstance(
            pSoundEvent, pSource, pVolume, pPitch, RandomSource.create(pSeed), pX, pY, pZ
        );
        if (pDistanceDelay && d0 > 100.0) {
            double d1 = Math.sqrt(d0) / 40.0;
            this.minecraft.getSoundManager().playDelayed(simplesoundinstance, (int)(d1 * 20.0));
        } else {
            this.minecraft.getSoundManager().play(simplesoundinstance);
        }
    }

    @Override
    public void createFireworks(
        double p_104585_, double p_104586_, double p_104587_, double p_104588_, double p_104589_, double p_104590_, List<FireworkExplosion> p_330286_
    ) {
        if (p_330286_.isEmpty()) {
            for (int i = 0; i < this.random.nextInt(3) + 2; i++) {
                this.addParticle(
                    ParticleTypes.POOF, p_104585_, p_104586_, p_104587_, this.random.nextGaussian() * 0.05, 0.005, this.random.nextGaussian() * 0.05
                );
            }
        } else {
            this.minecraft
                .particleEngine
                .add(
                    new FireworkParticles.Starter(this, p_104585_, p_104586_, p_104587_, p_104588_, p_104589_, p_104590_, this.minecraft.particleEngine, p_330286_)
                );
        }
    }

    @Override
    public void sendPacketToServer(Packet<?> pPacket) {
        this.connection.send(pPacket);
    }

    @Override
    public RecipeAccess recipeAccess() {
        return this.connection.recipes();
    }

    @Override
    public TickRateManager tickRateManager() {
        return this.tickRateManager;
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    public ClientChunkCache getChunkSource() {
        return this.chunkSource;
    }

    @Nullable
    @Override
    public MapItemSavedData getMapData(MapId p_334091_) {
        return this.mapData.get(p_334091_);
    }

    public void overrideMapData(MapId pMapId, MapItemSavedData pMapData) {
        this.mapData.put(pMapId, pMapData);
    }

    @Override
    public Scoreboard getScoreboard() {
        return this.connection.scoreboard();
    }

    @Override
    public void sendBlockUpdated(BlockPos pPos, BlockState pOldState, BlockState pNewState, int pFlags) {
        this.levelRenderer.blockChanged(this, pPos, pOldState, pNewState, pFlags);
    }

    @Override
    public void setBlocksDirty(BlockPos pBlockPos, BlockState pOldState, BlockState pNewState) {
        this.levelRenderer.setBlockDirty(pBlockPos, pOldState, pNewState);
    }

    public void setSectionDirtyWithNeighbors(int pSectionX, int pSectionY, int pSectionZ) {
        this.levelRenderer.setSectionDirtyWithNeighbors(pSectionX, pSectionY, pSectionZ);
    }

    public void setSectionRangeDirty(int pMinY, int pMinX, int pMinZ, int pMaxY, int pMaxX, int pMaxZ) {
        this.levelRenderer.setSectionRangeDirty(pMinY, pMinX, pMinZ, pMaxY, pMaxX, pMaxZ);
    }

    @Override
    public void destroyBlockProgress(int pBreakerId, BlockPos pPos, int pProgress) {
        this.levelRenderer.destroyBlockProgress(pBreakerId, pPos, pProgress);
    }

    @Override
    public void globalLevelEvent(int pId, BlockPos pPos, int pData) {
        this.levelEventHandler.globalLevelEvent(pId, pPos, pData);
    }

    @Override
    public void levelEvent(@Nullable Entity p_396292_, int p_104655_, BlockPos p_104656_, int p_104657_) {
        try {
            this.levelEventHandler.levelEvent(p_104655_, p_104656_, p_104657_);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Playing level event");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Level event being played");
            crashreportcategory.setDetail("Block coordinates", CrashReportCategory.formatLocation(this, p_104656_));
            crashreportcategory.setDetail("Event source", p_396292_);
            crashreportcategory.setDetail("Event type", p_104655_);
            crashreportcategory.setDetail("Event data", p_104657_);
            throw new ReportedException(crashreport);
        }
    }

    @Override
    public void addParticle(ParticleOptions pParticleData, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
        this.levelRenderer.addParticle(pParticleData, pParticleData.getType().getOverrideLimiter(), pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
    }

    @Override
    public void addParticle(
        ParticleOptions p_104714_,
        boolean p_104715_,
        boolean p_376911_,
        double p_104716_,
        double p_104717_,
        double p_104718_,
        double p_104719_,
        double p_104720_,
        double p_104721_
    ) {
        this.levelRenderer
            .addParticle(p_104714_, p_104714_.getType().getOverrideLimiter() || p_104715_, p_376911_, p_104716_, p_104717_, p_104718_, p_104719_, p_104720_, p_104721_);
    }

    @Override
    public void addAlwaysVisibleParticle(ParticleOptions pParticleData, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
        this.levelRenderer.addParticle(pParticleData, false, true, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
    }

    @Override
    public void addAlwaysVisibleParticle(
        ParticleOptions pParticleData,
        boolean pIgnoreRange,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed
    ) {
        this.levelRenderer
            .addParticle(pParticleData, pParticleData.getType().getOverrideLimiter() || pIgnoreRange, true, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
    }

    @Override
    public List<AbstractClientPlayer> players() {
        return this.players;
    }

    public List<EnderDragonPart> dragonParts() {
        return this.dragonParts;
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int p_205516_, int p_205517_, int p_205518_) {
        return this.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
    }

    public float getSkyDarken(float pPartialTick) {
        float f = this.getTimeOfDay(pPartialTick);
        float f1 = 1.0F - (Mth.cos(f * (float) (Math.PI * 2)) * 2.0F + 0.2F);
        f1 = Mth.clamp(f1, 0.0F, 1.0F);
        f1 = 1.0F - f1;
        f1 *= 1.0F - this.getRainLevel(pPartialTick) * 5.0F / 16.0F;
        f1 *= 1.0F - this.getThunderLevel(pPartialTick) * 5.0F / 16.0F;
        return f1 * 0.8F + 0.2F;
    }

    public int getSkyColor(Vec3 pCameraPosition, float pPartialTick) {
        float f = this.getTimeOfDay(pPartialTick);
        Vec3 vec3 = pCameraPosition.subtract(2.0, 2.0, 2.0).scale(0.25);
        Vec3 vec31 = CubicSampler.gaussianSampleVec3(
            vec3, (p_389332_, p_389333_, p_389334_) -> Vec3.fromRGB24(this.getBiomeManager().getNoiseBiomeAtQuart(p_389332_, p_389333_, p_389334_).value().getSkyColor())
        );
        float f1 = Mth.cos(f * (float) (Math.PI * 2)) * 2.0F + 0.5F;
        f1 = Mth.clamp(f1, 0.0F, 1.0F);
        vec31 = vec31.scale(f1);
        int i = ARGB.color(vec31);
        float f2 = this.getRainLevel(pPartialTick);
        if (f2 > 0.0F) {
            float f3 = 0.6F;
            float f4 = f2 * 0.75F;
            int j = ARGB.scaleRGB(ARGB.greyscale(i), 0.6F);
            i = ARGB.lerp(f4, i, j);
        }

        float f5 = this.getThunderLevel(pPartialTick);
        if (f5 > 0.0F) {
            float f6 = 0.2F;
            float f7 = f5 * 0.75F;
            int k = ARGB.scaleRGB(ARGB.greyscale(i), 0.2F);
            i = ARGB.lerp(f7, i, k);
        }

        int l = this.getSkyFlashTime();
        if (l > 0) {
            float f8 = Math.min(l - pPartialTick, 1.0F);
            f8 *= 0.45F;
            i = ARGB.lerp(f8, i, ARGB.color(204, 204, 255));
        }

        return i;
    }

    public int getCloudColor(float pPartialTick) {
        int i = -1;
        float f = this.getRainLevel(pPartialTick);
        if (f > 0.0F) {
            int j = ARGB.scaleRGB(ARGB.greyscale(i), 0.6F);
            i = ARGB.lerp(f * 0.95F, i, j);
        }

        float f3 = this.getTimeOfDay(pPartialTick);
        float f1 = Mth.cos(f3 * (float) (Math.PI * 2)) * 2.0F + 0.5F;
        f1 = Mth.clamp(f1, 0.0F, 1.0F);
        i = ARGB.multiply(i, ARGB.colorFromFloat(1.0F, f1 * 0.9F + 0.1F, f1 * 0.9F + 0.1F, f1 * 0.85F + 0.15F));
        float f2 = this.getThunderLevel(pPartialTick);
        if (f2 > 0.0F) {
            int k = ARGB.scaleRGB(ARGB.greyscale(i), 0.2F);
            i = ARGB.lerp(f2 * 0.95F, i, k);
        }

        return i;
    }

    public float getStarBrightness(float pPartialTick) {
        float f = this.getTimeOfDay(pPartialTick);
        float f1 = 1.0F - (Mth.cos(f * (float) (Math.PI * 2)) * 2.0F + 0.25F);
        f1 = Mth.clamp(f1, 0.0F, 1.0F);
        return f1 * f1 * 0.5F;
    }

    public int getSkyFlashTime() {
        return this.minecraft.options.hideLightningFlash().get() ? 0 : this.skyFlashTime;
    }

    @Override
    public void setSkyFlashTime(int pTimeFlash) {
        this.skyFlashTime = pTimeFlash;
    }

    @Override
    public float getShade(Direction p_104703_, boolean p_104704_) {
        boolean flag = this.effects().constantAmbientLight();
        if (!p_104704_) {
            return flag ? 0.9F : 1.0F;
        } else {
            switch (p_104703_) {
                case DOWN:
                    return flag ? 0.9F : 0.5F;
                case UP:
                    return flag ? 0.9F : 1.0F;
                case NORTH:
                case SOUTH:
                    return 0.8F;
                case WEST:
                case EAST:
                    return 0.6F;
                default:
                    return 1.0F;
            }
        }
    }

    @Override
    public int getBlockTint(BlockPos pBlockPos, ColorResolver pColorResolver) {
        BlockTintCache blocktintcache = this.tintCaches.get(pColorResolver);
        return blocktintcache.getColor(pBlockPos);
    }

    public int calculateBlockTint(BlockPos pBlockPos, ColorResolver pColorResolver) {
        int i = Minecraft.getInstance().options.biomeBlendRadius().get();
        if (i == 0) {
            return pColorResolver.getColor(this.getBiome(pBlockPos).value(), pBlockPos.getX(), pBlockPos.getZ());
        } else {
            int j = (i * 2 + 1) * (i * 2 + 1);
            int k = 0;
            int l = 0;
            int i1 = 0;
            Cursor3D cursor3d = new Cursor3D(
                pBlockPos.getX() - i,
                pBlockPos.getY(),
                pBlockPos.getZ() - i,
                pBlockPos.getX() + i,
                pBlockPos.getY(),
                pBlockPos.getZ() + i
            );
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            while (cursor3d.advance()) {
                blockpos$mutableblockpos.set(cursor3d.nextX(), cursor3d.nextY(), cursor3d.nextZ());
                int j1 = pColorResolver.getColor(
                    this.getBiome(blockpos$mutableblockpos).value(), blockpos$mutableblockpos.getX(), blockpos$mutableblockpos.getZ()
                );
                k += (j1 & 0xFF0000) >> 16;
                l += (j1 & 0xFF00) >> 8;
                i1 += j1 & 0xFF;
            }

            return (k / j & 0xFF) << 16 | (l / j & 0xFF) << 8 | i1 / j & 0xFF;
        }
    }

    public void setDefaultSpawnPos(BlockPos pSpawnPos, float pSpawnAngle) {
        this.levelData.setSpawn(pSpawnPos, pSpawnAngle);
    }

    @Override
    public String toString() {
        return "ClientLevel";
    }

    public ClientLevel.ClientLevelData getLevelData() {
        return this.clientLevelData;
    }

    @Override
    public void gameEvent(Holder<GameEvent> p_333083_, Vec3 p_233640_, GameEvent.Context p_233641_) {
    }

    protected Map<MapId, MapItemSavedData> getAllMapData() {
        return ImmutableMap.copyOf(this.mapData);
    }

    protected void addMapData(Map<MapId, MapItemSavedData> pMap) {
        this.mapData.putAll(pMap);
    }

    @Override
    protected LevelEntityGetter<Entity> getEntities() {
        return this.entityStorage.getEntityGetter();
    }

    @Override
    public String gatherChunkSourceStats() {
        return "Chunks[C] W: " + this.chunkSource.gatherStats() + " E: " + this.entityStorage.gatherStats();
    }

    @Override
    public void addDestroyBlockEffect(BlockPos p_171667_, BlockState p_171668_) {
        this.minecraft.particleEngine.destroy(p_171667_, p_171668_);
    }

    public void setServerSimulationDistance(int pServerSimulationDistance) {
        this.serverSimulationDistance = pServerSimulationDistance;
    }

    public int getServerSimulationDistance() {
        return this.serverSimulationDistance;
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return this.connection.enabledFeatures();
    }

    @Override
    public PotionBrewing potionBrewing() {
        return this.connection.potionBrewing();
    }

    @Override
    public FuelValues fuelValues() {
        return this.connection.fuelValues();
    }

    @Override
    public void explode(
        @Nullable Entity p_363323_,
        @Nullable DamageSource p_363606_,
        @Nullable ExplosionDamageCalculator p_366529_,
        double p_367635_,
        double p_367395_,
        double p_369080_,
        float p_362961_,
        boolean p_363119_,
        Level.ExplosionInteraction p_360993_,
        ParticleOptions p_360968_,
        ParticleOptions p_360771_,
        Holder<SoundEvent> p_368228_
    ) {
    }

    @Override
    public int getSeaLevel() {
        return this.seaLevel;
    }

    @Override
    public int getClientLeafTintColor(BlockPos p_391708_) {
        return Minecraft.getInstance().getBlockColors().getColor(this.getBlockState(p_391708_), this, p_391708_, 0);
    }

    @Override
    public void registerForCleaning(CacheSlot<ClientLevel, ?> p_396040_) {
        this.connection.registerForCleaning(p_396040_);
    }

    @Override
    public java.util.Collection<net.minecraftforge.entity.PartEntity<?>> getPartEntities() {
       return this.partEntities.values();
    }

    @Override
    public net.minecraftforge.client.model.data.ModelDataManager getModelDataManager() {
       return modelDataManager;
    }

    @OnlyIn(Dist.CLIENT)
    public static class ClientLevelData implements WritableLevelData {
        private final boolean hardcore;
        private final boolean isFlat;
        private BlockPos spawnPos;
        private float spawnAngle;
        private long gameTime;
        private long dayTime;
        private boolean raining;
        private Difficulty difficulty;
        private boolean difficultyLocked;

        public ClientLevelData(Difficulty pDifficulty, boolean pHardcore, boolean pIsFlat) {
            this.difficulty = pDifficulty;
            this.hardcore = pHardcore;
            this.isFlat = pIsFlat;
        }

        @Override
        public BlockPos getSpawnPos() {
            return this.spawnPos;
        }

        @Override
        public float getSpawnAngle() {
            return this.spawnAngle;
        }

        @Override
        public long getGameTime() {
            return this.gameTime;
        }

        @Override
        public long getDayTime() {
            return this.dayTime;
        }

        public void setGameTime(long pGameTime) {
            this.gameTime = pGameTime;
        }

        public void setDayTime(long pDayTime) {
            this.dayTime = pDayTime;
        }

        @Override
        public void setSpawn(BlockPos pSpawnPoint, float pAngle) {
            this.spawnPos = pSpawnPoint.immutable();
            this.spawnAngle = pAngle;
        }

        @Override
        public boolean isThundering() {
            return false;
        }

        @Override
        public boolean isRaining() {
            return this.raining;
        }

        @Override
        public void setRaining(boolean pIsRaining) {
            this.raining = pIsRaining;
        }

        @Override
        public boolean isHardcore() {
            return this.hardcore;
        }

        @Override
        public Difficulty getDifficulty() {
            return this.difficulty;
        }

        @Override
        public boolean isDifficultyLocked() {
            return this.difficultyLocked;
        }

        @Override
        public void fillCrashReportCategory(CrashReportCategory p_171690_, LevelHeightAccessor p_171691_) {
            WritableLevelData.super.fillCrashReportCategory(p_171690_, p_171691_);
        }

        public void setDifficulty(Difficulty pDifficulty) {
            net.minecraftforge.event.ForgeEventFactory.onDifficultyChange(pDifficulty, this.difficulty);
            this.difficulty = pDifficulty;
        }

        public void setDifficultyLocked(boolean pDifficultyLocked) {
            this.difficultyLocked = pDifficultyLocked;
        }

        public double getHorizonHeight(LevelHeightAccessor pLevel) {
            return this.isFlat ? pLevel.getMinY() : 63.0;
        }

        public float getClearColorScale() {
            return this.isFlat ? 1.0F : 0.03125F;
        }
    }

    @OnlyIn(Dist.CLIENT)
    final class EntityCallbacks implements LevelCallback<Entity> {
        public void onCreated(Entity p_171696_) {
        }

        public void onDestroyed(Entity p_171700_) {
        }

        public void onTickingStart(Entity p_171704_) {
            ClientLevel.this.tickingEntities.add(p_171704_);
        }

        public void onTickingEnd(Entity p_171708_) {
            ClientLevel.this.tickingEntities.remove(p_171708_);
        }

        public void onTrackingStart(Entity p_171712_) {
            switch (p_171712_) {
                case AbstractClientPlayer abstractclientplayer:
                    ClientLevel.this.players.add(abstractclientplayer);
                    break;
                case EnderDragon enderdragon:
                    ClientLevel.this.dragonParts.addAll(Arrays.asList(enderdragon.getSubEntities()));
                    break;
                default:
            }

            if (p_171712_.isMultipartEntity()) {
               for (var part : p_171712_.getParts()) {
                  ClientLevel.this.partEntities.put(part.getId(), part);
               }
            }
        }

        public void onTrackingEnd(Entity p_171716_) {
            p_171716_.unRide();
            switch (p_171716_) {
                case AbstractClientPlayer abstractclientplayer:
                    ClientLevel.this.players.remove(abstractclientplayer);
                    break;
                case EnderDragon enderdragon:
                    ClientLevel.this.dragonParts.removeAll(Arrays.asList(enderdragon.getSubEntities()));
                    break;
                default:
            }

            p_171716_.onRemovedFromWorld();
            net.minecraftforge.event.ForgeEventFactory.onEntityLeaveLevel(p_171716_, ClientLevel.this);

            if (p_171716_.isMultipartEntity()) {
               for (var part : p_171716_.getParts()) {
                  ClientLevel.this.partEntities.remove(part.getId());
               }
            }
        }

        public void onSectionChange(Entity p_233660_) {
        }
    }
}
