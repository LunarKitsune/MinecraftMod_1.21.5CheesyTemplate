package net.minecraft.client.resources.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectFunction;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ModelDiscovery {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Object2ObjectMap<ResourceLocation, ModelDiscovery.ModelWrapper> modelWrappers = new Object2ObjectOpenHashMap<>();
    private final ModelDiscovery.ModelWrapper missingModel;
    private final Object2ObjectFunction<ResourceLocation, ModelDiscovery.ModelWrapper> uncachedResolver;
    private final ResolvableModel.Resolver resolver;
    private final Queue<ModelDiscovery.ModelWrapper> parentDiscoveryQueue = new ArrayDeque<>();

    public ModelDiscovery(Map<ResourceLocation, UnbakedModel> pInputModels, UnbakedModel pMissingModel) {
        this.missingModel = new ModelDiscovery.ModelWrapper(MissingBlockModel.LOCATION, pMissingModel, true);
        this.modelWrappers.put(MissingBlockModel.LOCATION, this.missingModel);
        this.uncachedResolver = p_389603_ -> {
            ResourceLocation resourcelocation = (ResourceLocation)p_389603_;
            UnbakedModel unbakedmodel = pInputModels.get(resourcelocation);
            if (unbakedmodel == null) {
                LOGGER.warn("Missing block model: {}", resourcelocation);
                return this.missingModel;
            } else {
                return this.createAndQueueWrapper(resourcelocation, unbakedmodel);
            }
        };
        this.resolver = this::getOrCreateModel;
    }

    private static boolean isRoot(UnbakedModel pModel) {
        return pModel.parent() == null;
    }

    private ModelDiscovery.ModelWrapper getOrCreateModel(ResourceLocation pLocation) {
        return this.modelWrappers.computeIfAbsent(pLocation, this.uncachedResolver);
    }

    private ModelDiscovery.ModelWrapper createAndQueueWrapper(ResourceLocation pId, UnbakedModel pModel) {
        boolean flag = isRoot(pModel);
        ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper = new ModelDiscovery.ModelWrapper(pId, pModel, flag);
        if (!flag) {
            this.parentDiscoveryQueue.add(modeldiscovery$modelwrapper);
        }

        return modeldiscovery$modelwrapper;
    }

    public void addRoot(ResolvableModel pModel) {
        pModel.resolveDependencies(this.resolver);
    }

    public void addSpecialModel(ResourceLocation pId, UnbakedModel pModel) {
        if (!isRoot(pModel)) {
            LOGGER.warn("Trying to add non-root special model {}, ignoring", pId);
        } else {
            ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper = this.modelWrappers.put(pId, this.createAndQueueWrapper(pId, pModel));
            if (modeldiscovery$modelwrapper != null) {
                LOGGER.warn("Duplicate special model {}", pId);
            }
        }
    }

    public ResolvedModel missingModel() {
        return this.missingModel;
    }

    public Map<ResourceLocation, ResolvedModel> resolve() {
        List<ModelDiscovery.ModelWrapper> list = new ArrayList<>();
        this.discoverDependencies(list);
        propagateValidity(list);
        Builder<ResourceLocation, ResolvedModel> builder = ImmutableMap.builder();
        this.modelWrappers.forEach((p_389605_, p_389606_) -> {
            if (p_389606_.valid) {
                builder.put(p_389605_, p_389606_);
            } else {
                LOGGER.warn("Model {} ignored due to cyclic dependency", p_389605_);
            }
        });
        return builder.build();
    }

    private void discoverDependencies(List<ModelDiscovery.ModelWrapper> pWrappers) {
        ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper;
        while ((modeldiscovery$modelwrapper = this.parentDiscoveryQueue.poll()) != null) {
            ResourceLocation resourcelocation = Objects.requireNonNull(modeldiscovery$modelwrapper.wrapped.parent());
            ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper1 = this.getOrCreateModel(resourcelocation);
            modeldiscovery$modelwrapper.parent = modeldiscovery$modelwrapper1;
            if (modeldiscovery$modelwrapper1.valid) {
                modeldiscovery$modelwrapper.valid = true;
            } else {
                pWrappers.add(modeldiscovery$modelwrapper);
            }
        }
    }

    private static void propagateValidity(List<ModelDiscovery.ModelWrapper> pWrappers) {
        boolean flag = true;

        while (flag) {
            flag = false;
            Iterator<ModelDiscovery.ModelWrapper> iterator = pWrappers.iterator();

            while (iterator.hasNext()) {
                ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper = iterator.next();
                if (Objects.requireNonNull(modeldiscovery$modelwrapper.parent).valid) {
                    modeldiscovery$modelwrapper.valid = true;
                    iterator.remove();
                    flag = true;
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ModelWrapper implements ResolvedModel {
        private static final ModelDiscovery.Slot<Boolean> KEY_AMBIENT_OCCLUSION = slot(0);
        private static final ModelDiscovery.Slot<UnbakedModel.GuiLight> KEY_GUI_LIGHT = slot(1);
        private static final ModelDiscovery.Slot<UnbakedGeometry> KEY_GEOMETRY = slot(2);
        private static final ModelDiscovery.Slot<ItemTransforms> KEY_TRANSFORMS = slot(3);
        private static final ModelDiscovery.Slot<TextureSlots> KEY_TEXTURE_SLOTS = slot(4);
        private static final ModelDiscovery.Slot<TextureAtlasSprite> KEY_PARTICLE_SPRITE = slot(5);
        private static final ModelDiscovery.Slot<QuadCollection> KEY_DEFAULT_GEOMETRY = slot(6);
        private static final int SLOT_COUNT = 7;
        private final ResourceLocation id;
        boolean valid;
        @Nullable
        ModelDiscovery.ModelWrapper parent;
        final UnbakedModel wrapped;
        private final AtomicReferenceArray<Object> fixedSlots = new AtomicReferenceArray<>(7);
        private final Map<ModelState, QuadCollection> modelBakeCache = new ConcurrentHashMap<>();

        private static <T> ModelDiscovery.Slot<T> slot(int pIndex) {
            Objects.checkIndex(pIndex, 7);
            return new ModelDiscovery.Slot<>(pIndex);
        }

        ModelWrapper(ResourceLocation pId, UnbakedModel pWrapped, boolean pValid) {
            this.id = pId;
            this.wrapped = pWrapped;
            this.valid = pValid;
        }

        @Override
        public UnbakedModel wrapped() {
            return this.wrapped;
        }

        @Nullable
        @Override
        public ResolvedModel parent() {
            return this.parent;
        }

        @Nullable private net.minecraftforge.client.model.geometry.ModelContext context = null;
        @Override
        public net.minecraftforge.client.model.geometry.IGeometryBakingContext getContext() {
            if (context == null || context.parent() != parent)
                context = new net.minecraftforge.client.model.geometry.ModelContext(this);
            return context;
        }

        @Override
        public String debugName() {
            return this.id.toString();
        }

        @Nullable
        private <T> T getSlot(ModelDiscovery.Slot<T> pSlot) {
            return (T)this.fixedSlots.get(pSlot.index);
        }

        private <T> T updateSlot(ModelDiscovery.Slot<T> pSlot, T pValue) {
            T t = (T)this.fixedSlots.compareAndExchange(pSlot.index, null, pValue);
            return t == null ? pValue : t;
        }

        private <T> T getSimpleProperty(ModelDiscovery.Slot<T> pSlot, Function<ResolvedModel, T> pPropertyGetter) {
            T t = this.getSlot(pSlot);
            return t != null ? t : this.updateSlot(pSlot, pPropertyGetter.apply(this));
        }

        @Override
        public boolean getTopAmbientOcclusion() {
            return this.getSimpleProperty(KEY_AMBIENT_OCCLUSION, ResolvedModel::findTopAmbientOcclusion);
        }

        @Override
        public UnbakedModel.GuiLight getTopGuiLight() {
            return this.getSimpleProperty(KEY_GUI_LIGHT, ResolvedModel::findTopGuiLight);
        }

        @Override
        public ItemTransforms getTopTransforms() {
            return this.getSimpleProperty(KEY_TRANSFORMS, ResolvedModel::findTopTransforms);
        }

        @Override
        public UnbakedGeometry getTopGeometry() {
            return this.getSimpleProperty(KEY_GEOMETRY, ResolvedModel::findTopGeometry);
        }

        @Override
        public TextureSlots getTopTextureSlots() {
            return this.getSimpleProperty(KEY_TEXTURE_SLOTS, ResolvedModel::findTopTextureSlots);
        }

        @Override
        public TextureAtlasSprite resolveParticleSprite(TextureSlots p_396706_, ModelBaker p_393999_) {
            TextureAtlasSprite textureatlassprite = this.getSlot(KEY_PARTICLE_SPRITE);
            return textureatlassprite != null ? textureatlassprite : this.updateSlot(KEY_PARTICLE_SPRITE, ResolvedModel.resolveParticleSprite(p_396706_, p_393999_, this));
        }

        private QuadCollection bakeDefaultState(TextureSlots pTextureSlots, ModelBaker pModelBaker, ModelState pModelState) {
            QuadCollection quadcollection = this.getSlot(KEY_DEFAULT_GEOMETRY);
            return quadcollection != null ? quadcollection : this.updateSlot(KEY_DEFAULT_GEOMETRY, this.getTopGeometry().bake(pTextureSlots, pModelBaker, pModelState, this, getContext()));
        }

        @Override
        public QuadCollection bakeTopGeometry(TextureSlots p_396404_, ModelBaker p_391625_, ModelState p_396681_) {
            return p_396681_ == BlockModelRotation.X0_Y0
                ? this.bakeDefaultState(p_396404_, p_391625_, p_396681_)
                : this.modelBakeCache.computeIfAbsent(p_396681_, p_394933_ -> {
                    UnbakedGeometry unbakedgeometry = this.getTopGeometry();
                    return unbakedgeometry.bake(p_396404_, p_391625_, p_394933_, this, getContext());
                });
        }
    }

    @OnlyIn(Dist.CLIENT)
    record Slot<T>(int index) {
    }
}
