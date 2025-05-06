package net.minecraft.gametest.framework;

import com.google.common.base.Stopwatch;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.brigadier.StringReader;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.ReportType;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceSelectorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.debugchart.LocalSampleLogger;
import net.minecraft.util.debugchart.SampleLogger;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.slf4j.Logger;

public class GameTestServer extends MinecraftServer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PROGRESS_REPORT_INTERVAL = 20;
    private static final int TEST_POSITION_RANGE = 14999992;
    private static final Services NO_SERVICES = new Services(null, ServicesKeySet.EMPTY, null, null);
    private static final FeatureFlagSet ENABLED_FEATURES = FeatureFlags.REGISTRY
        .allFlags()
        .subtract(FeatureFlagSet.of(FeatureFlags.REDSTONE_EXPERIMENTS, FeatureFlags.MINECART_IMPROVEMENTS));
    private final LocalSampleLogger sampleLogger = new LocalSampleLogger(4);
    private final Optional<String> testSelection;
    private final boolean verify;
    private List<GameTestBatch> testBatches = new ArrayList<>();
    private final Stopwatch stopwatch = Stopwatch.createUnstarted();
    private static final WorldOptions WORLD_OPTIONS = new WorldOptions(0L, false, false);
    @Nullable
    private MultipleTestTracker testTracker;

    public static GameTestServer create(
        Thread pServerThread, LevelStorageSource.LevelStorageAccess pStorageSource, PackRepository pPackRepository, Optional<String> pTestSelection, boolean pVerify
    ) {
        pPackRepository.reload();
        ArrayList<String> arraylist = new ArrayList<>(pPackRepository.getSelectedIds()); // Forge: Use the selected datapacks as this takes sorting into account.
        arraylist.remove("vanilla");
        arraylist.addFirst("vanilla");
        WorldDataConfiguration worlddataconfiguration = new WorldDataConfiguration(new DataPackConfig(arraylist, List.of()), ENABLED_FEATURES);
        LevelSettings levelsettings = new LevelSettings(
            "Test Level", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(ENABLED_FEATURES), worlddataconfiguration
        );
        WorldLoader.PackConfig worldloader$packconfig = new WorldLoader.PackConfig(pPackRepository, worlddataconfiguration, false, true);
        WorldLoader.InitConfig worldloader$initconfig = new WorldLoader.InitConfig(worldloader$packconfig, Commands.CommandSelection.DEDICATED, 4);

        try {
            LOGGER.debug("Starting resource loading");
            Stopwatch stopwatch = Stopwatch.createStarted();
            WorldStem worldstem = Util.<WorldStem>blockUntilDone(
                    p_358474_ -> WorldLoader.load(
                        worldloader$initconfig,
                        p_358476_ -> {
                            Registry<LevelStem> registry = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable()).freeze();
                            WorldDimensions.Complete worlddimensions$complete = p_358476_.datapackWorldgen()
                                .lookupOrThrow(Registries.WORLD_PRESET)
                                .getOrThrow(WorldPresets.FLAT)
                                .value()
                                .createWorldDimensions()
                                .bake(registry);
                            return new WorldLoader.DataLoadOutput<>(
                                new PrimaryLevelData(levelsettings, WORLD_OPTIONS, worlddimensions$complete.specialWorldProperty(), worlddimensions$complete.lifecycle()),
                                worlddimensions$complete.dimensionsRegistryAccess()
                            );
                        },
                        WorldStem::new,
                        Util.backgroundExecutor(),
                        p_358474_
                    )
                )
                .get();
            stopwatch.stop();
            LOGGER.debug("Finished resource loading after {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            return new GameTestServer(pServerThread, pStorageSource, pPackRepository, worldstem, pTestSelection, pVerify);
        } catch (Exception exception) {
            LOGGER.warn("Failed to load vanilla datapack, bit oops", (Throwable)exception);
            System.exit(-1);
            throw new IllegalStateException();
        }
    }

    private GameTestServer(
        Thread pServerThread,
        LevelStorageSource.LevelStorageAccess pStorageSource,
        PackRepository pPackRepository,
        WorldStem pWorldStem,
        Optional<String> pTestSelection,
        boolean pVerify
    ) {
        super(pServerThread, pStorageSource, pPackRepository, pWorldStem, Proxy.NO_PROXY, DataFixers.getDataFixer(), NO_SERVICES, LoggerChunkProgressListener::createFromGameruleRadius);
        this.testSelection = pTestSelection;
        this.verify = pVerify;
    }

    @Override
    public boolean initServer() {
        if (!net.minecraftforge.server.ServerLifecycleHooks.handleServerAboutToStart(this)) return false;
        this.setPlayerList(new PlayerList(this, this.registries(), this.playerDataStorage, 1) {});
        this.loadLevel();
        ServerLevel serverlevel = this.overworld();
        this.testBatches = this.evaluateTestsToRun(serverlevel);
        LOGGER.info("Started game test server");
        return true;
    }

    private List<GameTestBatch> evaluateTestsToRun(ServerLevel pLevel) {
        Registry<GameTestInstance> registry = pLevel.registryAccess().lookupOrThrow(Registries.TEST_INSTANCE);
        Collection<Holder.Reference<GameTestInstance>> collection;
        GameTestBatchFactory.TestDecorator gametestbatchfactory$testdecorator;
        if (this.testSelection.isPresent()) {
            collection = getTestsForSelection(pLevel.registryAccess(), this.testSelection.get()).filter(p_389777_ -> !p_389777_.value().manualOnly()).toList();
            if (this.verify) {
                gametestbatchfactory$testdecorator = GameTestServer::rotateAndMultiply;
                LOGGER.info("Verify requested. Will run each test that matches {} {} times", this.testSelection.get(), 100 * Rotation.values().length);
            } else {
                gametestbatchfactory$testdecorator = GameTestBatchFactory.DIRECT;
                LOGGER.info("Will run tests matching {} ({} tests)", this.testSelection.get(), collection.size());
            }
        } else {
            collection = registry.listElements().filter(p_389778_ -> !p_389778_.value().manualOnly()).toList();
            gametestbatchfactory$testdecorator = GameTestBatchFactory.DIRECT;
        }

        return GameTestBatchFactory.divideIntoBatches(collection, gametestbatchfactory$testdecorator, pLevel);
    }

    private static Stream<GameTestInfo> rotateAndMultiply(Holder.Reference<GameTestInstance> pTest, ServerLevel pLevel) {
        Builder<GameTestInfo> builder = Stream.builder();

        for (Rotation rotation : Rotation.values()) {
            for (int i = 0; i < 100; i++) {
                builder.add(new GameTestInfo(pTest, rotation, pLevel, RetryOptions.noRetries()));
            }
        }

        return builder.build();
    }

    public static Stream<Holder.Reference<GameTestInstance>> getTestsForSelection(RegistryAccess pRegistries, String pSelection) {
        return ResourceSelectorArgument.parse(new StringReader(pSelection), pRegistries.lookupOrThrow(Registries.TEST_INSTANCE)).stream();
    }

    @Override
    public void tickServer(BooleanSupplier p_177619_) {
        super.tickServer(p_177619_);
        ServerLevel serverlevel = this.overworld();
        if (!this.haveTestsStarted()) {
            this.startTests(serverlevel);
        }

        if (serverlevel.getGameTime() % 20L == 0L) {
            LOGGER.info(this.testTracker.getProgressBar());
        }

        if (this.testTracker.isDone()) {
            this.halt(false);
            LOGGER.info(this.testTracker.getProgressBar());
            GlobalTestReporter.finish();
            LOGGER.info("========= {} GAME TESTS COMPLETE IN {} ======================", this.testTracker.getTotalCount(), this.stopwatch.stop());
            if (this.testTracker.hasFailedRequired()) {
                LOGGER.info("{} required tests failed :(", this.testTracker.getFailedRequiredCount());
                this.testTracker.getFailedRequired().forEach(GameTestServer::logFailedTest);
            } else {
                LOGGER.info("All {} required tests passed :)", this.testTracker.getTotalCount());
            }

            if (this.testTracker.hasFailedOptional()) {
                LOGGER.info("{} optional tests failed", this.testTracker.getFailedOptionalCount());
                this.testTracker.getFailedOptional().forEach(GameTestServer::logFailedTest);
            }

            LOGGER.info("====================================================");
        }
    }

    private static void logFailedTest(GameTestInfo pInfo) {
        if (pInfo.getRotation() != Rotation.NONE) {
            LOGGER.info(
                "   - {} with rotation {}: {}", pInfo.id(), pInfo.getRotation().getSerializedName(), pInfo.getError().getDescription().getString()
            );
        } else {
            LOGGER.info("   - {}: {}", pInfo.id(), pInfo.getError().getDescription().getString());
        }
    }

    @Override
    public SampleLogger getTickTimeLogger() {
        return this.sampleLogger;
    }

    @Override
    public boolean isTickTimeLoggingEnabled() {
        return false;
    }

    @Override
    public void waitUntilNextTick() {
        this.runAllTasks();
    }

    @Override
    public SystemReport fillServerSystemReport(SystemReport p_177613_) {
        p_177613_.setDetail("Type", "Game test server");
        return p_177613_;
    }

    @Override
    public void onServerExit() {
        super.onServerExit();
        LOGGER.info("Game test server shutting down");
        System.exit(this.testTracker != null ? this.testTracker.getFailedRequiredCount() : -1);
    }

    @Override
    public void onServerCrash(CrashReport p_177623_) {
        super.onServerCrash(p_177623_);
        LOGGER.error("Game test server crashed\n{}", p_177623_.getFriendlyReport(ReportType.CRASH));
        System.exit(1);
    }

    private void startTests(ServerLevel pServerLevel) {
        BlockPos blockpos = new BlockPos(pServerLevel.random.nextIntBetweenInclusive(-14999992, 14999992), -59, pServerLevel.random.nextIntBetweenInclusive(-14999992, 14999992));
        pServerLevel.setDefaultSpawnPos(blockpos, 0.0F);
        GameTestRunner gametestrunner = GameTestRunner.Builder.fromBatches(this.testBatches, pServerLevel)
            .newStructureSpawner(new StructureGridSpawner(blockpos, 8, false))
            .build();
        Collection<GameTestInfo> collection = gametestrunner.getTestInfos();
        this.testTracker = new MultipleTestTracker(collection);
        LOGGER.info("{} tests are now running at position {}!", this.testTracker.getTotalCount(), blockpos.toShortString());
        this.stopwatch.reset();
        this.stopwatch.start();
        gametestrunner.start();
    }

    private boolean haveTestsStarted() {
        return this.testTracker != null;
    }

    @Override
    public boolean isHardcore() {
        return false;
    }

    @Override
    public int getOperatorUserPermissionLevel() {
        return 0;
    }

    @Override
    public int getFunctionCompilationLevel() {
        return 4;
    }

    @Override
    public boolean shouldRconBroadcast() {
        return false;
    }

    @Override
    public boolean isDedicatedServer() {
        return false;
    }

    @Override
    public int getRateLimitPacketsPerSecond() {
        return 0;
    }

    @Override
    public boolean isEpollEnabled() {
        return false;
    }

    @Override
    public boolean isCommandBlockEnabled() {
        return true;
    }

    @Override
    public boolean isPublished() {
        return false;
    }

    @Override
    public boolean shouldInformAdmins() {
        return false;
    }

    @Override
    public boolean isSingleplayerOwner(GameProfile p_177617_) {
        return false;
    }
}
