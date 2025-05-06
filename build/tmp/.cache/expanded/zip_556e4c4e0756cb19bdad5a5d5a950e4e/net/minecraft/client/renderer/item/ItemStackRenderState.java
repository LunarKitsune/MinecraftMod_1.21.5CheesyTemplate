package net.minecraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class ItemStackRenderState {
    ItemDisplayContext displayContext = ItemDisplayContext.NONE;
    private int activeLayerCount;
    private ItemStackRenderState.LayerRenderState[] layers = new ItemStackRenderState.LayerRenderState[]{new ItemStackRenderState.LayerRenderState()};

    public void ensureCapacity(int pExpectedSize) {
        int i = this.layers.length;
        int j = this.activeLayerCount + pExpectedSize;
        if (j > i) {
            this.layers = Arrays.copyOf(this.layers, j);

            for (int k = i; k < j; k++) {
                this.layers[k] = new ItemStackRenderState.LayerRenderState();
            }
        }
    }

    public ItemStackRenderState.LayerRenderState newLayer() {
        this.ensureCapacity(1);
        return this.layers[this.activeLayerCount++];
    }

    public void clear() {
        this.displayContext = ItemDisplayContext.NONE;

        for (int i = 0; i < this.activeLayerCount; i++) {
            this.layers[i].clear();
        }

        this.activeLayerCount = 0;
    }

    private ItemStackRenderState.LayerRenderState firstLayer() {
        return this.layers[0];
    }

    public boolean isEmpty() {
        return this.activeLayerCount == 0;
    }

    public boolean usesBlockLight() {
        return this.firstLayer().usesBlockLight;
    }

    @Nullable
    public TextureAtlasSprite pickParticleIcon(RandomSource pRandom) {
        return this.activeLayerCount == 0 ? null : this.layers[pRandom.nextInt(this.activeLayerCount)].particleIcon;
    }

    public void visitExtents(Consumer<Vector3fc> pVisitor) {
        Vector3f vector3f = new Vector3f();
        PoseStack.Pose posestack$pose = new PoseStack.Pose();

        for (int i = 0; i < this.activeLayerCount; i++) {
            ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = this.layers[i];
            itemstackrenderstate$layerrenderstate.transform.apply(this.displayContext.leftHand(), posestack$pose);
            Matrix4f matrix4f = posestack$pose.pose();
            Vector3f[] avector3f = itemstackrenderstate$layerrenderstate.extents.get();

            for (Vector3f vector3f1 : avector3f) {
                pVisitor.accept(vector3f.set(vector3f1).mulPosition(matrix4f));
            }

            posestack$pose.setIdentity();
        }
    }

    public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
        for (int i = 0; i < this.activeLayerCount; i++) {
            this.layers[i].render(pPoseStack, pBufferSource, pPackedLight, pPackedOverlay);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum FoilType {
        NONE,
        STANDARD,
        SPECIAL;
    }

    @OnlyIn(Dist.CLIENT)
    public class LayerRenderState {
        private static final Vector3f[] NO_EXTENTS = new Vector3f[0];
        public static final Supplier<Vector3f[]> NO_EXTENTS_SUPPLIER = () -> NO_EXTENTS;
        private final List<BakedQuad> quads = new ArrayList<>();
        boolean usesBlockLight;
        @Nullable
        TextureAtlasSprite particleIcon;
        ItemTransform transform = ItemTransform.NO_TRANSFORM;
        @Nullable
        private RenderType renderType;
        private ItemStackRenderState.FoilType foilType = ItemStackRenderState.FoilType.NONE;
        private int[] tintLayers = new int[0];
        @Nullable
        private SpecialModelRenderer<Object> specialRenderer;
        @Nullable
        private Object argumentForSpecialRendering;
        Supplier<Vector3f[]> extents = NO_EXTENTS_SUPPLIER;

        public void clear() {
            this.quads.clear();
            this.renderType = null;
            this.foilType = ItemStackRenderState.FoilType.NONE;
            this.specialRenderer = null;
            this.argumentForSpecialRendering = null;
            Arrays.fill(this.tintLayers, -1);
            this.usesBlockLight = false;
            this.particleIcon = null;
            this.transform = ItemTransform.NO_TRANSFORM;
            this.extents = NO_EXTENTS_SUPPLIER;
        }

        public List<BakedQuad> prepareQuadList() {
            return this.quads;
        }

        public void setRenderType(RenderType pRenderType) {
            this.renderType = pRenderType;
        }

        public void setUsesBlockLight(boolean pUsesBlockLight) {
            this.usesBlockLight = pUsesBlockLight;
        }

        public void setExtents(Supplier<Vector3f[]> pExtents) {
            this.extents = pExtents;
        }

        public void setParticleIcon(TextureAtlasSprite pParticleIcon) {
            this.particleIcon = pParticleIcon;
        }

        public void setTransform(ItemTransform pTransform) {
            this.transform = pTransform;
        }

        public <T> void setupSpecialModel(SpecialModelRenderer<T> pRenderer, @Nullable T pArgument) {
            this.specialRenderer = eraseSpecialRenderer(pRenderer);
            this.argumentForSpecialRendering = pArgument;
        }

        private static SpecialModelRenderer<Object> eraseSpecialRenderer(SpecialModelRenderer<?> pSpecialRenderer) {
            return (SpecialModelRenderer<Object>)pSpecialRenderer;
        }

        public void setFoilType(ItemStackRenderState.FoilType pFoilType) {
            this.foilType = pFoilType;
        }

        public int[] prepareTintLayers(int pCount) {
            if (pCount > this.tintLayers.length) {
                this.tintLayers = new int[pCount];
                Arrays.fill(this.tintLayers, -1);
            }

            return this.tintLayers;
        }

        void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
            pPoseStack.pushPose();
            this.transform.apply(ItemStackRenderState.this.displayContext.leftHand(), pPoseStack.last());
            if (this.specialRenderer != null) {
                this.specialRenderer
                    .render(
                        this.argumentForSpecialRendering,
                        ItemStackRenderState.this.displayContext,
                        pPoseStack,
                        pBufferSource,
                        pPackedLight,
                        pPackedOverlay,
                        this.foilType != ItemStackRenderState.FoilType.NONE
                    );
            } else if (this.renderType != null) {
                ItemRenderer.renderItem(
                    ItemStackRenderState.this.displayContext,
                    pPoseStack,
                    pBufferSource,
                    pPackedLight,
                    pPackedOverlay,
                    this.tintLayers,
                    this.quads,
                    this.renderType,
                    this.foilType
                );
            }

            pPoseStack.popPose();
        }
    }
}