package net.minecraft.client.resources.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SpecialBlockModelRenderer;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ModelManager implements PreparableReloadListener, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter MODEL_LISTER = FileToIdConverter.json("models");
    private static final Map<ResourceLocation, ResourceLocation> VANILLA_ATLASES = Map.of(
        Sheets.BANNER_SHEET,
        AtlasIds.BANNER_PATTERNS,
        Sheets.BED_SHEET,
        AtlasIds.BEDS,
        Sheets.CHEST_SHEET,
        AtlasIds.CHESTS,
        Sheets.SHIELD_SHEET,
        AtlasIds.SHIELD_PATTERNS,
        Sheets.SIGN_SHEET,
        AtlasIds.SIGNS,
        Sheets.SHULKER_SHEET,
        AtlasIds.SHULKER_BOXES,
        Sheets.ARMOR_TRIMS_SHEET,
        AtlasIds.ARMOR_TRIMS,
        Sheets.DECORATED_POT_SHEET,
        AtlasIds.DECORATED_POT,
        TextureAtlas.LOCATION_BLOCKS,
        AtlasIds.BLOCKS
    );
    private Map<ResourceLocation, ItemModel> bakedItemStackModels = Map.of();
    private Map<ResourceLocation, ItemModel> bakedItemStackModelsView = Map.of();
    private Map<ResourceLocation, ClientItem.Properties> itemProperties = Map.of();
    private final AtlasSet atlases;
    private final BlockModelShaper blockModelShaper;
    private final BlockColors blockColors;
    private EntityModelSet entityModelSet = EntityModelSet.EMPTY;
    private SpecialBlockModelRenderer specialBlockModelRenderer = SpecialBlockModelRenderer.EMPTY;
    private int maxMipmapLevels;
    private ModelBakery.MissingModels missingModels;
    private Object2IntMap<BlockState> modelGroups = Object2IntMaps.emptyMap();
    private ModelBakery modelBakery;

    public ModelManager(TextureManager pTextureManager, BlockColors pBlockColors, int pMaxMipmapLevels) {
        this.blockColors = pBlockColors;
        this.maxMipmapLevels = pMaxMipmapLevels;
        this.blockModelShaper = new BlockModelShaper(this);
        this.atlases = new AtlasSet(VANILLA_ATLASES, pTextureManager);
    }

    public BlockStateModel getMissingBlockStateModel() {
        return this.missingModels.block();
    }

    public ItemModel getItemModel(ResourceLocation pModelLocation) {
        return this.bakedItemStackModels.getOrDefault(pModelLocation, this.missingModels.item());
    }

    public Map<ResourceLocation, ItemModel> getItemModels() {
        return this.bakedItemStackModelsView;
    }

    public ClientItem.Properties getItemProperties(ResourceLocation pItemId) {
        return this.itemProperties.getOrDefault(pItemId, ClientItem.Properties.DEFAULT);
    }

    public BlockModelShaper getBlockModelShaper() {
        return this.blockModelShaper;
    }

    @Override
    public final CompletableFuture<Void> reload(
        PreparableReloadListener.PreparationBarrier p_249079_, ResourceManager p_251134_, Executor p_250550_, Executor p_249221_
    ) {
        net.minecraftforge.client.model.geometry.GeometryLoaderManager.init();
        CompletableFuture<EntityModelSet> completablefuture = CompletableFuture.supplyAsync(EntityModelSet::vanilla, p_250550_);
        CompletableFuture<SpecialBlockModelRenderer> completablefuture1 = completablefuture.thenApplyAsync(SpecialBlockModelRenderer::vanilla, p_250550_);
        CompletableFuture<Map<ResourceLocation, UnbakedModel>> completablefuture2 = loadBlockModels(p_251134_, p_250550_);
        CompletableFuture<BlockStateModelLoader.LoadedModels> completablefuture3 = BlockStateModelLoader.loadBlockStates(p_251134_, p_250550_);
        CompletableFuture<ClientItemInfoLoader.LoadedClientInfos> completablefuture4 = ClientItemInfoLoader.scheduleLoad(p_251134_, p_250550_);
        CompletableFuture<ModelManager.ResolvedModels> completablefuture5 = CompletableFuture.allOf(completablefuture2, completablefuture3, completablefuture4)
            .thenApplyAsync(p_389625_ -> discoverModelDependencies(completablefuture2.join(), completablefuture3.join(), completablefuture4.join()), p_250550_);
        CompletableFuture<Object2IntMap<BlockState>> completablefuture6 = completablefuture3.thenApplyAsync(
            p_358038_ -> buildModelGroups(this.blockColors, p_358038_), p_250550_
        );
        Map<ResourceLocation, CompletableFuture<AtlasSet.StitchResult>> map = this.atlases.scheduleLoad(p_251134_, this.maxMipmapLevels, p_250550_);
        return CompletableFuture.allOf(
                Stream.concat(
                        map.values().stream(),
                        Stream.of(
                            completablefuture5,
                            completablefuture6,
                            completablefuture3,
                            completablefuture4,
                            completablefuture,
                            completablefuture1,
                            completablefuture2
                        )
                    )
                    .toArray(CompletableFuture[]::new)
            )
            .thenComposeAsync(
                p_389621_ -> {
                    Map<ResourceLocation, AtlasSet.StitchResult> map1 = Util.mapValues(map, CompletableFuture::join);
                    ModelManager.ResolvedModels modelmanager$resolvedmodels = completablefuture5.join();
                    Object2IntMap<BlockState> object2intmap = completablefuture6.join();
                    Set<ResourceLocation> set = Sets.difference(completablefuture2.join().keySet(), modelmanager$resolvedmodels.models.keySet());
                    if (!set.isEmpty()) {
                        LOGGER.debug(
                            "Unreferenced models: \n{}", set.stream().sorted().map(p_374723_ -> "\t" + p_374723_ + "\n").collect(Collectors.joining())
                        );
                    }

                    ModelBakery modelbakery = new ModelBakery(
                        completablefuture.join(),
                        completablefuture3.join().models(),
                        completablefuture4.join().contents(),
                        modelmanager$resolvedmodels.models(),
                        modelmanager$resolvedmodels.missing()
                    );
                    return loadModels(map1, modelbakery, object2intmap, completablefuture.join(), completablefuture1.join(), p_250550_);
                },
                p_250550_
            )
            .thenCompose(p_252255_ -> p_252255_.readyForUpload.thenApply(p_251581_ -> (ModelManager.ReloadState)p_252255_))
            .thenCompose(p_249079_::wait)
            .thenAcceptAsync(p_358039_ -> this.apply(p_358039_, Profiler.get()), p_249221_);
    }

    private static CompletableFuture<Map<ResourceLocation, UnbakedModel>> loadBlockModels(ResourceManager pResourceManager, Executor pExecutor) {
        return CompletableFuture.<Map<ResourceLocation, Resource>>supplyAsync(() -> MODEL_LISTER.listMatchingResources(pResourceManager), pExecutor)
            .thenCompose(
                p_250597_ -> {
                    List<CompletableFuture<Pair<ResourceLocation, BlockModel>>> list = new ArrayList<>(p_250597_.size());

                    for (Entry<ResourceLocation, Resource> entry : p_250597_.entrySet()) {
                        list.add(CompletableFuture.supplyAsync(() -> {
                            ResourceLocation resourcelocation = MODEL_LISTER.fileToId(entry.getKey());

                            try {
                                Pair pair;
                                try (Reader reader = entry.getValue().openAsReader()) {
                                    pair = Pair.of(resourcelocation, BlockModel.fromStream(reader));
                                }

                                return pair;
                            } catch (Exception exception) {
                                LOGGER.error("Failed to load model {}", entry.getKey(), exception);
                                return null;
                            }
                        }, pExecutor));
                    }

                    return Util.sequence(list)
                        .thenApply(
                            p_250813_ -> p_250813_.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond))
                        );
                }
            );
    }

    private static ModelManager.ResolvedModels discoverModelDependencies(
        Map<ResourceLocation, UnbakedModel> pInputModels, BlockStateModelLoader.LoadedModels pLoadedModels, ClientItemInfoLoader.LoadedClientInfos pLoadedClientInfos
    ) {
        ModelManager.ResolvedModels modelmanager$resolvedmodels;
        try (Zone zone = Profiler.get().zone("dependencies")) {
            ModelDiscovery modeldiscovery = new ModelDiscovery(pInputModels, MissingBlockModel.missingModel());
            modeldiscovery.addSpecialModel(ItemModelGenerator.GENERATED_ITEM_MODEL_ID, new ItemModelGenerator());
            pLoadedModels.models().values().forEach(modeldiscovery::addRoot);
            pLoadedClientInfos.contents().values().forEach(p_374734_ -> modeldiscovery.addRoot(p_374734_.model()));
            modelmanager$resolvedmodels = new ModelManager.ResolvedModels(modeldiscovery.missingModel(), modeldiscovery.resolve());
        }

        return modelmanager$resolvedmodels;
    }

    private static CompletableFuture<ModelManager.ReloadState> loadModels(
        final Map<ResourceLocation, AtlasSet.StitchResult> pStitchResults,
        ModelBakery pModelBakery,
        Object2IntMap<BlockState> pModelGroups,
        EntityModelSet pEntityModelSet,
        SpecialBlockModelRenderer pSpecialBlockModelRenderer,
        Executor pExecutor
    ) {
        CompletableFuture<Void> completablefuture = CompletableFuture.allOf(
            pStitchResults.values().stream().map(AtlasSet.StitchResult::readyForUpload).toArray(CompletableFuture[]::new)
        );
        final Multimap<String, Material> multimap = Multimaps.synchronizedMultimap(HashMultimap.create());
        final Multimap<String, String> multimap1 = Multimaps.synchronizedMultimap(HashMultimap.create());
        return pModelBakery.bakeModels(new SpriteGetter() {
                private final TextureAtlasSprite missingSprite = pStitchResults.get(TextureAtlas.LOCATION_BLOCKS).missing();

                @Override
                public TextureAtlasSprite get(Material p_375858_, ModelDebugName p_375833_) {
                    AtlasSet.StitchResult atlasset$stitchresult = pStitchResults.get(p_375858_.atlasLocation());
                    TextureAtlasSprite textureatlassprite = atlasset$stitchresult.getSprite(p_375858_.texture());
                    if (textureatlassprite != null) {
                        return textureatlassprite;
                    } else {
                        multimap.put(p_375833_.debugName(), p_375858_);
                        return atlasset$stitchresult.missing();
                    }
                }

                @Override
                public TextureAtlasSprite reportMissingReference(String p_378821_, ModelDebugName p_377684_) {
                    multimap1.put(p_377684_.debugName(), p_378821_);
                    return this.missingSprite;
                }
            }, pExecutor)
            .thenApply(
                p_389636_ -> {
                    net.minecraftforge.client.ForgeHooksClient.onModifyBakingResult(pModelBakery, p_389636_);
                    multimap.asMap()
                        .forEach(
                            (p_376688_, p_252017_) -> LOGGER.warn(
                                "Missing textures in model {}:\n{}",
                                p_376688_,
                                p_252017_.stream()
                                    .sorted(Material.COMPARATOR)
                                    .map(p_325574_ -> "    " + p_325574_.atlasLocation() + ":" + p_325574_.texture())
                                    .collect(Collectors.joining("\n"))
                            )
                        );
                    multimap1.asMap()
                        .forEach(
                            (p_374739_, p_374740_) -> LOGGER.warn(
                                "Missing texture references in model {}:\n{}",
                                p_374739_,
                                p_374740_.stream().sorted().map(p_374742_ -> "    " + p_374742_).collect(Collectors.joining("\n"))
                            )
                        );
                    Map<BlockState, BlockStateModel> map = createBlockStateToModelDispatch(p_389636_.blockStateModels(), p_389636_.missingModels().block());
                    return new ModelManager.ReloadState(p_389636_, pModelGroups, map, pStitchResults, pEntityModelSet, pSpecialBlockModelRenderer, completablefuture, pModelBakery);
                }
            );
    }

    private static Map<BlockState, BlockStateModel> createBlockStateToModelDispatch(Map<BlockState, BlockStateModel> pBlockStateModels, BlockStateModel pMissingModel) {
        Object object;
        try (Zone zone = Profiler.get().zone("block state dispatch")) {
            Map<BlockState, BlockStateModel> map = new IdentityHashMap<>(pBlockStateModels);

            for (Block block : BuiltInRegistries.BLOCK) {
                block.getStateDefinition().getPossibleStates().forEach(p_389628_ -> {
                    if (pBlockStateModels.putIfAbsent(p_389628_, pMissingModel) == null) {
                        LOGGER.warn("Missing model for variant: '{}'", p_389628_);
                    }
                });
            }

            object = map;
        }

        return (Map<BlockState, BlockStateModel>)object;
    }

    private static Object2IntMap<BlockState> buildModelGroups(BlockColors pBlockColors, BlockStateModelLoader.LoadedModels pLoadedModels) {
        Object2IntMap object2intmap;
        try (Zone zone = Profiler.get().zone("block groups")) {
            object2intmap = ModelGroupCollector.build(pBlockColors, pLoadedModels);
        }

        return object2intmap;
    }

    private void apply(ModelManager.ReloadState pReloadState, ProfilerFiller pProfiler) {
        pProfiler.push("upload");
        pReloadState.atlasPreparations.values().forEach(AtlasSet.StitchResult::upload);
        ModelBakery.BakingResult modelbakery$bakingresult = pReloadState.bakedModels;
        // TODO [BlockState Models] fix
        //this.bakedBlockStateModelsView = java.util.Collections.unmodifiableMap(this.bakedBlockStateModels);
        this.bakedItemStackModels = modelbakery$bakingresult.itemStackModels();
        this.bakedItemStackModelsView = java.util.Collections.unmodifiableMap(this.bakedItemStackModels);
        this.itemProperties = modelbakery$bakingresult.itemProperties();
        this.modelGroups = pReloadState.modelGroups;
        this.missingModels = modelbakery$bakingresult.missingModels();
        this.modelBakery = pReloadState.modelBakery();
        net.minecraftforge.client.ForgeHooksClient.onModelBake(this, this.modelBakery);
        pProfiler.popPush("cache");
        this.blockModelShaper.replaceCache(pReloadState.modelCache);
        this.specialBlockModelRenderer = pReloadState.specialBlockModelRenderer;
        this.entityModelSet = pReloadState.entityModelSet;
        pProfiler.pop();
    }

    public boolean requiresRender(BlockState pOldState, BlockState pNewState) {
        if (pOldState == pNewState) {
            return false;
        } else {
            int i = this.modelGroups.getInt(pOldState);
            if (i != -1) {
                int j = this.modelGroups.getInt(pNewState);
                if (i == j) {
                    FluidState fluidstate = pOldState.getFluidState();
                    FluidState fluidstate1 = pNewState.getFluidState();
                    return fluidstate != fluidstate1;
                }
            }

            return true;
        }
    }

    public TextureAtlas getAtlas(ResourceLocation pLocation) {
        if (this.atlases == null) throw new RuntimeException("getAtlasTexture called too early!");
        return this.atlases.getAtlas(pLocation);
    }

    @Override
    public void close() {
        this.atlases.close();
    }

    public void updateMaxMipLevel(int pLevel) {
        this.maxMipmapLevels = pLevel;
    }

    public ModelBakery getModelBakery() {
        return com.google.common.base.Preconditions.checkNotNull(modelBakery, "Attempted to query model bakery before it has been initialized.");
    }

    public Supplier<SpecialBlockModelRenderer> specialBlockModelRenderer() {
        return () -> this.specialBlockModelRenderer;
    }

    public Supplier<EntityModelSet> entityModels() {
        return () -> this.entityModelSet;
    }

    @OnlyIn(Dist.CLIENT)
    record ReloadState(
        ModelBakery.BakingResult bakedModels,
        Object2IntMap<BlockState> modelGroups,
        Map<BlockState, BlockStateModel> modelCache,
        Map<ResourceLocation, AtlasSet.StitchResult> atlasPreparations,
        EntityModelSet entityModelSet,
        SpecialBlockModelRenderer specialBlockModelRenderer,
        CompletableFuture<Void> readyForUpload,
        ModelBakery modelBakery
    ) {
    }

    @OnlyIn(Dist.CLIENT)
    record ResolvedModels(ResolvedModel missing, Map<ResourceLocation, ResolvedModel> models) {
    }
}
