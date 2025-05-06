package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SpecialBlockModelRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockRenderDispatcher implements ResourceManagerReloadListener {
    private final BlockModelShaper blockModelShaper;
    private final ModelBlockRenderer modelRenderer;
    private final Supplier<SpecialBlockModelRenderer> specialBlockModelRenderer;
    private final LiquidBlockRenderer liquidBlockRenderer;
    private final RandomSource singleThreadRandom = RandomSource.create();
    private final List<BlockModelPart> singleThreadPartList = new ArrayList<>();
    private final BlockColors blockColors;

    public BlockRenderDispatcher(BlockModelShaper pBlockModelShaper, Supplier<SpecialBlockModelRenderer> pSpecialBlockModelRenderer, BlockColors pBlockColors) {
        this.blockModelShaper = pBlockModelShaper;
        this.specialBlockModelRenderer = pSpecialBlockModelRenderer;
        this.blockColors = pBlockColors;
        this.modelRenderer = new ModelBlockRenderer(this.blockColors);
        this.liquidBlockRenderer = new LiquidBlockRenderer();
    }

    public BlockModelShaper getBlockModelShaper() {
        return this.blockModelShaper;
    }

    @Deprecated //Forge: Model data parameter
    public void renderBreakingTexture(BlockState pState, BlockPos pPos, BlockAndTintGetter pLevel, PoseStack pPoseStack, VertexConsumer pConsumer) {
        renderBreakingTexture(pState, pPos, pLevel, pPoseStack, pConsumer, net.minecraftforge.client.model.data.ModelData.EMPTY);
    }

    public void renderBreakingTexture(BlockState pState, BlockPos pPos, BlockAndTintGetter pLevel, PoseStack pPoseStack, VertexConsumer pConsumer, net.minecraftforge.client.model.data.ModelData modelData) {
        if (pState.getRenderShape() == RenderShape.MODEL) {
            BlockStateModel blockstatemodel = this.blockModelShaper.getBlockModel(pState);
            this.singleThreadRandom.setSeed(pState.getSeed(pPos));
            this.singleThreadPartList.clear();
            blockstatemodel.collectParts(this.singleThreadRandom, this.singleThreadPartList, modelData, null);
            this.modelRenderer.tesselateBlock(pLevel, this.singleThreadPartList, pState, pPos, pPoseStack, pConsumer, true, OverlayTexture.NO_OVERLAY);
        }
    }

    public void renderBatched(
        BlockState pState,
        BlockPos pPos,
        BlockAndTintGetter pLevel,
        PoseStack pPoseStack,
        VertexConsumer pConsumer,
        boolean pCheckSides,
        List<BlockModelPart> pParts
    ) {
        try {
            this.modelRenderer.tesselateBlock(pLevel, pParts, pState, pPos, pPoseStack, pConsumer, pCheckSides, OverlayTexture.NO_OVERLAY);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating block in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being tesselated");
            CrashReportCategory.populateBlockDetails(crashreportcategory, pLevel, pPos, pState);
            throw new ReportedException(crashreport);
        }
    }

    public void renderLiquid(BlockPos pPos, BlockAndTintGetter pLevel, VertexConsumer pConsumer, BlockState pBlockState, FluidState pFluidState) {
        try {
            this.liquidBlockRenderer.tesselate(pLevel, pPos, pConsumer, pBlockState, pFluidState);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating liquid in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being tesselated");
            CrashReportCategory.populateBlockDetails(crashreportcategory, pLevel, pPos, pBlockState);
            throw new ReportedException(crashreport);
        }
    }

    public ModelBlockRenderer getModelRenderer() {
        return this.modelRenderer;
    }

    public BlockStateModel getBlockModel(BlockState pState) {
        return this.blockModelShaper.getBlockModel(pState);
    }

    @Deprecated //Forge: Model data and render type parameter
    public void renderSingleBlock(BlockState pState, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
        renderSingleBlock(pState, pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, net.minecraftforge.client.model.data.ModelData.EMPTY, null);
    }

    public void renderSingleBlock(BlockState pState, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay, net.minecraftforge.client.model.data.ModelData modelData, net.minecraft.client.renderer.RenderType renderType) {
        RenderShape rendershape = pState.getRenderShape();
        if (rendershape != RenderShape.INVISIBLE) {
            BlockStateModel blockstatemodel = this.getBlockModel(pState);
            int i = this.blockColors.getColor(pState, null, null, 0);
            float f = (i >> 16 & 0xFF) / 255.0F;
            float f1 = (i >> 8 & 0xFF) / 255.0F;
            float f2 = (i & 0xFF) / 255.0F;
            for (var rt : blockstatemodel.getRenderTypes(pState, RandomSource.create(42), modelData))
            ModelBlockRenderer.renderModel(
                pPoseStack.last(), pBufferSource.getBuffer(renderType != null ? renderType : net.minecraftforge.client.RenderTypeHelper.getEntityRenderType(rt)), blockstatemodel, f, f1, f2, pPackedLight, pPackedOverlay, modelData, rt
            );
            this.specialBlockModelRenderer.get().renderByBlock(pState.getBlock(), ItemDisplayContext.NONE, pPoseStack, pBufferSource, pPackedLight, pPackedOverlay);
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager pResourceManager) {
        this.liquidBlockRenderer.setupSprites();
    }
}
