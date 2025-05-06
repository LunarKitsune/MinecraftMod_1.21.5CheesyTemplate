package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.UserApiService.UserFlag;
import com.mojang.authlib.minecraft.UserApiService.UserProperties;
import com.mojang.authlib.yggdrasil.ProfileActionType;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.ServicesKeyType;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.ClientShutdownWatchdog;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.TimerQuery;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.datafixers.DataFixer;
import com.mojang.jtracy.DiscontinuousFrame;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.net.Proxy;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.FileUtil;
import net.minecraft.Optionull;
import net.minecraft.ReportType;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiSpriteManager;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.BanNoticeScreens;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.OutOfMemoryScreen;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.main.SilentInitException;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.profiling.ClientMetricsSamplersProvider;
import net.minecraft.client.quickplay.QuickPlay;
import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.VirtualScreen;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.client.resources.DryFoliageColorReloadListener;
import net.minecraft.client.resources.FoliageColorReloadListener;
import net.minecraft.client.resources.GrassColorReloadListener;
import net.minecraft.client.resources.MapDecorationTextureManager;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.client.resources.PaintingTextureManager;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.sounds.MusicInfo;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.client.telemetry.events.GameLoadTimesEvent;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.KeybindResolver;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ProcessorChunkProgressListener;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.FileZipper;
import net.minecraft.util.MemoryReserve;
import net.minecraft.util.ModCheck;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.Unit;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.ContinuousProfiler;
import net.minecraft.util.profiling.EmptyProfileResults;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.SingleTickProfiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.util.profiling.metrics.profiling.ActiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.InactiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.MetricsRecorder;
import net.minecraft.util.profiling.metrics.storage.MetricsPersister;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.FileUtils;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Minecraft extends ReentrantBlockableEventLoop<Runnable> implements WindowEventHandler, net.minecraftforge.client.extensions.IForgeMinecraft {
    static Minecraft instance;
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final boolean ON_OSX = Util.getPlatform() == Util.OS.OSX;
    private static final int MAX_TICKS_PER_UPDATE = 10;
    public static final ResourceLocation DEFAULT_FONT = ResourceLocation.withDefaultNamespace("default");
    public static final ResourceLocation UNIFORM_FONT = ResourceLocation.withDefaultNamespace("uniform");
    public static final ResourceLocation ALT_FONT = ResourceLocation.withDefaultNamespace("alt");
    private static final ResourceLocation REGIONAL_COMPLIANCIES = ResourceLocation.withDefaultNamespace("regional_compliancies.json");
    private static final CompletableFuture<Unit> RESOURCE_RELOAD_INITIAL_TASK = CompletableFuture.completedFuture(Unit.INSTANCE);
    private static final Component SOCIAL_INTERACTIONS_NOT_AVAILABLE = Component.translatable("multiplayer.socialInteractions.not_available");
    public static final String UPDATE_DRIVERS_ADVICE = "Please make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).";
    private final long canary = Double.doubleToLongBits(Math.PI);
    private final Path resourcePackDirectory;
    private final CompletableFuture<ProfileResult> profileFuture;
    public final TextureManager textureManager;
    private final ShaderManager shaderManager;
    private final DataFixer fixerUpper;
    private final VirtualScreen virtualScreen;
    private final Window window;
    private final DeltaTracker.Timer deltaTracker = new DeltaTracker.Timer(20.0F, 0L, this::getTickTargetMillis);
    private final RenderBuffers renderBuffers;
    public final LevelRenderer levelRenderer;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final ItemModelResolver itemModelResolver;
    private final ItemRenderer itemRenderer;
    private final MapRenderer mapRenderer;
    public final ParticleEngine particleEngine;
    private final User user;
    public final Font font;
    public final Font fontFilterFishy;
    public final GameRenderer gameRenderer;
    public final DebugRenderer debugRenderer;
    private final AtomicReference<StoringChunkProgressListener> progressListener = new AtomicReference<>();
    public final Gui gui;
    public final Options options;
    private final HotbarManager hotbarManager;
    public final MouseHandler mouseHandler;
    public final KeyboardHandler keyboardHandler;
    private InputType lastInputType = InputType.NONE;
    public final File gameDirectory;
    private final String launchedVersion;
    private final String versionType;
    private final Proxy proxy;
    private final LevelStorageSource levelSource;
    private final boolean demo;
    private final boolean allowsMultiplayer;
    private final boolean allowsChat;
    private final ReloadableResourceManager resourceManager;
    private final VanillaPackResources vanillaPackResources;
    private final DownloadedPackSource downloadedPackSource;
    private final PackRepository resourcePackRepository;
    private final LanguageManager languageManager;
    private final BlockColors blockColors;
    private final RenderTarget mainRenderTarget;
    @Nullable
    private final TracyFrameCapture tracyFrameCapture;
    private final SoundManager soundManager;
    private final MusicManager musicManager;
    private final FontManager fontManager;
    private final SplashManager splashManager;
    private final GpuWarnlistManager gpuWarnlistManager;
    private final PeriodicNotificationManager regionalCompliancies = new PeriodicNotificationManager(REGIONAL_COMPLIANCIES, Minecraft::countryEqualsISO3);
    private final YggdrasilAuthenticationService authenticationService;
    private final MinecraftSessionService minecraftSessionService;
    private final UserApiService userApiService;
    private final CompletableFuture<UserProperties> userPropertiesFuture;
    private final SkinManager skinManager;
    private final ModelManager modelManager;
    private final BlockRenderDispatcher blockRenderer;
    private final PaintingTextureManager paintingTextures;
    private final MobEffectTextureManager mobEffectTextures;
    private final MapTextureManager mapTextureManager;
    private final MapDecorationTextureManager mapDecorationTextures;
    private final GuiSpriteManager guiSprites;
    private final ToastManager toastManager;
    private final Tutorial tutorial;
    private final PlayerSocialManager playerSocialManager;
    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
    private final ClientTelemetryManager telemetryManager;
    private final ProfileKeyPairManager profileKeyPairManager;
    private final RealmsDataFetcher realmsDataFetcher;
    private final QuickPlayLog quickPlayLog;
    @Nullable
    public MultiPlayerGameMode gameMode;
    @Nullable
    public ClientLevel level;
    @Nullable
    public LocalPlayer player;
    @Nullable
    private IntegratedServer singleplayerServer;
    @Nullable
    private Connection pendingConnection;
    private boolean isLocalServer;
    @Nullable
    public Entity cameraEntity;
    @Nullable
    public Entity crosshairPickEntity;
    @Nullable
    public HitResult hitResult;
    private int rightClickDelay;
    protected int missTime;
    private volatile boolean pause;
    private long lastNanoTime = Util.getNanos();
    private long lastTime;
    private int frames;
    public boolean noRender;
    @Nullable
    public Screen screen;
    @Nullable
    private Overlay overlay;
    private boolean clientLevelTeardownInProgress;
    Thread gameThread;
    private volatile boolean running;
    @Nullable
    private Supplier<CrashReport> delayedCrash;
    private static int fps;
    public String fpsString = "";
    private long frameTimeNs;
    private final FramerateLimitTracker framerateLimitTracker;
    public boolean wireframe;
    public boolean sectionPath;
    public boolean sectionVisibility;
    public boolean smartCull = true;
    private boolean windowActive;
    private final Queue<Runnable> progressTasks = Queues.newConcurrentLinkedQueue();
    @Nullable
    private CompletableFuture<Void> pendingReload;
    @Nullable
    private TutorialToast socialInteractionsToast;
    private int fpsPieRenderTicks;
    private final ContinuousProfiler fpsPieProfiler;
    private MetricsRecorder metricsRecorder = InactiveMetricsRecorder.INSTANCE;
    private final ResourceLoadStateTracker reloadStateTracker = new ResourceLoadStateTracker();
    private long savedCpuDuration;
    private double gpuUtilization;
    @Nullable
    private TimerQuery.FrameProfile currentFrameProfile;
    private final GameNarrator narrator;
    private final ChatListener chatListener;
    private ReportingContext reportingContext;
    private final CommandHistory commandHistory;
    private final DirectoryValidator directoryValidator;
    private boolean gameLoadFinished;
    private final long clientStartTimeMs;
    private long clientTickCount;

    public Minecraft(final GameConfig pGameConfig) {
        super("Client");
        instance = this;
        this.clientStartTimeMs = System.currentTimeMillis();
        this.gameDirectory = pGameConfig.location.gameDirectory;
        File file1 = pGameConfig.location.assetDirectory;
        this.resourcePackDirectory = pGameConfig.location.resourcePackDirectory.toPath();
        this.launchedVersion = pGameConfig.game.launchVersion;
        this.versionType = pGameConfig.game.versionType;
        Path path = this.gameDirectory.toPath();
        this.directoryValidator = LevelStorageSource.parseValidator(path.resolve("allowed_symlinks.txt"));
        ClientPackSource clientpacksource = new ClientPackSource(pGameConfig.location.getExternalAssetSource(), this.directoryValidator);
        this.downloadedPackSource = new DownloadedPackSource(this, path.resolve("downloads"), pGameConfig.user);
        RepositorySource repositorysource = new FolderRepositorySource(this.resourcePackDirectory, PackType.CLIENT_RESOURCES, PackSource.DEFAULT, this.directoryValidator);
        this.resourcePackRepository = new PackRepository(clientpacksource, this.downloadedPackSource.createRepositorySource(), repositorysource);
        this.vanillaPackResources = clientpacksource.getVanillaPack();
        this.proxy = pGameConfig.user.proxy;
        this.authenticationService = new YggdrasilAuthenticationService(this.proxy);
        this.minecraftSessionService = this.authenticationService.createMinecraftSessionService();
        this.user = pGameConfig.user.user;
        this.profileFuture = CompletableFuture.supplyAsync(() -> this.minecraftSessionService.fetchProfile(this.user.getProfileId(), true), Util.nonCriticalIoPool());
        this.userApiService = this.createUserApiService(this.authenticationService, pGameConfig);
        this.userPropertiesFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return this.userApiService.fetchProperties();
            } catch (AuthenticationException authenticationexception) {
                LOGGER.error("Failed to fetch user properties", (Throwable)authenticationexception);
                return UserApiService.OFFLINE_PROPERTIES;
            }
        }, Util.nonCriticalIoPool());
        LOGGER.info("Setting user: {}", this.user.getName());
        this.demo = pGameConfig.game.demo;
        this.allowsMultiplayer = !pGameConfig.game.disableMultiplayer;
        this.allowsChat = !pGameConfig.game.disableChat;
        this.singleplayerServer = null;
        KeybindResolver.setKeyResolver(KeyMapping::createNameSupplier);
        this.fixerUpper = DataFixers.getDataFixer();
        this.toastManager = new ToastManager(this);
        this.gameThread = Thread.currentThread();
        this.options = new Options(this, this.gameDirectory);
        boolean flag = this.options.startedCleanly;
        this.options.startedCleanly = false;
        this.options.save();
        RenderSystem.setShaderGlintAlpha(this.options.glintStrength().get());
        this.running = true;
        this.tutorial = new Tutorial(this, this.options);
        this.hotbarManager = new HotbarManager(path, this.fixerUpper);
        LOGGER.info("Backend library: {}", RenderSystem.getBackendDescription());
        DisplayData displaydata = pGameConfig.display;
        if (this.options.overrideHeight > 0 && this.options.overrideWidth > 0) {
            displaydata = pGameConfig.display.withSize(this.options.overrideWidth, this.options.overrideHeight);
        }

        if (!flag) {
            displaydata = displaydata.withFullscreen(false);
            this.options.fullscreenVideoModeString = null;
            LOGGER.warn("Detected unexpected shutdown during last game startup: resetting fullscreen mode");
        }

        Util.timeSource = RenderSystem.initBackendSystem();
        this.virtualScreen = new VirtualScreen(this);
        this.window = this.virtualScreen.newWindow(displaydata, this.options.fullscreenVideoModeString, this.createTitle());
        this.setWindowActive(true);
        this.window.setWindowCloseCallback(new Runnable() {
            private boolean threadStarted;

            @Override
            public void run() {
                if (!this.threadStarted) {
                    this.threadStarted = true;
                    ClientShutdownWatchdog.startShutdownWatchdog(pGameConfig.location.gameDirectory, Minecraft.this.gameThread.threadId());
                }
            }
        });
        GameLoadTimesEvent.INSTANCE.endStep(TelemetryProperty.LOAD_TIME_PRE_WINDOW_MS);

        try {
            this.window.setIcon(this.vanillaPackResources, SharedConstants.getCurrentVersion().isStable() ? IconSet.RELEASE : IconSet.SNAPSHOT);
        } catch (IOException ioexception) {
            LOGGER.error("Couldn't set icon", (Throwable)ioexception);
        }

        // FORGE: Move mouse and keyboard handler setup further below
        this.mouseHandler = new MouseHandler(this);
        this.keyboardHandler = new KeyboardHandler(this);
        RenderSystem.initRenderer(
            this.window.getWindow(),
            this.options.glDebugVerbosity,
            false,
            (p_389127_, p_389128_) -> this.getShaderManager().getShader(p_389127_, p_389128_),
            pGameConfig.game.renderDebugLabels
        );
        LOGGER.info("Using optional rendering extensions: {}", String.join(", ", RenderSystem.getDevice().getEnabledExtensions()));
        this.mainRenderTarget = new MainTarget(this.window.getWidth(), this.window.getHeight());
        this.resourceManager = new ReloadableResourceManager(PackType.CLIENT_RESOURCES);
        net.minecraftforge.client.loading.ClientModLoader.begin(this, this.resourcePackRepository, this.resourceManager);
        this.resourcePackRepository.reload();
        this.options.loadSelectedResourcePacks(this.resourcePackRepository);
        this.languageManager = new LanguageManager(this.options.languageCode, p_340763_ -> {
            if (this.player != null) {
                this.player.connection.updateSearchTrees();
            }
        });
        this.resourceManager.registerReloadListener(this.languageManager);
        this.textureManager = new TextureManager(this.resourceManager);
        this.resourceManager.registerReloadListener(this.textureManager);
        this.shaderManager = new ShaderManager(this.textureManager, this::triggerResourcePackRecovery);
        this.resourceManager.registerReloadListener(this.shaderManager);
        this.skinManager = new SkinManager(file1.toPath().resolve("skins"), this.minecraftSessionService, this);
        this.levelSource = new LevelStorageSource(path.resolve("saves"), path.resolve("backups"), this.directoryValidator, this.fixerUpper);
        this.commandHistory = new CommandHistory(path);
        this.soundManager = new SoundManager(this.options);
        this.resourceManager.registerReloadListener(this.soundManager);
        this.splashManager = new SplashManager(this.user);
        this.resourceManager.registerReloadListener(this.splashManager);
        this.musicManager = new MusicManager(this);
        this.fontManager = new FontManager(this.textureManager);
        this.font = this.fontManager.createFont();
        this.fontFilterFishy = this.fontManager.createFontFilterFishy();
        this.resourceManager.registerReloadListener(this.fontManager);
        this.updateFontOptions();
        this.resourceManager.registerReloadListener(new GrassColorReloadListener());
        this.resourceManager.registerReloadListener(new FoliageColorReloadListener());
        this.resourceManager.registerReloadListener(new DryFoliageColorReloadListener());
        this.window.setErrorSection("Startup");
        RenderSystem.setupDefaultState();
        this.window.setErrorSection("Post startup");
        this.blockColors = BlockColors.createDefault();
        this.modelManager = new ModelManager(this.textureManager, this.blockColors, this.options.mipmapLevels().get());
        this.resourceManager.registerReloadListener(this.modelManager);
        EquipmentAssetManager equipmentassetmanager = new EquipmentAssetManager();
        this.resourceManager.registerReloadListener(equipmentassetmanager);
        this.itemModelResolver = new ItemModelResolver(this.modelManager);
        this.itemRenderer = new ItemRenderer(this.itemModelResolver);
        this.mapTextureManager = new MapTextureManager(this.textureManager);
        this.mapDecorationTextures = new MapDecorationTextureManager(this.textureManager);
        this.resourceManager.registerReloadListener(this.mapDecorationTextures);
        this.mapRenderer = new MapRenderer(this.mapDecorationTextures, this.mapTextureManager);

        try {
            int i = Runtime.getRuntime().availableProcessors();
            Tesselator.init();
            this.renderBuffers = new RenderBuffers(i);
        } catch (OutOfMemoryError outofmemoryerror) {
            TinyFileDialogs.tinyfd_messageBox(
                "Minecraft",
                "Oh no! The game was unable to allocate memory off-heap while trying to start. You may try to free some memory by closing other applications on your computer, check that your system meets the minimum requirements, and try again. If the problem persists, please visit: "
                    + CommonLinks.GENERAL_HELP,
                "ok",
                "error",
                true
            );
            throw new SilentInitException("Unable to allocate render buffers", outofmemoryerror);
        }

        this.playerSocialManager = new PlayerSocialManager(this, this.userApiService);
        this.blockRenderer = new BlockRenderDispatcher(this.modelManager.getBlockModelShaper(), this.modelManager.specialBlockModelRenderer(), this.blockColors);
        this.resourceManager.registerReloadListener(this.blockRenderer);
        this.entityRenderDispatcher = new EntityRenderDispatcher(
            this,
            this.textureManager,
            this.itemModelResolver,
            this.itemRenderer,
            this.mapRenderer,
            this.blockRenderer,
            this.font,
            this.options,
            this.modelManager.entityModels(),
            equipmentassetmanager
        );
        this.resourceManager.registerReloadListener(this.entityRenderDispatcher);
        this.blockEntityRenderDispatcher = new BlockEntityRenderDispatcher(this.font, this.modelManager.entityModels(), this.blockRenderer, this.itemModelResolver, this.itemRenderer, this.entityRenderDispatcher);
        this.resourceManager.registerReloadListener(this.blockEntityRenderDispatcher);
        this.particleEngine = new ParticleEngine(this.level, this.textureManager);
        net.minecraftforge.client.ForgeHooksClient.onRegisterParticleProviders(this.particleEngine);
        this.resourceManager.registerReloadListener(this.particleEngine);
        this.paintingTextures = new PaintingTextureManager(this.textureManager);
        this.resourceManager.registerReloadListener(this.paintingTextures);
        this.mobEffectTextures = new MobEffectTextureManager(this.textureManager);
        this.resourceManager.registerReloadListener(this.mobEffectTextures);
        this.guiSprites = new GuiSpriteManager(this.textureManager);
        this.resourceManager.registerReloadListener(this.guiSprites);
        this.gameRenderer = new GameRenderer(this, this.entityRenderDispatcher.getItemInHandRenderer(), this.resourceManager, this.renderBuffers);
        this.levelRenderer = new LevelRenderer(this, this.entityRenderDispatcher, this.blockEntityRenderDispatcher, this.renderBuffers);
        this.resourceManager.registerReloadListener(this.levelRenderer);
        this.resourceManager.registerReloadListener(this.levelRenderer.getCloudRenderer());
        this.gpuWarnlistManager = new GpuWarnlistManager();
        this.resourceManager.registerReloadListener(this.gpuWarnlistManager);
        this.resourceManager.registerReloadListener(this.regionalCompliancies);
        this.gui = new Gui(this);
        // FORGE: Moved keyboard and mouse handler setup below ingame gui creation to prevent NPEs in them.
        this.mouseHandler.setup(this.window.getWindow());
        this.keyboardHandler.setup(this.window.getWindow());
        this.debugRenderer = new DebugRenderer(this);
        RealmsClient realmsclient = RealmsClient.getOrCreate(this);
        this.realmsDataFetcher = new RealmsDataFetcher(realmsclient);
        RenderSystem.setErrorCallback(this::onFullscreenError);
        if (this.mainRenderTarget.width != this.window.getWidth() || this.mainRenderTarget.height != this.window.getHeight()) {
            StringBuilder stringbuilder = new StringBuilder(
                "Recovering from unsupported resolution ("
                    + this.window.getWidth()
                    + "x"
                    + this.window.getHeight()
                    + ").\nPlease make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions)."
            );

            try {
                GpuDevice gpudevice = RenderSystem.getDevice();
                List<String> list = gpudevice.getLastDebugMessages();
                if (!list.isEmpty()) {
                    stringbuilder.append("\n\nReported GL debug messages:\n").append(String.join("\n", list));
                }
            } catch (Throwable throwable) {
            }

            this.window.setWindowed(this.mainRenderTarget.width, this.mainRenderTarget.height);
            TinyFileDialogs.tinyfd_messageBox("Minecraft", stringbuilder.toString(), "ok", "error", false);
        } else if (this.options.fullscreen().get() && !this.window.isFullscreen()) {
            if (flag) {
                this.window.toggleFullScreen();
                this.options.fullscreen().set(this.window.isFullscreen());
            } else {
                this.options.fullscreen().set(false);
            }
        }

        net.minecraftforge.client.ForgeHooksClient.initClientHooks(this, this.resourceManager);
        this.window.updateVsync(this.options.enableVsync().get());
        this.window.updateRawMouseInput(this.options.rawMouseInput().get());
        this.window.setDefaultErrorCallback();
        this.resizeDisplay();
        this.gameRenderer.preloadUiShader(this.vanillaPackResources.asProvider());
        this.telemetryManager = new ClientTelemetryManager(this, this.userApiService, this.user);
        this.profileKeyPairManager = ProfileKeyPairManager.create(this.userApiService, this.user, path);
        this.narrator = new GameNarrator(this);
        this.narrator.checkStatus(this.options.narrator().get() != NarratorStatus.OFF);
        this.chatListener = new ChatListener(this);
        this.chatListener.setMessageDelay(this.options.chatDelay().get());
        this.reportingContext = ReportingContext.create(ReportEnvironment.local(), this.userApiService);
        TitleScreen.registerTextures(this.textureManager);
        LoadingOverlay.registerTextures(this.textureManager);
        this.setScreen(new GenericMessageScreen(Component.translatable("gui.loadingMinecraft")));
        List<PackResources> list1 = this.resourcePackRepository.openAllSelected();
        this.reloadStateTracker.startReload(ResourceLoadStateTracker.ReloadReason.INITIAL, list1);
        ReloadInstance reloadinstance = this.resourceManager.createReload(Util.backgroundExecutor().forName("resourceLoad"), this, RESOURCE_RELOAD_INITIAL_TASK, list1);
        GameLoadTimesEvent.INSTANCE.beginStep(TelemetryProperty.LOAD_TIME_LOADING_OVERLAY_MS);
        Minecraft.GameLoadCookie minecraft$gameloadcookie = new Minecraft.GameLoadCookie(realmsclient, pGameConfig.quickPlay);
        this.setOverlay(
            net.minecraftforge.fml.loading.ImmediateWindowHandler.<LoadingOverlay>loadingOverlay(
                () -> this, () -> reloadinstance, p_296164_ -> Util.ifElse(p_296164_, p_296162_ -> this.rollbackResourcePacks(p_296162_, minecraft$gameloadcookie), () -> {
                    if (SharedConstants.IS_RUNNING_IN_IDE) {
                        this.selfTest();
                    }

                    this.reloadStateTracker.finishReload();
                    this.onResourceLoadFinished(minecraft$gameloadcookie);
                    net.minecraftforge.client.loading.ClientModLoader.completeModLoading();
                }), false
            ).get()
        );
        this.quickPlayLog = QuickPlayLog.of(pGameConfig.quickPlay.path());
        this.framerateLimitTracker = new FramerateLimitTracker(this.options, this);
        this.fpsPieProfiler = new ContinuousProfiler(Util.timeSource, () -> this.fpsPieRenderTicks, this.framerateLimitTracker::isHeavilyThrottled);
        if (TracyClient.isAvailable() && pGameConfig.game.captureTracyImages) {
            this.tracyFrameCapture = new TracyFrameCapture();
        } else {
            this.tracyFrameCapture = null;
        }
    }

    private void onResourceLoadFinished(@Nullable Minecraft.GameLoadCookie pGameLoadCookie) {
        if (!this.gameLoadFinished) {
            this.gameLoadFinished = true;
            this.onGameLoadFinished(pGameLoadCookie);
        }
    }

    private void onGameLoadFinished(@Nullable Minecraft.GameLoadCookie pGameLoadCookie) {
        Runnable runnable = this.buildInitialScreens(pGameLoadCookie);
        GameLoadTimesEvent.INSTANCE.endStep(TelemetryProperty.LOAD_TIME_LOADING_OVERLAY_MS);
        GameLoadTimesEvent.INSTANCE.endStep(TelemetryProperty.LOAD_TIME_TOTAL_TIME_MS);
        GameLoadTimesEvent.INSTANCE.send(this.telemetryManager.getOutsideSessionSender());
        runnable.run();
        this.options.startedCleanly = true;
        this.options.save();
    }

    public boolean isGameLoadFinished() {
        return this.gameLoadFinished;
    }

    private Runnable buildInitialScreens(@Nullable Minecraft.GameLoadCookie pGameLoadCookie) {
        List<Function<Runnable, Screen>> list = new ArrayList<>();
        this.addInitialScreens(list);
        Runnable runnable = () -> {
            if (pGameLoadCookie != null && pGameLoadCookie.quickPlayData().isEnabled()) {
                QuickPlay.connect(this, pGameLoadCookie.quickPlayData(), pGameLoadCookie.realmsClient());
            } else {
                this.setScreen(new TitleScreen(true));
            }
        };

        for (Function<Runnable, Screen> function : Lists.reverse(list)) {
            Screen screen = function.apply(runnable);
            runnable = () -> this.setScreen(screen);
        }

        return runnable;
    }

    private void addInitialScreens(List<Function<Runnable, Screen>> pOutput) {
        if (this.options.onboardAccessibility) {
            pOutput.add(p_296165_ -> new AccessibilityOnboardingScreen(this.options, p_296165_));
        }

        BanDetails bandetails = this.multiplayerBan();
        if (bandetails != null) {
            pOutput.add(p_296160_ -> BanNoticeScreens.create(p_340761_ -> {
                if (p_340761_) {
                    Util.getPlatform().openUri(CommonLinks.SUSPENSION_HELP);
                }

                p_296160_.run();
            }, bandetails));
        }

        ProfileResult profileresult = this.profileFuture.join();
        if (profileresult != null) {
            GameProfile gameprofile = profileresult.profile();
            Set<ProfileActionType> set = profileresult.actions();
            if (set.contains(ProfileActionType.FORCED_NAME_CHANGE)) {
                pOutput.add(p_296154_ -> BanNoticeScreens.createNameBan(gameprofile.getName(), p_296154_));
            }

            if (set.contains(ProfileActionType.USING_BANNED_SKIN)) {
                pOutput.add(BanNoticeScreens::createSkinBan);
            }
        }
    }

    private static boolean countryEqualsISO3(Object pCountry) {
        try {
            return Locale.getDefault().getISO3Country().equals(pCountry);
        } catch (MissingResourceException missingresourceexception) {
            return false;
        }
    }

    public void updateTitle() {
        this.window.setTitle(this.createTitle());
    }

    private String createTitle() {
        StringBuilder stringbuilder = new StringBuilder("Minecraft");
        if (checkModStatus().shouldReportAsModified()) {
            stringbuilder.append("*");
            stringbuilder.append(" Forge");
        }

        stringbuilder.append(" ");
        stringbuilder.append(SharedConstants.getCurrentVersion().getName());
        ClientPacketListener clientpacketlistener = this.getConnection();
        if (clientpacketlistener != null && clientpacketlistener.getConnection().isConnected()) {
            stringbuilder.append(" - ");
            ServerData serverdata = this.getCurrentServer();
            if (this.singleplayerServer != null && !this.singleplayerServer.isPublished()) {
                stringbuilder.append(I18n.get("title.singleplayer"));
            } else if (serverdata != null && serverdata.isRealm()) {
                stringbuilder.append(I18n.get("title.multiplayer.realms"));
            } else if (this.singleplayerServer == null && (serverdata == null || !serverdata.isLan())) {
                stringbuilder.append(I18n.get("title.multiplayer.other"));
            } else {
                stringbuilder.append(I18n.get("title.multiplayer.lan"));
            }
        }

        return stringbuilder.toString();
    }

    private UserApiService createUserApiService(YggdrasilAuthenticationService pAuthenticationService, GameConfig pGameConfig) {
        if ("0".equals(pGameConfig.user.user.getAccessToken())) // Forge: We use "0" in dev. Short circuit to stop exception spam.
            return UserApiService.OFFLINE;
        return pGameConfig.user.user.getType() != User.Type.MSA
            ? UserApiService.OFFLINE
            : pAuthenticationService.createUserApiService(pGameConfig.user.user.getAccessToken());
    }

    public static ModCheck checkModStatus() {
        return ModCheck.identify("vanilla", ClientBrandRetriever::getClientModName, "Client", Minecraft.class);
    }

    private void rollbackResourcePacks(Throwable pThrowable, @Nullable Minecraft.GameLoadCookie pGameLoadCookie) {
        if (this.resourcePackRepository.getSelectedPacks().stream().anyMatch(e -> !e.isRequired())) { //Forge: This caused infinite loop if any resource packs are forced. Such as mod resources. So check if we can disable any.
            this.clearResourcePacksOnError(pThrowable, null, pGameLoadCookie);
        } else {
            Util.throwAsRuntime(pThrowable);
        }
    }

    public void clearResourcePacksOnError(Throwable pThrowable, @Nullable Component pErrorMessage, @Nullable Minecraft.GameLoadCookie pGameLoadCookie) {
        LOGGER.info("Caught error loading resourcepacks, removing all selected resourcepacks", pThrowable);
        this.reloadStateTracker.startRecovery(pThrowable);
        this.downloadedPackSource.onRecovery();
        this.resourcePackRepository.setSelected(Collections.emptyList());
        this.options.resourcePacks.clear();
        this.options.incompatibleResourcePacks.clear();
        this.options.save();
        this.reloadResourcePacks(true, pGameLoadCookie).thenRunAsync(() -> this.addResourcePackLoadFailToast(pErrorMessage), this);
    }

    private void abortResourcePackRecovery() {
        this.setOverlay(null);
        if (this.level != null) {
            this.level.disconnect();
            this.disconnect();
        }

        this.setScreen(new TitleScreen());
        this.addResourcePackLoadFailToast(null);
    }

    private void addResourcePackLoadFailToast(@Nullable Component pMessage) {
        ToastManager toastmanager = this.getToastManager();
        SystemToast.addOrUpdate(toastmanager, SystemToast.SystemToastId.PACK_LOAD_FAILURE, Component.translatable("resourcePack.load_fail"), pMessage);
    }

    public void triggerResourcePackRecovery(Exception pError) {
        if (!this.resourcePackRepository.isAbleToClearAnyPack()) {
            if (this.resourcePackRepository.getSelectedIds().size() <= 1) {
                LOGGER.error(LogUtils.FATAL_MARKER, pError.getMessage(), (Throwable)pError);
                this.emergencySaveAndCrash(new CrashReport(pError.getMessage(), pError));
            } else {
                this.schedule(this::abortResourcePackRecovery);
            }
        } else {
            this.clearResourcePacksOnError(pError, Component.translatable("resourcePack.runtime_failure"), null);
        }
    }

    public void run() {
        this.gameThread = Thread.currentThread();
        if (Runtime.getRuntime().availableProcessors() > 4) {
            this.gameThread.setPriority(10);
        }

        DiscontinuousFrame discontinuousframe = TracyClient.createDiscontinuousFrame("Client Tick");

        try {
            boolean flag = false;

            while (this.running) {
                this.handleDelayedCrash();

                try {
                    SingleTickProfiler singletickprofiler = SingleTickProfiler.createTickProfiler("Renderer");
                    boolean flag1 = this.getDebugOverlay().showProfilerChart();

                    try (Profiler.Scope profiler$scope = Profiler.use(this.constructProfiler(flag1, singletickprofiler))) {
                        this.metricsRecorder.startTick();
                        discontinuousframe.start();
                        this.runTick(!flag);
                        discontinuousframe.end();
                        this.metricsRecorder.endTick();
                    }

                    this.finishProfilers(flag1, singletickprofiler);
                } catch (OutOfMemoryError outofmemoryerror) {
                    if (flag) {
                        throw outofmemoryerror;
                    }

                    this.emergencySave();
                    this.setScreen(new OutOfMemoryScreen());
                    System.gc();
                    LOGGER.error(LogUtils.FATAL_MARKER, "Out of memory", (Throwable)outofmemoryerror);
                    flag = true;
                }
            }
        } catch (ReportedException reportedexception) {
            LOGGER.error(LogUtils.FATAL_MARKER, "Reported exception thrown!", (Throwable)reportedexception);
            this.emergencySaveAndCrash(reportedexception.getReport());
        } catch (Throwable throwable1) {
            LOGGER.error(LogUtils.FATAL_MARKER, "Unreported exception thrown!", throwable1);
            this.emergencySaveAndCrash(new CrashReport("Unexpected error", throwable1));
        }
    }

    void updateFontOptions() {
        this.fontManager.updateOptions(this.options);
    }

    private void onFullscreenError(int pError, long pDescription) {
        this.options.enableVsync().set(false);
        this.options.save();
    }

    public RenderTarget getMainRenderTarget() {
        return this.mainRenderTarget;
    }

    public String getLaunchedVersion() {
        return this.launchedVersion;
    }

    public String getVersionType() {
        return this.versionType;
    }

    public void delayCrash(CrashReport pReport) {
        this.delayedCrash = () -> this.fillReport(pReport);
    }

    public void delayCrashRaw(CrashReport pReport) {
        this.delayedCrash = () -> pReport;
    }

    private void handleDelayedCrash() {
        if (this.delayedCrash != null) {
            crash(this, this.gameDirectory, this.delayedCrash.get());
        }
    }

    public void emergencySaveAndCrash(CrashReport pCrashReport) {
        MemoryReserve.release();
        CrashReport crashreport = this.fillReport(pCrashReport);
        this.emergencySave();
        crash(this, this.gameDirectory, crashreport);
    }

    public static int saveReport(File pFile, CrashReport pCrashReport) {
        Path path = pFile.toPath().resolve("crash-reports");
        Path path1 = path.resolve("crash-" + Util.getFilenameFormattedDateTime() + "-client.txt");
        Bootstrap.realStdoutPrintln(pCrashReport.getFriendlyReport(ReportType.CRASH));
        if (pCrashReport.getSaveFile() != null) {
            Bootstrap.realStdoutPrintln("#@!@# Game crashed! Crash report saved to: #@!@# " + pCrashReport.getSaveFile().toAbsolutePath());
            return -1;
        } else if (pCrashReport.saveToFile(path1, ReportType.CRASH)) {
            Bootstrap.realStdoutPrintln("#@!@# Game crashed! Crash report saved to: #@!@# " + path1.toAbsolutePath());
            return -1;
        } else {
            Bootstrap.realStdoutPrintln("#@?@# Game crashed! Crash report could not be saved. #@?@#");
            return -2;
        }
    }

    public static void crash(@Nullable Minecraft pMinecraft, File pGameDirectory, CrashReport pCrashReport) {
        int i = saveReport(pGameDirectory, pCrashReport);
        if (pMinecraft != null) {
            pMinecraft.soundManager.emergencyShutdown();
        }

        System.exit(i);
    }

    public boolean isEnforceUnicode() {
        return this.options.forceUnicodeFont().get();
    }

    public CompletableFuture<Void> reloadResourcePacks() {
        return this.reloadResourcePacks(false, null);
    }

    private CompletableFuture<Void> reloadResourcePacks(boolean pError, @Nullable Minecraft.GameLoadCookie pGameLoadCookie) {
        if (this.pendingReload != null) {
            return this.pendingReload;
        } else {
            CompletableFuture<Void> completablefuture = new CompletableFuture<>();
            if (!pError && this.overlay instanceof LoadingOverlay) {
                this.pendingReload = completablefuture;
                return completablefuture;
            } else {
                this.resourcePackRepository.reload();
                List<PackResources> list = this.resourcePackRepository.openAllSelected();
                if (!pError) {
                    this.reloadStateTracker.startReload(ResourceLoadStateTracker.ReloadReason.MANUAL, list);
                }

                this.setOverlay(
                    new LoadingOverlay(
                        this,
                        this.resourceManager.createReload(Util.backgroundExecutor().forName("resourceLoad"), this, RESOURCE_RELOAD_INITIAL_TASK, list),
                        p_296158_ -> Util.ifElse(p_296158_, p_308166_ -> {
                            if (pError) {
                                this.downloadedPackSource.onRecoveryFailure();
                                this.abortResourcePackRecovery();
                            } else {
                                this.rollbackResourcePacks(p_308166_, pGameLoadCookie);
                            }
                        }, () -> {
                            this.levelRenderer.allChanged();
                            this.reloadStateTracker.finishReload();
                            this.downloadedPackSource.onReloadSuccess();
                            completablefuture.complete(null);
                            this.onResourceLoadFinished(pGameLoadCookie);
                        }),
                        !pError
                    )
                );
                return completablefuture;
            }
        }
    }

    private void selfTest() {
        boolean flag = false;
        BlockModelShaper blockmodelshaper = this.getBlockRenderer().getBlockModelShaper();
        BlockStateModel blockstatemodel = blockmodelshaper.getModelManager().getMissingBlockStateModel();

        for (Block block : BuiltInRegistries.BLOCK) {
            for (BlockState blockstate : block.getStateDefinition().getPossibleStates()) {
                if (blockstate.getRenderShape() == RenderShape.MODEL) {
                    BlockStateModel blockstatemodel1 = blockmodelshaper.getBlockModel(blockstate);
                    if (blockstatemodel1 == blockstatemodel) {
                        LOGGER.debug("Missing model for: {}", blockstate);
                        flag = true;
                    }
                }
            }
        }

        TextureAtlasSprite textureatlassprite1 = blockstatemodel.particleIcon();

        for (Block block1 : BuiltInRegistries.BLOCK) {
            for (BlockState blockstate1 : block1.getStateDefinition().getPossibleStates()) {
                TextureAtlasSprite textureatlassprite = blockmodelshaper.getParticleIcon(blockstate1);
                if (!blockstate1.isAir() && textureatlassprite == textureatlassprite1) {
                    LOGGER.debug("Missing particle icon for: {}", blockstate1);
                }
            }
        }

        BuiltInRegistries.ITEM.listElements().forEach(p_357641_ -> {
            Item item = p_357641_.value();
            String s = item.getDescriptionId();
            String s1 = Component.translatable(s).getString();
            if (s1.toLowerCase(Locale.ROOT).equals(item.getDescriptionId())) {
                LOGGER.debug("Missing translation for: {} {} {}", p_357641_.key().location(), s, item);
            }
        });
        flag |= MenuScreens.selfTest();
        flag |= EntityRenderers.validateRegistrations();
        if (flag) {
            throw new IllegalStateException("Your game data is foobar, fix the errors above!");
        }
    }

    public LevelStorageSource getLevelSource() {
        return this.levelSource;
    }

    private void openChatScreen(String pDefaultText) {
        Minecraft.ChatStatus minecraft$chatstatus = this.getChatStatus();
        if (!minecraft$chatstatus.isChatAllowed(this.isLocalServer())) {
            if (this.gui.isShowingChatDisabledByPlayer()) {
                this.gui.setChatDisabledByPlayerShown(false);
                this.setScreen(new ConfirmLinkScreen(p_340762_ -> {
                    if (p_340762_) {
                        Util.getPlatform().openUri(CommonLinks.ACCOUNT_SETTINGS);
                    }

                    this.setScreen(null);
                }, Minecraft.ChatStatus.INFO_DISABLED_BY_PROFILE, CommonLinks.ACCOUNT_SETTINGS, true));
            } else {
                Component component = minecraft$chatstatus.getMessage();
                this.gui.setOverlayMessage(component, false);
                this.narrator.sayNow(component);
                this.gui.setChatDisabledByPlayerShown(minecraft$chatstatus == Minecraft.ChatStatus.DISABLED_BY_PROFILE);
            }
        } else {
            this.setScreen(new ChatScreen(pDefaultText));
        }
    }

    public void setScreen(@Nullable Screen pGuiScreen) {
        if (SharedConstants.IS_RUNNING_IN_IDE && Thread.currentThread() != this.gameThread) {
            LOGGER.error("setScreen called from non-game thread");
        }

        if (pGuiScreen == null && this.clientLevelTeardownInProgress) {
            throw new IllegalStateException("Trying to return to in-game GUI during disconnection");
        } else {
            if (pGuiScreen == null && this.level == null) {
                pGuiScreen = new TitleScreen();
            } else if (pGuiScreen == null && this.player.isDeadOrDying()) {
                if (this.player.shouldShowDeathScreen()) {
                    pGuiScreen = new DeathScreen(null, this.level.getLevelData().isHardcore());
                } else {
                    this.player.respawn();
                }
            }

            net.minecraftforge.client.ForgeHooksClient.clearGuiLayers(this);
            Screen old = this.screen;
            if (pGuiScreen != null) {
               var event = net.minecraftforge.client.event.ForgeEventFactoryClient.onScreenOpening(old, pGuiScreen);
               if (event == null) return;
               pGuiScreen = event;
            }

            if (pGuiScreen != old) {
                if (old != null) {
                    net.minecraftforge.client.event.ForgeEventFactoryClient.onScreenClose(old);
                    old.removed();
                } else {
                    this.setLastInputType(InputType.NONE);
                }
            }

            this.screen = pGuiScreen;
            if (this.screen != null) {
                this.screen.added();
            }

            if (pGuiScreen != null) {
                this.mouseHandler.releaseMouse();
                KeyMapping.releaseAll();
                pGuiScreen.init(this, this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
                this.noRender = false;
            } else {
                this.soundManager.resume();
                this.mouseHandler.grabMouse();
            }

            this.updateTitle();
        }
    }

    public void setOverlay(@Nullable Overlay pLoadingGui) {
        this.overlay = pLoadingGui;
    }

    public void destroy() {
        try {
            LOGGER.info("Stopping!");

            try {
                this.narrator.destroy();
            } catch (Throwable throwable1) {
            }

            try {
                if (this.level != null) {
                    this.level.disconnect();
                }

                this.disconnect();
            } catch (Throwable throwable) {
            }

            if (this.screen != null) {
                this.screen.removed();
            }

            this.close();
        } finally {
            Util.timeSource = System::nanoTime;
            if (this.delayedCrash == null) {
                System.exit(0);
            }
        }
    }

    @Override
    public void close() {
        if (this.currentFrameProfile != null) {
            this.currentFrameProfile.cancel();
        }

        try {
            this.telemetryManager.close();
            this.regionalCompliancies.close();
            this.modelManager.close();
            this.fontManager.close();
            this.gameRenderer.close();
            this.shaderManager.close();
            this.levelRenderer.close();
            this.soundManager.destroy();
            this.particleEngine.close();
            this.mobEffectTextures.close();
            this.paintingTextures.close();
            this.mapDecorationTextures.close();
            this.guiSprites.close();
            this.mapTextureManager.close();
            this.textureManager.close();
            this.resourceManager.close();
            if (this.tracyFrameCapture != null) {
                this.tracyFrameCapture.close();
            }

            FreeTypeUtil.destroy();
            Util.shutdownExecutors();
            RenderSystem.getDevice().close();
            net.minecraftforge.fml.config.ConfigTracker.INSTANCE.forceUnload();
        } catch (Throwable throwable) {
            LOGGER.error("Shutdown failure!", throwable);
            throw throwable;
        } finally {
            this.virtualScreen.close();
            this.window.close();
        }
    }

    private void runTick(boolean pRenderLevel) {
        this.window.setErrorSection("Pre render");
        if (this.window.shouldClose()) {
            this.stop();
        }

        if (this.pendingReload != null && !(this.overlay instanceof LoadingOverlay)) {
            CompletableFuture<Void> completablefuture = this.pendingReload;
            this.pendingReload = null;
            this.reloadResourcePacks().thenRun(() -> completablefuture.complete(null));
        }

        Runnable runnable;
        while ((runnable = this.progressTasks.poll()) != null) {
            runnable.run();
        }

        int i = this.deltaTracker.advanceTime(Util.getMillis(), pRenderLevel);
        ProfilerFiller profilerfiller = Profiler.get();
        if (pRenderLevel) {
            profilerfiller.push("scheduledExecutables");
            this.runAllTasks();
            profilerfiller.pop();
            profilerfiller.push("tick");

            for (int j = 0; j < Math.min(10, i); j++) {
                profilerfiller.incrementCounter("clientTick");
                this.tick();
            }

            profilerfiller.pop();
        }

        this.window.setErrorSection("Render");
        profilerfiller.push("gpuAsync");
        RenderSystem.executePendingTasks();
        profilerfiller.popPush("sound");
        this.soundManager.updateSource(this.gameRenderer.getMainCamera());
        profilerfiller.popPush("toasts");
        this.toastManager.update();
        profilerfiller.popPush("render");
        long j1 = Util.getNanos();
        boolean flag;
        if (!this.getDebugOverlay().showDebugScreen() && !this.metricsRecorder.isRecording()) {
            flag = false;
            this.gpuUtilization = 0.0;
        } else {
            flag = this.currentFrameProfile == null || this.currentFrameProfile.isDone();
            if (flag) {
                TimerQuery.getInstance().ifPresent(TimerQuery::beginProfile);
            }
        }

        RenderTarget rendertarget = this.getMainRenderTarget();
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(rendertarget.getColorTexture(), 0, rendertarget.getDepthTexture(), 1.0);
        RenderSystem.setShaderFog(FogParameters.NO_FOG);
        profilerfiller.push("mouse");
        this.mouseHandler.handleAccumulatedMovement();
        profilerfiller.pop();
        if (!this.noRender) {
            net.minecraftforge.client.event.ForgeEventFactoryClient.onRenderTickStart(this.deltaTracker);
            profilerfiller.popPush("gameRenderer");
            this.gameRenderer.render(this.deltaTracker, pRenderLevel);
            profilerfiller.pop();
            net.minecraftforge.client.event.ForgeEventFactoryClient.onRenderTickEnd(this.deltaTracker);
        }

        profilerfiller.push("blit");
        if (!this.window.isMinimized()) {
            rendertarget.blitToScreen();
        }

        this.frameTimeNs = Util.getNanos() - j1;
        if (flag) {
            TimerQuery.getInstance().ifPresent(p_231363_ -> this.currentFrameProfile = p_231363_.endProfile());
        }

        profilerfiller.popPush("updateDisplay");
        if (this.tracyFrameCapture != null) {
            this.tracyFrameCapture.upload();
            this.tracyFrameCapture.capture(rendertarget);
        }

        this.window.updateDisplay(this.tracyFrameCapture);
        int k = this.framerateLimitTracker.getFramerateLimit();
        if (k < 260) {
            RenderSystem.limitDisplayFPS(k);
        }

        profilerfiller.popPush("yield");
        Thread.yield();
        profilerfiller.pop();
        this.window.setErrorSection("Post render");
        this.frames++;
        this.pause = this.hasSingleplayerServer()
            && (this.screen != null && this.screen.isPauseScreen() || this.overlay != null && this.overlay.isPauseScreen())
            && !this.singleplayerServer.isPublished();
        this.deltaTracker.updatePauseState(this.pause);
        this.deltaTracker.updateFrozenState(!this.isLevelRunningNormally());
        long l = Util.getNanos();
        long i1 = l - this.lastNanoTime;
        if (flag) {
            this.savedCpuDuration = i1;
        }

        this.getDebugOverlay().logFrameDuration(i1);
        this.lastNanoTime = l;
        profilerfiller.push("fpsUpdate");
        if (this.currentFrameProfile != null && this.currentFrameProfile.isDone()) {
            this.gpuUtilization = this.currentFrameProfile.get() * 100.0 / this.savedCpuDuration;
        }

        while (Util.getMillis() >= this.lastTime + 1000L) {
            String s;
            if (this.gpuUtilization > 0.0) {
                s = " GPU: " + (this.gpuUtilization > 100.0 ? ChatFormatting.RED + "100%" : Math.round(this.gpuUtilization) + "%");
            } else {
                s = "";
            }

            fps = this.frames;
            this.fpsString = String.format(
                Locale.ROOT,
                "%d fps T: %s%s%s%s B: %d%s",
                fps,
                k == 260 ? "inf" : k,
                this.options.enableVsync().get() ? " vsync " : " ",
                this.options.graphicsMode().get(),
                this.options.cloudStatus().get() == CloudStatus.OFF
                    ? ""
                    : (this.options.cloudStatus().get() == CloudStatus.FAST ? " fast-clouds" : " fancy-clouds"),
                this.options.biomeBlendRadius().get(),
                s
            );
            this.lastTime += 1000L;
            this.frames = 0;
        }

        profilerfiller.pop();
    }

    private ProfilerFiller constructProfiler(boolean pRenderFpsPie, @Nullable SingleTickProfiler pSingleTickProfiler) {
        if (!pRenderFpsPie) {
            this.fpsPieProfiler.disable();
            if (!this.metricsRecorder.isRecording() && pSingleTickProfiler == null) {
                return InactiveProfiler.INSTANCE;
            }
        }

        ProfilerFiller profilerfiller;
        if (pRenderFpsPie) {
            if (!this.fpsPieProfiler.isEnabled()) {
                this.fpsPieRenderTicks = 0;
                this.fpsPieProfiler.enable();
            }

            this.fpsPieRenderTicks++;
            profilerfiller = this.fpsPieProfiler.getFiller();
        } else {
            profilerfiller = InactiveProfiler.INSTANCE;
        }

        if (this.metricsRecorder.isRecording()) {
            profilerfiller = ProfilerFiller.combine(profilerfiller, this.metricsRecorder.getProfiler());
        }

        return SingleTickProfiler.decorateFiller(profilerfiller, pSingleTickProfiler);
    }

    private void finishProfilers(boolean pRenderFpsPie, @Nullable SingleTickProfiler pProfiler) {
        if (pProfiler != null) {
            pProfiler.endTick();
        }

        ProfilerPieChart profilerpiechart = this.getDebugOverlay().getProfilerPieChart();
        if (pRenderFpsPie) {
            profilerpiechart.setPieChartResults(this.fpsPieProfiler.getResults());
        } else {
            profilerpiechart.setPieChartResults(null);
        }
    }

    @Override
    public void resizeDisplay() {
        int i = this.window.calculateScale(this.options.guiScale().get(), this.isEnforceUnicode());
        this.window.setGuiScale(i);
        if (this.screen != null) {
            this.screen.resize(this, this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
            net.minecraftforge.client.ForgeHooksClient.resizeGuiLayers(this, this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
        }

        RenderTarget rendertarget = this.getMainRenderTarget();
        rendertarget.resize(this.window.getWidth(), this.window.getHeight());
        if (this.gameRenderer != null)
        this.gameRenderer.resize(this.window.getWidth(), this.window.getHeight());
        this.mouseHandler.setIgnoreFirstMove();
    }

    @Override
    public void cursorEntered() {
        this.mouseHandler.cursorEntered();
    }

    public int getFps() {
        return fps;
    }

    public long getFrameTimeNs() {
        return this.frameTimeNs;
    }

    private void emergencySave() {
        MemoryReserve.release();

        try {
            if (this.isLocalServer && this.singleplayerServer != null) {
                this.singleplayerServer.halt(true);
            }

            this.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
        } catch (Throwable throwable) {
        }

        System.gc();
    }

    public boolean debugClientMetricsStart(Consumer<Component> pLogger) {
        if (this.metricsRecorder.isRecording()) {
            this.debugClientMetricsStop();
            return false;
        } else {
            Consumer<ProfileResults> consumer = p_231435_ -> {
                if (p_231435_ != EmptyProfileResults.EMPTY) {
                    int i = p_231435_.getTickDuration();
                    double d0 = (double)p_231435_.getNanoDuration() / TimeUtil.NANOSECONDS_PER_SECOND;
                    this.execute(
                        () -> pLogger.accept(
                            Component.translatable("commands.debug.stopped", String.format(Locale.ROOT, "%.2f", d0), i, String.format(Locale.ROOT, "%.2f", i / d0))
                        )
                    );
                }
            };
            Consumer<Path> consumer1 = p_231438_ -> {
                Component component = Component.literal(p_231438_.toString())
                    .withStyle(ChatFormatting.UNDERLINE)
                    .withStyle(p_389132_ -> p_389132_.withClickEvent(new ClickEvent.OpenFile(p_231438_.getParent())));
                this.execute(() -> pLogger.accept(Component.translatable("debug.profiling.stop", component)));
            };
            SystemReport systemreport = fillSystemReport(new SystemReport(), this, this.languageManager, this.launchedVersion, this.options);
            Consumer<List<Path>> consumer2 = p_231349_ -> {
                Path path = this.archiveProfilingReport(systemreport, p_231349_);
                consumer1.accept(path);
            };
            Consumer<Path> consumer3;
            if (this.singleplayerServer == null) {
                consumer3 = p_231404_ -> consumer2.accept(ImmutableList.of(p_231404_));
            } else {
                this.singleplayerServer.fillSystemReport(systemreport);
                CompletableFuture<Path> completablefuture = new CompletableFuture<>();
                CompletableFuture<Path> completablefuture1 = new CompletableFuture<>();
                CompletableFuture.allOf(completablefuture, completablefuture1)
                    .thenRunAsync(() -> consumer2.accept(ImmutableList.of(completablefuture.join(), completablefuture1.join())), Util.ioPool());
                this.singleplayerServer.startRecordingMetrics(p_231351_ -> {}, completablefuture1::complete);
                consumer3 = completablefuture::complete;
            }

            this.metricsRecorder = ActiveMetricsRecorder.createStarted(
                new ClientMetricsSamplersProvider(Util.timeSource, this.levelRenderer),
                Util.timeSource,
                Util.ioPool(),
                new MetricsPersister("client"),
                p_231401_ -> {
                    this.metricsRecorder = InactiveMetricsRecorder.INSTANCE;
                    consumer.accept(p_231401_);
                },
                consumer3
            );
            return true;
        }
    }

    private void debugClientMetricsStop() {
        this.metricsRecorder.end();
        if (this.singleplayerServer != null) {
            this.singleplayerServer.finishRecordingMetrics();
        }
    }

    private void debugClientMetricsCancel() {
        this.metricsRecorder.cancel();
        if (this.singleplayerServer != null) {
            this.singleplayerServer.cancelRecordingMetrics();
        }
    }

    private Path archiveProfilingReport(SystemReport pReport, List<Path> pPaths) {
        String s;
        if (this.isLocalServer()) {
            s = this.getSingleplayerServer().getWorldData().getLevelName();
        } else {
            ServerData serverdata = this.getCurrentServer();
            s = serverdata != null ? serverdata.name : "unknown";
        }

        Path path;
        try {
            String s2 = String.format(Locale.ROOT, "%s-%s-%s", Util.getFilenameFormattedDateTime(), s, SharedConstants.getCurrentVersion().getId());
            String s1 = FileUtil.findAvailableName(MetricsPersister.PROFILING_RESULTS_DIR, s2, ".zip");
            path = MetricsPersister.PROFILING_RESULTS_DIR.resolve(s1);
        } catch (IOException ioexception1) {
            throw new UncheckedIOException(ioexception1);
        }

        try (FileZipper filezipper = new FileZipper(path)) {
            filezipper.add(Paths.get("system.txt"), pReport.toLineSeparatedString());
            filezipper.add(Paths.get("client").resolve(this.options.getFile().getName()), this.options.dumpOptionsForReport());
            pPaths.forEach(filezipper::add);
        } finally {
            for (Path path1 : pPaths) {
                try {
                    FileUtils.forceDelete(path1.toFile());
                } catch (IOException ioexception) {
                    LOGGER.warn("Failed to delete temporary profiling result {}", path1, ioexception);
                }
            }
        }

        return path;
    }

    public void stop() {
        if (this.isRunning()) net.minecraftforge.event.ForgeEventFactory.onGameShuttingDown();
        this.running = false;
    }

    public boolean isRunning() {
        return this.running;
    }

    public void pauseGame(boolean pPauseOnly) {
        if (this.screen == null) {
            boolean flag = this.hasSingleplayerServer() && !this.singleplayerServer.isPublished();
            if (flag) {
                this.setScreen(new PauseScreen(!pPauseOnly));
                this.soundManager.pause();
            } else {
                this.setScreen(new PauseScreen(true));
            }
        }
    }

    private void continueAttack(boolean pLeftClick) {
        if (!pLeftClick) {
            this.missTime = 0;
        }

        if (this.missTime <= 0 && !this.player.isUsingItem()) {
            if (pLeftClick && this.hitResult != null && this.hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockhitresult = (BlockHitResult)this.hitResult;
                BlockPos blockpos = blockhitresult.getBlockPos();
                if (!this.level.isEmptyBlock(blockpos)) {
                    var inputEvent = net.minecraftforge.client.event.ForgeEventFactoryClient.onClickInput(0, this.options.keyAttack, InteractionHand.MAIN_HAND);
                    if (inputEvent.isCanceled()) {
                       if (inputEvent.shouldSwingHand()) {
                          this.particleEngine.addBlockHitEffects(blockpos, blockhitresult);
                          this.player.swing(InteractionHand.MAIN_HAND);
                       }
                       return;
                    }
                    Direction direction = blockhitresult.getDirection();
                    if (this.gameMode.continueDestroyBlock(blockpos, direction) && inputEvent.shouldSwingHand()) {
                        this.particleEngine.addBlockHitEffects(blockpos, blockhitresult);
                        this.player.swing(InteractionHand.MAIN_HAND);
                    }
                }
            } else {
                this.gameMode.stopDestroyBlock();
            }
        }
    }

    private boolean startAttack() {
        if (this.missTime > 0) {
            return false;
        } else if (this.hitResult == null) {
            LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");
            if (this.gameMode.hasMissTime()) {
                this.missTime = 10;
            }

            return false;
        } else if (this.player.isHandsBusy()) {
            return false;
        } else {
            ItemStack itemstack = this.player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!itemstack.isItemEnabled(this.level.enabledFeatures())) {
                return false;
            } else {
                boolean flag = false;
                var inputEvent = net.minecraftforge.client.event.ForgeEventFactoryClient.onClickInput(0, this.options.keyAttack, InteractionHand.MAIN_HAND);
                if (!inputEvent.isCanceled())
                switch (this.hitResult.getType()) {
                    case ENTITY:
                        this.gameMode.attack(this.player, ((EntityHitResult)this.hitResult).getEntity());
                        break;
                    case BLOCK:
                        BlockHitResult blockhitresult = (BlockHitResult)this.hitResult;
                        BlockPos blockpos = blockhitresult.getBlockPos();
                        if (!this.level.isEmptyBlock(blockpos)) {
                            this.gameMode.startDestroyBlock(blockpos, blockhitresult.getDirection());
                            if (this.level.getBlockState(blockpos).isAir()) {
                                flag = true;
                            }
                            break;
                        }
                    case MISS:
                        if (this.gameMode.hasMissTime()) {
                            this.missTime = 10;
                        }

                        this.player.resetAttackStrengthTicker();
                        net.minecraftforge.event.ForgeEventFactory.onLeftClickEmpty(this.player);
                }

                if (inputEvent.shouldSwingHand())
                this.player.swing(InteractionHand.MAIN_HAND);
                return flag;
            }
        }
    }

    private void startUseItem() {
        if (!this.gameMode.isDestroying()) {
            this.rightClickDelay = 4;
            if (!this.player.isHandsBusy()) {
                if (this.hitResult == null) {
                    LOGGER.warn("Null returned as 'hitResult', this shouldn't happen!");
                }

                for (InteractionHand interactionhand : InteractionHand.values()) {
                    var inputEvent = net.minecraftforge.client.event.ForgeEventFactoryClient.onClickInput(1, this.options.keyUse, interactionhand);
                    if (inputEvent.isCanceled()) {
                       if (inputEvent.shouldSwingHand()) this.player.swing(interactionhand);
                       return;
                    }

                    ItemStack itemstack = this.player.getItemInHand(interactionhand);
                    if (!itemstack.isItemEnabled(this.level.enabledFeatures())) {
                        return;
                    }

                    if (this.hitResult != null) {
                        switch (this.hitResult.getType()) {
                            case ENTITY:
                                EntityHitResult entityhitresult = (EntityHitResult)this.hitResult;
                                Entity entity = entityhitresult.getEntity();
                                if (!this.level.getWorldBorder().isWithinBounds(entity.blockPosition())) {
                                    return;
                                }

                                InteractionResult interactionresult = this.gameMode.interactAt(this.player, entity, entityhitresult, interactionhand);
                                if (!interactionresult.consumesAction()) {
                                    interactionresult = this.gameMode.interact(this.player, entity, interactionhand);
                                }

                                if (interactionresult instanceof InteractionResult.Success interactionresult$success2) {
                                    if (interactionresult$success2.swingSource() == InteractionResult.SwingSource.CLIENT && inputEvent.shouldSwingHand()) {
                                        this.player.swing(interactionhand);
                                    }

                                    return;
                                }
                                break;
                            case BLOCK:
                                BlockHitResult blockhitresult = (BlockHitResult)this.hitResult;
                                int i = itemstack.getCount();
                                InteractionResult interactionresult1 = this.gameMode.useItemOn(this.player, interactionhand, blockhitresult);
                                if (interactionresult1 instanceof InteractionResult.Success interactionresult$success) {
                                    if (interactionresult$success.swingSource() == InteractionResult.SwingSource.CLIENT && inputEvent.shouldSwingHand()) {
                                        this.player.swing(interactionhand);
                                        if (!itemstack.isEmpty() && (itemstack.getCount() != i || this.player.hasInfiniteMaterials())) {
                                            this.gameRenderer.itemInHandRenderer.itemUsed(interactionhand);
                                        }
                                    }

                                    return;
                                }

                                if (interactionresult1 instanceof InteractionResult.Fail) {
                                    return;
                                }
                        }
                    }

                    if (itemstack.isEmpty() && (this.hitResult == null || this.hitResult.getType() == HitResult.Type.MISS))
                       net.minecraftforge.event.ForgeEventFactory.onRightClickEmpty(this.player, interactionhand);

                    if (!itemstack.isEmpty()
                        && this.gameMode.useItem(this.player, interactionhand) instanceof InteractionResult.Success interactionresult$success1) {
                        if (interactionresult$success1.swingSource() == InteractionResult.SwingSource.CLIENT) {
                            this.player.swing(interactionhand);
                        }

                        this.gameRenderer.itemInHandRenderer.itemUsed(interactionhand);
                        return;
                    }
                }
            }
        }
    }

    public MusicManager getMusicManager() {
        return this.musicManager;
    }

    public void tick() {
        this.clientTickCount++;
        if (this.level != null && !this.pause) {
            this.level.tickRateManager().tick();
        }

        if (this.rightClickDelay > 0) {
            this.rightClickDelay--;
        }

        ProfilerFiller profilerfiller = Profiler.get();
        net.minecraftforge.event.ForgeEventFactory.onPreClientTick();

        profilerfiller.push("gui");
        this.chatListener.tick();
        this.gui.tick(this.pause);
        profilerfiller.pop();
        this.gameRenderer.pick(1.0F);
        this.tutorial.onLookAt(this.level, this.hitResult);
        profilerfiller.push("gameMode");
        if (!this.pause && this.level != null) {
            this.gameMode.tick();
        }

        profilerfiller.popPush("textures");
        if (this.isLevelRunningNormally()) {
            this.textureManager.tick();
        }

        if (this.screen != null || this.player == null) {
            if (this.screen instanceof InBedChatScreen inbedchatscreen && !this.player.isSleeping()) {
                inbedchatscreen.onPlayerWokeUp();
            }
        } else if (this.player.isDeadOrDying() && !(this.screen instanceof DeathScreen)) {
            this.setScreen(null);
        } else if (this.player.isSleeping() && this.level != null) {
            this.setScreen(new InBedChatScreen());
        }

        if (this.screen != null) {
            this.missTime = 10000;
        }

        if (this.screen != null) {
            try {
                this.screen.tick();
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Ticking screen");
                this.screen.fillCrashDetails(crashreport);
                throw new ReportedException(crashreport);
            }
        }

        if (!this.getDebugOverlay().showDebugScreen()) {
            this.gui.clearCache();
        }

        if (this.overlay == null && this.screen == null) {
            profilerfiller.popPush("Keybindings");
            this.handleKeybinds();
            if (this.missTime > 0) {
                this.missTime--;
            }
        }

        if (this.level != null) {
            profilerfiller.popPush("gameRenderer");
            if (!this.pause) {
                this.gameRenderer.tick();
            }

            profilerfiller.popPush("levelRenderer");
            if (!this.pause) {
                this.levelRenderer.tick();
            }

            profilerfiller.popPush("level");
            if (!this.pause) {
                this.level.tickEntities();
            }
        } else if (this.gameRenderer.currentPostEffect() != null) {
            this.gameRenderer.clearPostEffect();
        }

        if (!this.pause) {
            this.musicManager.tick();
        }

        this.soundManager.tick(this.pause);
        if (this.level != null) {
            if (!this.pause) {
                if (!this.options.joinedFirstServer && this.isMultiplayerServer()) {
                    Component component = Component.translatable("tutorial.socialInteractions.title");
                    Component component1 = Component.translatable("tutorial.socialInteractions.description", Tutorial.key("socialInteractions"));
                    this.socialInteractionsToast = new TutorialToast(this.font, TutorialToast.Icons.SOCIAL_INTERACTIONS, component, component1, true, 8000);
                    this.toastManager.addToast(this.socialInteractionsToast);
                    this.options.joinedFirstServer = true;
                    this.options.save();
                }

                this.tutorial.tick();

                net.minecraftforge.event.ForgeEventFactory.onPreLevelTick(this.level, () -> true);
                try {
                    this.level.tick(() -> true);
                } catch (Throwable throwable1) {
                    CrashReport crashreport1 = CrashReport.forThrowable(throwable1, "Exception in world tick");
                    if (this.level == null) {
                        CrashReportCategory crashreportcategory = crashreport1.addCategory("Affected level");
                        crashreportcategory.setDetail("Problem", "Level is null!");
                    } else {
                        this.level.fillReportDetails(crashreport1);
                    }

                    throw new ReportedException(crashreport1);
                }
                net.minecraftforge.event.ForgeEventFactory.onPostLevelTick(this.level, () -> true);
            }

            profilerfiller.popPush("animateTick");
            if (!this.pause && this.isLevelRunningNormally()) {
                this.level.animateTick(this.player.getBlockX(), this.player.getBlockY(), this.player.getBlockZ());
            }

            profilerfiller.popPush("particles");
            if (!this.pause && this.isLevelRunningNormally()) {
                this.particleEngine.tick();
            }

            ClientPacketListener clientpacketlistener = this.getConnection();
            if (clientpacketlistener != null && !this.pause) {
                clientpacketlistener.send(ServerboundClientTickEndPacket.INSTANCE);
            }
        } else if (this.pendingConnection != null) {
            profilerfiller.popPush("pendingConnection");
            this.pendingConnection.tick();
        }

        profilerfiller.popPush("keyboard");
        this.keyboardHandler.tick();
        profilerfiller.pop();
        net.minecraftforge.event.ForgeEventFactory.onPostClientTick();
    }

    private boolean isLevelRunningNormally() {
        return this.level == null || this.level.tickRateManager().runsNormally();
    }

    private boolean isMultiplayerServer() {
        return !this.isLocalServer || this.singleplayerServer != null && this.singleplayerServer.isPublished();
    }

    private void handleKeybinds() {
        while (this.options.keyTogglePerspective.consumeClick()) {
            CameraType cameratype = this.options.getCameraType();
            this.options.setCameraType(this.options.getCameraType().cycle());
            if (cameratype.isFirstPerson() != this.options.getCameraType().isFirstPerson()) {
                this.gameRenderer.checkEntityPostEffect(this.options.getCameraType().isFirstPerson() ? this.getCameraEntity() : null);
            }

            this.levelRenderer.needsUpdate();
        }

        while (this.options.keySmoothCamera.consumeClick()) {
            this.options.smoothCamera = !this.options.smoothCamera;
        }

        for (int i = 0; i < 9; i++) {
            boolean flag = this.options.keySaveHotbarActivator.isDown();
            boolean flag1 = this.options.keyLoadHotbarActivator.isDown();
            if (this.options.keyHotbarSlots[i].consumeClick()) {
                if (this.player.isSpectator()) {
                    this.gui.getSpectatorGui().onHotbarSelected(i);
                } else if (!this.player.hasInfiniteMaterials() || this.screen != null || !flag1 && !flag) {
                    this.player.getInventory().setSelectedSlot(i);
                } else {
                    CreativeModeInventoryScreen.handleHotbarLoadOrSave(this, i, flag1, flag);
                }
            }
        }

        while (this.options.keySocialInteractions.consumeClick()) {
            if (!this.isMultiplayerServer()) {
                this.player.displayClientMessage(SOCIAL_INTERACTIONS_NOT_AVAILABLE, true);
                this.narrator.sayNow(SOCIAL_INTERACTIONS_NOT_AVAILABLE);
            } else {
                if (this.socialInteractionsToast != null) {
                    this.socialInteractionsToast.hide();
                    this.socialInteractionsToast = null;
                }

                this.setScreen(new SocialInteractionsScreen());
            }
        }

        while (this.options.keyInventory.consumeClick()) {
            if (this.gameMode.isServerControlledInventory()) {
                this.player.sendOpenInventory();
            } else {
                this.tutorial.onOpenInventory();
                this.setScreen(new InventoryScreen(this.player));
            }
        }

        while (this.options.keyAdvancements.consumeClick()) {
            this.setScreen(new AdvancementsScreen(this.player.connection.getAdvancements()));
        }

        while (this.options.keySwapOffhand.consumeClick()) {
            if (!this.player.isSpectator()) {
                this.getConnection()
                    .send(
                        new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN)
                    );
            }
        }

        while (this.options.keyDrop.consumeClick()) {
            if (!this.player.isSpectator() && this.player.drop(Screen.hasControlDown())) {
                this.player.swing(InteractionHand.MAIN_HAND);
            }
        }

        while (this.options.keyChat.consumeClick()) {
            this.openChatScreen("");
        }

        if (this.screen == null && this.overlay == null && this.options.keyCommand.consumeClick()) {
            this.openChatScreen("/");
        }

        boolean flag2 = false;
        if (this.player.isUsingItem()) {
            if (!this.options.keyUse.isDown()) {
                this.gameMode.releaseUsingItem(this.player);
            }

            while (this.options.keyAttack.consumeClick()) {
            }

            while (this.options.keyUse.consumeClick()) {
            }

            while (this.options.keyPickItem.consumeClick()) {
            }
        } else {
            while (this.options.keyAttack.consumeClick()) {
                flag2 |= this.startAttack();
            }

            while (this.options.keyUse.consumeClick()) {
                this.startUseItem();
            }

            while (this.options.keyPickItem.consumeClick()) {
                this.pickBlock();
            }
        }

        if (this.options.keyUse.isDown() && this.rightClickDelay == 0 && !this.player.isUsingItem()) {
            this.startUseItem();
        }

        this.continueAttack(this.screen == null && !flag2 && this.options.keyAttack.isDown() && this.mouseHandler.isMouseGrabbed());
    }

    public ClientTelemetryManager getTelemetryManager() {
        return this.telemetryManager;
    }

    public double getGpuUtilization() {
        return this.gpuUtilization;
    }

    public ProfileKeyPairManager getProfileKeyPairManager() {
        return this.profileKeyPairManager;
    }

    public WorldOpenFlows createWorldOpenFlows() {
        return new WorldOpenFlows(this, this.levelSource);
    }

    public void doWorldLoad(LevelStorageSource.LevelStorageAccess pLevelStorage, PackRepository pPackRepository, WorldStem pWorldStem, boolean pNewWorld) {
        this.disconnect();
        this.progressListener.set(null);
        Instant instant = Instant.now();

        try {
            pLevelStorage.saveDataTag(pWorldStem.registries().compositeAccess(), pWorldStem.worldData());
            Services services = Services.create(this.authenticationService, this.gameDirectory);
            services.profileCache().setExecutor(this);
            SkullBlockEntity.setup(services, this);
            GameProfileCache.setUsesAuthentication(false);
            this.singleplayerServer = MinecraftServer.spin(
                p_231361_ -> new IntegratedServer(p_231361_, this, pLevelStorage, pPackRepository, pWorldStem, services, p_325274_ -> {
                    StoringChunkProgressListener storingchunkprogresslistener = StoringChunkProgressListener.createFromGameruleRadius(p_325274_ + 0);
                    this.progressListener.set(storingchunkprogresslistener);
                    return ProcessorChunkProgressListener.createStarted(storingchunkprogresslistener, this.progressTasks::add);
                })
            );
            this.isLocalServer = true;
            this.updateReportEnvironment(ReportEnvironment.local());
            this.quickPlayLog.setWorldData(QuickPlayLog.Type.SINGLEPLAYER, pLevelStorage.getLevelId(), pWorldStem.worldData().getLevelName());
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Starting integrated server");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Starting integrated server");
            crashreportcategory.setDetail("Level ID", pLevelStorage.getLevelId());
            crashreportcategory.setDetail("Level Name", () -> pWorldStem.worldData().getLevelName());
            throw new ReportedException(crashreport);
        }

        while (this.progressListener.get() == null) {
            Thread.yield();
        }

        LevelLoadingScreen levelloadingscreen = new LevelLoadingScreen(this.progressListener.get());
        ProfilerFiller profilerfiller = Profiler.get();
        this.setScreen(levelloadingscreen);
        profilerfiller.push("waitForServer");

        for (; !this.singleplayerServer.isReady() || this.overlay != null; this.handleDelayedCrash()) {
            levelloadingscreen.tick();
            this.runTick(false);

            try {
                Thread.sleep(16L);
            } catch (InterruptedException interruptedexception) {
            }
        }

        profilerfiller.pop();
        Duration duration = Duration.between(instant, Instant.now());
        SocketAddress socketaddress = this.singleplayerServer.getConnection().startMemoryChannel();
        Connection connection = Connection.connectToLocalServer(socketaddress);
        connection.initiateServerboundPlayConnection(
            socketaddress.toString(), 0, new ClientHandshakePacketListenerImpl(connection, this, null, null, pNewWorld, duration, p_231442_ -> {}, null)
        );
        connection.send(new ServerboundHelloPacket(this.getUser().getName(), this.getUser().getProfileId()));
        this.pendingConnection = connection;
    }

    public void setLevel(ClientLevel pLevel, ReceivingLevelScreen.Reason pReason) {
        if (this.level != null) net.minecraftforge.event.ForgeEventFactory.onLevelUnload(this.level);
        this.updateScreenAndTick(new ReceivingLevelScreen(() -> false, pReason));
        this.level = pLevel;
        this.updateLevelInEngines(pLevel);
        if (!this.isLocalServer) {
            Services services = Services.create(this.authenticationService, this.gameDirectory);
            services.profileCache().setExecutor(this);
            SkullBlockEntity.setup(services, this);
            GameProfileCache.setUsesAuthentication(false);
        }
    }

    public void disconnect() {
        this.disconnect(new ProgressScreen(true), false);
    }

    public void disconnect(Screen pNextScreen) {
        this.disconnect(pNextScreen, false);
    }

    public void disconnect(Screen pNextScreen, boolean pKeepResourcePacks) {
        ClientPacketListener clientpacketlistener = this.getConnection();
        if (clientpacketlistener != null) {
            this.dropAllTasks();
            clientpacketlistener.close();
            if (!pKeepResourcePacks) {
                this.clearDownloadedResourcePacks();
            }
        }

        this.playerSocialManager.stopOnlineMode();
        if (this.metricsRecorder.isRecording()) {
            this.debugClientMetricsCancel();
        }

        IntegratedServer integratedserver = this.singleplayerServer;
        this.singleplayerServer = null;
        this.gameRenderer.resetData();
        net.minecraftforge.client.event.ForgeEventFactoryClient.firePlayerLogout(this.gameMode, this.player);
        this.gameMode = null;
        this.narrator.clear();
        this.clientLevelTeardownInProgress = true;

        try {
            this.updateScreenAndTick(pNextScreen);
            if (this.level != null) {
                net.minecraftforge.event.ForgeEventFactory.onLevelUnload(this.level);
                if (integratedserver != null) {
                    ProfilerFiller profilerfiller = Profiler.get();
                    profilerfiller.push("waitForServer");

                    while (!integratedserver.isShutdown()) {
                        this.runTick(false);
                    }

                    profilerfiller.pop();
                }

                this.gui.onDisconnected();
                this.isLocalServer = false;
                net.minecraftforge.client.ForgeHooksClient.handleClientLevelClosing(this.level);
            }

            this.level = null;
            this.updateLevelInEngines(null);
            this.player = null;
        } finally {
            this.clientLevelTeardownInProgress = false;
        }

        SkullBlockEntity.clear();
    }

    public void clearDownloadedResourcePacks() {
        this.downloadedPackSource.cleanupAfterDisconnect();
        this.runAllTasks();
    }

    public void clearClientLevel(Screen pNextScreen) {
        ClientPacketListener clientpacketlistener = this.getConnection();
        if (clientpacketlistener != null) {
            clientpacketlistener.clearLevel();
        }

        if (this.metricsRecorder.isRecording()) {
            this.debugClientMetricsCancel();
        }

        this.gameRenderer.resetData();
        this.gameMode = null;
        this.narrator.clear();
        this.clientLevelTeardownInProgress = true;

        try {
            this.updateScreenAndTick(pNextScreen);
            this.gui.onDisconnected();
            this.level = null;
            this.updateLevelInEngines(null);
            this.player = null;
        } finally {
            this.clientLevelTeardownInProgress = false;
        }

        SkullBlockEntity.clear();
    }

    private void updateScreenAndTick(Screen pScreen) {
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("forcedTick");
        this.soundManager.stop();
        this.cameraEntity = null;
        this.pendingConnection = null;
        this.setScreen(pScreen);
        this.runTick(false);
        profilerfiller.pop();
    }

    public void forceSetScreen(Screen pScreen) {
        try (Zone zone = Profiler.get().zone("forcedTick")) {
            this.setScreen(pScreen);
            this.runTick(false);
        }
    }

    private void updateLevelInEngines(@Nullable ClientLevel pLevel) {
        this.levelRenderer.setLevel(pLevel);
        this.particleEngine.setLevel(pLevel);
        this.blockEntityRenderDispatcher.setLevel(pLevel);
        this.updateTitle();
    }

    private UserProperties userProperties() {
        return this.userPropertiesFuture.join();
    }

    public boolean telemetryOptInExtra() {
        return this.extraTelemetryAvailable() && this.options.telemetryOptInExtra().get();
    }

    public boolean extraTelemetryAvailable() {
        return this.allowsTelemetry() && this.userProperties().flag(UserFlag.OPTIONAL_TELEMETRY_AVAILABLE);
    }

    public boolean allowsTelemetry() {
        return SharedConstants.IS_RUNNING_IN_IDE ? false : this.userProperties().flag(UserFlag.TELEMETRY_ENABLED);
    }

    public boolean allowsMultiplayer() {
        return this.allowsMultiplayer && this.userProperties().flag(UserFlag.SERVERS_ALLOWED) && this.multiplayerBan() == null && !this.isNameBanned();
    }

    public boolean allowsRealms() {
        return this.userProperties().flag(UserFlag.REALMS_ALLOWED) && this.multiplayerBan() == null;
    }

    @Nullable
    public BanDetails multiplayerBan() {
        return this.userProperties().bannedScopes().get("MULTIPLAYER");
    }

    public boolean isNameBanned() {
        ProfileResult profileresult = this.profileFuture.getNow(null);
        return profileresult != null && profileresult.actions().contains(ProfileActionType.FORCED_NAME_CHANGE);
    }

    public boolean isBlocked(UUID pPlayerUUID) {
        return this.getChatStatus().isChatAllowed(false)
            ? this.playerSocialManager.shouldHideMessageFrom(pPlayerUUID)
            : (this.player == null || !pPlayerUUID.equals(this.player.getUUID())) && !pPlayerUUID.equals(Util.NIL_UUID);
    }

    public Minecraft.ChatStatus getChatStatus() {
        if (this.options.chatVisibility().get() == ChatVisiblity.HIDDEN) {
            return Minecraft.ChatStatus.DISABLED_BY_OPTIONS;
        } else if (!this.allowsChat) {
            return Minecraft.ChatStatus.DISABLED_BY_LAUNCHER;
        } else {
            return !this.userProperties().flag(UserFlag.CHAT_ALLOWED) ? Minecraft.ChatStatus.DISABLED_BY_PROFILE : Minecraft.ChatStatus.ENABLED;
        }
    }

    public final boolean isDemo() {
        return this.demo;
    }

    @Nullable
    public ClientPacketListener getConnection() {
        return this.player == null ? null : this.player.connection;
    }

    public static boolean renderNames() {
        return !instance.options.hideGui;
    }

    public static boolean useFancyGraphics() {
        return instance.options.graphicsMode().get().getId() >= GraphicsStatus.FANCY.getId();
    }

    public static boolean useShaderTransparency() {
        return !instance.gameRenderer.isPanoramicMode() && instance.options.graphicsMode().get().getId() >= GraphicsStatus.FABULOUS.getId();
    }

    public static boolean useAmbientOcclusion() {
        return instance.options.ambientOcclusion().get();
    }

    private void pickBlock() {
        if (this.hitResult != null && this.hitResult.getType() != HitResult.Type.MISS) {
            if (net.minecraftforge.client.event.ForgeEventFactoryClient.onClickInput(2, this.options.keyPickItem, InteractionHand.MAIN_HAND).isCanceled()) return;
            boolean flag = Screen.hasControlDown();
            switch (this.hitResult) {
                case BlockHitResult blockhitresult:
                    this.gameMode.handlePickItemFromBlock(blockhitresult.getBlockPos(), flag);
                    break;
                case EntityHitResult entityhitresult:
                    this.gameMode.handlePickItemFromEntity(entityhitresult.getEntity(), flag);
                    break;
                default:
            }
        }
    }

    public CrashReport fillReport(CrashReport pTheCrash) {
        SystemReport systemreport = pTheCrash.getSystemReport();

        try {
            fillSystemReport(systemreport, this, this.languageManager, this.launchedVersion, this.options);
            this.fillUptime(pTheCrash.addCategory("Uptime"));
            if (this.level != null) {
                this.level.fillReportDetails(pTheCrash);
            }

            if (this.singleplayerServer != null) {
                this.singleplayerServer.fillSystemReport(systemreport);
            }

            this.reloadStateTracker.fillCrashReport(pTheCrash);
        } catch (Throwable throwable) {
            LOGGER.error("Failed to collect details", throwable);
        }

        return pTheCrash;
    }

    public static void fillReport(
        @Nullable Minecraft pMinecraft, @Nullable LanguageManager pLanguageManager, String pLaunchVersion, @Nullable Options pOptions, CrashReport pReport
    ) {
        SystemReport systemreport = pReport.getSystemReport();
        fillSystemReport(systemreport, pMinecraft, pLanguageManager, pLaunchVersion, pOptions);
    }

    private static String formatSeconds(double pSeconds) {
        return String.format(Locale.ROOT, "%.3fs", pSeconds);
    }

    private void fillUptime(CrashReportCategory pCategory) {
        pCategory.setDetail("JVM uptime", () -> formatSeconds(ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0));
        pCategory.setDetail("Wall uptime", () -> formatSeconds((System.currentTimeMillis() - this.clientStartTimeMs) / 1000.0));
        pCategory.setDetail("High-res time", () -> formatSeconds(Util.getMillis() / 1000.0));
        pCategory.setDetail("Client ticks", () -> String.format(Locale.ROOT, "%d ticks / %.3fs", this.clientTickCount, this.clientTickCount / 20.0));
    }

    private static SystemReport fillSystemReport(
        SystemReport pReport, @Nullable Minecraft pMinecraft, @Nullable LanguageManager pLanguageManager, String pLaunchVersion, @Nullable Options pOptions
    ) {
        pReport.setDetail("Launched Version", () -> pLaunchVersion);
        String s = getLauncherBrand();
        if (s != null) {
            pReport.setDetail("Launcher name", s);
        }

        pReport.setDetail("Backend library", RenderSystem::getBackendDescription);
        pReport.setDetail("Backend API", RenderSystem::getApiDescription);
        pReport.setDetail("Window size", () -> pMinecraft != null ? pMinecraft.window.getWidth() + "x" + pMinecraft.window.getHeight() : "<not initialized>");
        pReport.setDetail("GFLW Platform", Window::getPlatform);
        pReport.setDetail("Render Extensions", () -> String.join(", ", RenderSystem.getDevice().getEnabledExtensions()));
        pReport.setDetail("GL debug messages", () -> {
            GpuDevice gpudevice = RenderSystem.tryGetDevice();
            if (gpudevice == null) {
                return "<no renderer available>";
            } else {
                return gpudevice.isDebuggingEnabled() ? String.join("\n", gpudevice.getLastDebugMessages()) : "<debugging unavailable>";
            }
        });
        pReport.setDetail("Is Modded", () -> checkModStatus().fullDescription());
        pReport.setDetail("Universe", () -> pMinecraft != null ? Long.toHexString(pMinecraft.canary) : "404");
        pReport.setDetail("Type", "Client (map_client.txt)");
        if (pOptions != null) {
            if (pMinecraft != null) {
                String s1 = pMinecraft.getGpuWarnlistManager().getAllWarnings();
                if (s1 != null) {
                    pReport.setDetail("GPU Warnings", s1);
                }
            }

            pReport.setDetail("Graphics mode", pOptions.graphicsMode().get().toString());
            pReport.setDetail("Render Distance", pOptions.getEffectiveRenderDistance() + "/" + pOptions.renderDistance().get() + " chunks");
        }

        if (pMinecraft != null) {
            pReport.setDetail("Resource Packs", () -> PackRepository.displayPackList(pMinecraft.getResourcePackRepository().getSelectedPacks()));
        }

        if (pLanguageManager != null) {
            pReport.setDetail("Current Language", () -> pLanguageManager.getSelected());
        }

        pReport.setDetail("Locale", String.valueOf(Locale.getDefault()));
        pReport.setDetail("System encoding", () -> System.getProperty("sun.jnu.encoding", "<not set>"));
        pReport.setDetail("File encoding", () -> System.getProperty("file.encoding", "<not set>"));
        pReport.setDetail("CPU", GLX::_getCpuInfo);
        return pReport;
    }

    public static Minecraft getInstance() {
        return instance;
    }

    public CompletableFuture<Void> delayTextureReload() {
        return this.submit((Supplier<CompletableFuture<Void>>)this::reloadResourcePacks).thenCompose(p_231391_ -> (CompletionStage<Void>)p_231391_);
    }

    public void updateReportEnvironment(ReportEnvironment pReportEnvironment) {
        if (!this.reportingContext.matches(pReportEnvironment)) {
            this.reportingContext = ReportingContext.create(pReportEnvironment, this.userApiService);
        }
    }

    @Nullable
    public ServerData getCurrentServer() {
        return Optionull.map(this.getConnection(), ClientPacketListener::getServerData);
    }

    public boolean isLocalServer() {
        return this.isLocalServer;
    }

    public boolean hasSingleplayerServer() {
        return this.isLocalServer && this.singleplayerServer != null;
    }

    @Nullable
    public IntegratedServer getSingleplayerServer() {
        return this.singleplayerServer;
    }

    public boolean isSingleplayer() {
        IntegratedServer integratedserver = this.getSingleplayerServer();
        return integratedserver != null && !integratedserver.isPublished();
    }

    public boolean isLocalPlayer(UUID pUuid) {
        return pUuid.equals(this.getUser().getProfileId());
    }

    public User getUser() {
        return this.user;
    }

    public GameProfile getGameProfile() {
        ProfileResult profileresult = this.profileFuture.join();
        return profileresult != null ? profileresult.profile() : new GameProfile(this.user.getProfileId(), this.user.getName());
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public TextureManager getTextureManager() {
        return this.textureManager;
    }

    public ShaderManager getShaderManager() {
        return this.shaderManager;
    }

    public ResourceManager getResourceManager() {
        return this.resourceManager;
    }

    public PackRepository getResourcePackRepository() {
        return this.resourcePackRepository;
    }

    public VanillaPackResources getVanillaPackResources() {
        return this.vanillaPackResources;
    }

    public DownloadedPackSource getDownloadedPackSource() {
        return this.downloadedPackSource;
    }

    public Path getResourcePackDirectory() {
        return this.resourcePackDirectory;
    }

    public LanguageManager getLanguageManager() {
        return this.languageManager;
    }

    public Function<ResourceLocation, TextureAtlasSprite> getTextureAtlas(ResourceLocation pLocation) {
        return this.modelManager.getAtlas(pLocation)::getSprite;
    }

    public boolean isPaused() {
        return this.pause;
    }

    public GpuWarnlistManager getGpuWarnlistManager() {
        return this.gpuWarnlistManager;
    }

    public SoundManager getSoundManager() {
        return this.soundManager;
    }

    public MusicInfo getSituationalMusic() {
        Music music = Optionull.map(this.screen, Screen::getBackgroundMusic);
        if (music != null) {
            return new MusicInfo(music);
        } else if (this.player != null) {
            Level level = this.player.level();
            if (level.dimension() == Level.END) {
                return this.gui.getBossOverlay().shouldPlayMusic() ? new MusicInfo(Musics.END_BOSS) : new MusicInfo(Musics.END);
            } else {
                Holder<Biome> holder = level.getBiome(this.player.blockPosition());
                float f = holder.value().getBackgroundMusicVolume();
                if (!this.musicManager.isPlayingMusic(Musics.UNDER_WATER) && (!this.player.isUnderWater() || !holder.is(BiomeTags.PLAYS_UNDERWATER_MUSIC))) {
                    if (level.dimension() != Level.NETHER && this.player.getAbilities().instabuild && this.player.getAbilities().mayfly) {
                        return new MusicInfo(Musics.CREATIVE, f);
                    } else {
                        Optional<WeightedList<Music>> optional = holder.value().getBackgroundMusic();
                        if (optional.isPresent()) {
                            Optional<Music> optional1 = optional.get().getRandom(level.random);
                            return new MusicInfo(optional1.orElse(null), f);
                        } else {
                            return new MusicInfo(Musics.GAME, f);
                        }
                    }
                } else {
                    return new MusicInfo(Musics.UNDER_WATER, f);
                }
            }
        } else {
            return new MusicInfo(Musics.MENU);
        }
    }

    public MinecraftSessionService getMinecraftSessionService() {
        return this.minecraftSessionService;
    }

    public SkinManager getSkinManager() {
        return this.skinManager;
    }

    @Nullable
    public Entity getCameraEntity() {
        return this.cameraEntity;
    }

    public void setCameraEntity(Entity pViewingEntity) {
        this.cameraEntity = pViewingEntity;
        this.gameRenderer.checkEntityPostEffect(pViewingEntity);
    }

    public boolean shouldEntityAppearGlowing(Entity pEntity) {
        return pEntity.isCurrentlyGlowing()
            || this.player != null && this.player.isSpectator() && this.options.keySpectatorOutlines.isDown() && pEntity.getType() == EntityType.PLAYER;
    }

    @Override
    protected Thread getRunningThread() {
        return this.gameThread;
    }

    @Override
    public Runnable wrapRunnable(Runnable pRunnable) {
        return pRunnable;
    }

    @Override
    protected boolean shouldRun(Runnable pRunnable) {
        return true;
    }

    public BlockRenderDispatcher getBlockRenderer() {
        return this.blockRenderer;
    }

    public EntityRenderDispatcher getEntityRenderDispatcher() {
        return this.entityRenderDispatcher;
    }

    public BlockEntityRenderDispatcher getBlockEntityRenderDispatcher() {
        return this.blockEntityRenderDispatcher;
    }

    public ItemRenderer getItemRenderer() {
        return this.itemRenderer;
    }

    public MapRenderer getMapRenderer() {
        return this.mapRenderer;
    }

    public DataFixer getFixerUpper() {
        return this.fixerUpper;
    }

    public DeltaTracker getDeltaTracker() {
        return this.deltaTracker;
    }

    public BlockColors getBlockColors() {
        return this.blockColors;
    }

    public boolean showOnlyReducedInfo() {
        return this.player != null && this.player.isReducedDebugInfo() || this.options.reducedDebugInfo().get();
    }

    public ToastManager getToastManager() {
        return this.toastManager;
    }

    public Tutorial getTutorial() {
        return this.tutorial;
    }

    public boolean isWindowActive() {
        return this.windowActive;
    }

    public HotbarManager getHotbarManager() {
        return this.hotbarManager;
    }

    public ModelManager getModelManager() {
        return this.modelManager;
    }

    public PaintingTextureManager getPaintingTextures() {
        return this.paintingTextures;
    }

    public MobEffectTextureManager getMobEffectTextures() {
        return this.mobEffectTextures;
    }

    public MapTextureManager getMapTextureManager() {
        return this.mapTextureManager;
    }

    public MapDecorationTextureManager getMapDecorationTextures() {
        return this.mapDecorationTextures;
    }

    public GuiSpriteManager getGuiSprites() {
        return this.guiSprites;
    }

    @Override
    public void setWindowActive(boolean pFocused) {
        this.windowActive = pFocused;
    }

    public Component grabPanoramixScreenshot(File pGameDirectory, int pWidth, int pHeight) {
        int i = this.window.getWidth();
        int j = this.window.getHeight();
        RenderTarget rendertarget = this.getMainRenderTarget();
        float f = this.player.getXRot();
        float f1 = this.player.getYRot();
        float f2 = this.player.xRotO;
        float f3 = this.player.yRotO;
        this.gameRenderer.setRenderBlockOutline(false);

        MutableComponent mutablecomponent;
        try {
            this.gameRenderer.setPanoramicMode(true);
            this.window.setWidth(pWidth);
            this.window.setHeight(pHeight);
            rendertarget.resize(pWidth, pHeight);

            for (int k = 0; k < 6; k++) {
                switch (k) {
                    case 0:
                        this.player.setYRot(f1);
                        this.player.setXRot(0.0F);
                        break;
                    case 1:
                        this.player.setYRot((f1 + 90.0F) % 360.0F);
                        this.player.setXRot(0.0F);
                        break;
                    case 2:
                        this.player.setYRot((f1 + 180.0F) % 360.0F);
                        this.player.setXRot(0.0F);
                        break;
                    case 3:
                        this.player.setYRot((f1 - 90.0F) % 360.0F);
                        this.player.setXRot(0.0F);
                        break;
                    case 4:
                        this.player.setYRot(f1);
                        this.player.setXRot(-90.0F);
                        break;
                    case 5:
                    default:
                        this.player.setYRot(f1);
                        this.player.setXRot(90.0F);
                }

                this.player.yRotO = this.player.getYRot();
                this.player.xRotO = this.player.getXRot();
                this.gameRenderer.renderLevel(DeltaTracker.ONE);

                try {
                    Thread.sleep(10L);
                } catch (InterruptedException interruptedexception) {
                }

                Screenshot.grab(pGameDirectory, "panorama_" + k + ".png", rendertarget, p_231415_ -> {});
            }

            Component component = Component.literal(pGameDirectory.getName())
                .withStyle(ChatFormatting.UNDERLINE)
                .withStyle(p_389130_ -> p_389130_.withClickEvent(new ClickEvent.OpenFile(pGameDirectory.getAbsoluteFile())));
            return Component.translatable("screenshot.success", component);
        } catch (Exception exception) {
            LOGGER.error("Couldn't save image", (Throwable)exception);
            mutablecomponent = Component.translatable("screenshot.failure", exception.getMessage());
        } finally {
            this.player.setXRot(f);
            this.player.setYRot(f1);
            this.player.xRotO = f2;
            this.player.yRotO = f3;
            this.gameRenderer.setRenderBlockOutline(true);
            this.window.setWidth(i);
            this.window.setHeight(j);
            rendertarget.resize(i, j);
            this.gameRenderer.setPanoramicMode(false);
        }

        return mutablecomponent;
    }

    @Nullable
    public StoringChunkProgressListener getProgressListener() {
        return this.progressListener.get();
    }

    public SplashManager getSplashManager() {
        return this.splashManager;
    }

    @Nullable
    public Overlay getOverlay() {
        return this.overlay;
    }

    public PlayerSocialManager getPlayerSocialManager() {
        return this.playerSocialManager;
    }

    public Window getWindow() {
        return this.window;
    }

    public FramerateLimitTracker getFramerateLimitTracker() {
        return this.framerateLimitTracker;
    }

    public DebugScreenOverlay getDebugOverlay() {
        return this.gui.getDebugOverlay();
    }

    public RenderBuffers renderBuffers() {
        return this.renderBuffers;
    }

    public void updateMaxMipLevel(int pMipMapLevel) {
        this.modelManager.updateMaxMipLevel(pMipMapLevel);
    }

    public EntityModelSet getEntityModels() {
        return this.modelManager.entityModels().get();
    }

    public boolean isTextFilteringEnabled() {
        return this.userProperties().flag(UserFlag.PROFANITY_FILTER_ENABLED);
    }

    public void prepareForMultiplayer() {
        this.playerSocialManager.startOnlineMode();
        this.getProfileKeyPairManager().prepareKeyPair();
    }

    @Nullable
    public SignatureValidator getProfileKeySignatureValidator() {
        return SignatureValidator.from(this.authenticationService.getServicesKeySet(), ServicesKeyType.PROFILE_KEY);
    }

    public boolean canValidateProfileKeys() {
        return !this.authenticationService.getServicesKeySet().keys(ServicesKeyType.PROFILE_KEY).isEmpty();
    }

    public InputType getLastInputType() {
        return this.lastInputType;
    }

    public void setLastInputType(InputType pLastInputType) {
        this.lastInputType = pLastInputType;
    }

    public GameNarrator getNarrator() {
        return this.narrator;
    }

    public ChatListener getChatListener() {
        return this.chatListener;
    }

    public ReportingContext getReportingContext() {
        return this.reportingContext;
    }

    public RealmsDataFetcher realmsDataFetcher() {
        return this.realmsDataFetcher;
    }

    public QuickPlayLog quickPlayLog() {
        return this.quickPlayLog;
    }

    public CommandHistory commandHistory() {
        return this.commandHistory;
    }

    public DirectoryValidator directoryValidator() {
        return this.directoryValidator;
    }

    private float getTickTargetMillis(float pDefaultValue) {
        if (this.level != null) {
            TickRateManager tickratemanager = this.level.tickRateManager();
            if (tickratemanager.runsNormally()) {
                return Math.max(pDefaultValue, tickratemanager.millisecondsPerTick());
            }
        }

        return pDefaultValue;
    }

    public ItemModelResolver getItemModelResolver() {
        return this.itemModelResolver;
    }

    @Nullable
    public static String getLauncherBrand() {
        return System.getProperty("minecraft.launcher.brand");
    }

    @OnlyIn(Dist.CLIENT)
    public static enum ChatStatus {
        ENABLED(CommonComponents.EMPTY) {
            @Override
            public boolean isChatAllowed(boolean p_168045_) {
                return true;
            }
        },
        DISABLED_BY_OPTIONS(Component.translatable("chat.disabled.options").withStyle(ChatFormatting.RED)) {
            @Override
            public boolean isChatAllowed(boolean p_168051_) {
                return false;
            }
        },
        DISABLED_BY_LAUNCHER(Component.translatable("chat.disabled.launcher").withStyle(ChatFormatting.RED)) {
            @Override
            public boolean isChatAllowed(boolean p_168057_) {
                return p_168057_;
            }
        },
        DISABLED_BY_PROFILE(
            Component.translatable("chat.disabled.profile", Component.keybind(Minecraft.instance.options.keyChat.getName())).withStyle(ChatFormatting.RED)
        ) {
            @Override
            public boolean isChatAllowed(boolean p_168063_) {
                return p_168063_;
            }
        };

        static final Component INFO_DISABLED_BY_PROFILE = Component.translatable("chat.disabled.profile.moreInfo");
        private final Component message;

        ChatStatus(final Component pMessage) {
            this.message = pMessage;
        }

        public Component getMessage() {
            return this.message;
        }

        public abstract boolean isChatAllowed(boolean pIsLocalServer);
    }

    @OnlyIn(Dist.CLIENT)
    record GameLoadCookie(RealmsClient realmsClient, GameConfig.QuickPlayData quickPlayData) {
    }
}
