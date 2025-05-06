package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import java.util.List;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelBlockRenderer {
    private static final Direction[] DIRECTIONS = Direction.values();
    private final BlockColors blockColors;
    private static final int CACHE_SIZE = 100;
    static final ThreadLocal<ModelBlockRenderer.Cache> CACHE = ThreadLocal.withInitial(ModelBlockRenderer.Cache::new);

    public ModelBlockRenderer(BlockColors pBlockColors) {
        this.blockColors = pBlockColors;
    }

    public void tesselateBlock(
        BlockAndTintGetter pLevel,
        List<BlockModelPart> pParts,
        BlockState pState,
        BlockPos pPos,
        PoseStack pPoseStack,
        VertexConsumer pConsumer,
        boolean pCheckSides,
        int pPackedOverlay
    ) {
        if (!pParts.isEmpty()) {
            boolean flag = Minecraft.useAmbientOcclusion() && pState.getLightEmission(pLevel, pPos) == 0 && pParts.getFirst().useAmbientOcclusion();
            pPoseStack.translate(pState.getOffset(pPos));

            try {
                if (flag) {
                    this.tesselateWithAO(pLevel, pParts, pState, pPos, pPoseStack, pConsumer, pCheckSides, pPackedOverlay);
                } else {
                    this.tesselateWithoutAO(pLevel, pParts, pState, pPos, pPoseStack, pConsumer, pCheckSides, pPackedOverlay);
                }
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating block model");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Block model being tesselated");
                CrashReportCategory.populateBlockDetails(crashreportcategory, pLevel, pPos, pState);
                crashreportcategory.setDetail("Using AO", flag);
                throw new ReportedException(crashreport);
            }
        }
    }

    private static boolean shouldRenderFace(BlockAndTintGetter pLevel, BlockState pState, boolean pCheckSides, Direction pFace, BlockPos pPos) {
        if (!pCheckSides) {
            return true;
        } else {
            BlockState blockstate = pLevel.getBlockState(pPos);
            return Block.shouldRenderFace(pState, blockstate, pFace);
        }
    }

    public void tesselateWithAO(
        BlockAndTintGetter pLevel,
        List<BlockModelPart> pParts,
        BlockState pState,
        BlockPos pPos,
        PoseStack pPoseStack,
        VertexConsumer pConsumer,
        boolean pCheckSides,
        int pPackedOverlay
    ) {
        ModelBlockRenderer.AmbientOcclusionRenderStorage modelblockrenderer$ambientocclusionrenderstorage = new ModelBlockRenderer.AmbientOcclusionRenderStorage();
        int i = 0;
        int j = 0;

        for (BlockModelPart blockmodelpart : pParts) {
            for (Direction direction : DIRECTIONS) {
                int k = 1 << direction.ordinal();
                boolean flag = (i & k) == 1;
                boolean flag1 = (j & k) == 1;
                if (!flag || flag1) {
                    List<BakedQuad> list = blockmodelpart.getQuads(direction);
                    if (!list.isEmpty()) {
                        if (!flag) {
                            flag1 = shouldRenderFace(
                                pLevel,
                                pState,
                                pCheckSides,
                                direction,
                                modelblockrenderer$ambientocclusionrenderstorage.scratchPos.setWithOffset(pPos, direction)
                            );
                            i |= k;
                            if (flag1) {
                                j |= k;
                            }
                        }

                        if (flag1) {
                            this.renderModelFaceAO(
                                pLevel, pState, pPos, pPoseStack, pConsumer, list, modelblockrenderer$ambientocclusionrenderstorage, pPackedOverlay
                            );
                        }
                    }
                }
            }

            List<BakedQuad> list1 = blockmodelpart.getQuads(null);
            if (!list1.isEmpty()) {
                this.renderModelFaceAO(pLevel, pState, pPos, pPoseStack, pConsumer, list1, modelblockrenderer$ambientocclusionrenderstorage, pPackedOverlay);
            }
        }
    }

    public void tesselateWithoutAO(
        BlockAndTintGetter pLevel,
        List<BlockModelPart> pParts,
        BlockState pState,
        BlockPos pPos,
        PoseStack pPoseStack,
        VertexConsumer pConsumer,
        boolean pCheckSides,
        int pPackedOverlay
    ) {
        ModelBlockRenderer.CommonRenderStorage modelblockrenderer$commonrenderstorage = new ModelBlockRenderer.CommonRenderStorage();
        int i = 0;
        int j = 0;

        for (BlockModelPart blockmodelpart : pParts) {
            for (Direction direction : DIRECTIONS) {
                int k = 1 << direction.ordinal();
                boolean flag = (i & k) == 1;
                boolean flag1 = (j & k) == 1;
                if (!flag || flag1) {
                    List<BakedQuad> list = blockmodelpart.getQuads(direction);
                    if (!list.isEmpty()) {
                        BlockPos blockpos = modelblockrenderer$commonrenderstorage.scratchPos.setWithOffset(pPos, direction);
                        if (!flag) {
                            flag1 = shouldRenderFace(pLevel, pState, pCheckSides, direction, blockpos);
                            i |= k;
                            if (flag1) {
                                j |= k;
                            }
                        }

                        if (flag1) {
                            int l = modelblockrenderer$commonrenderstorage.cache.getLightColor(pState, pLevel, blockpos);
                            this.renderModelFaceFlat(
                                pLevel, pState, pPos, l, pPackedOverlay, false, pPoseStack, pConsumer, list, modelblockrenderer$commonrenderstorage
                            );
                        }
                    }
                }
            }

            List<BakedQuad> list1 = blockmodelpart.getQuads(null);
            if (!list1.isEmpty()) {
                this.renderModelFaceFlat(pLevel, pState, pPos, -1, pPackedOverlay, true, pPoseStack, pConsumer, list1, modelblockrenderer$commonrenderstorage);
            }
        }
    }

    private void renderModelFaceAO(
        BlockAndTintGetter pLevel,
        BlockState pState,
        BlockPos pPos,
        PoseStack pPoseStack,
        VertexConsumer pConsumer,
        List<BakedQuad> pQuads,
        ModelBlockRenderer.AmbientOcclusionRenderStorage pRenderStorage,
        int pPackedOverlay
    ) {
        for (BakedQuad bakedquad : pQuads) {
            calculateShape(pLevel, pState, pPos, bakedquad.vertices(), bakedquad.direction(), pRenderStorage);
            pRenderStorage.calculate(pLevel, pState, pPos, bakedquad.direction(), bakedquad.shade());
            this.putQuadData(pLevel, pState, pPos, pConsumer, pPoseStack.last(), bakedquad, pRenderStorage, pPackedOverlay);
        }
    }

    private void putQuadData(
        BlockAndTintGetter pLevel,
        BlockState pState,
        BlockPos pPos,
        VertexConsumer pConsumer,
        PoseStack.Pose pPose,
        BakedQuad pQuad,
        ModelBlockRenderer.CommonRenderStorage pRenderStorage,
        int pPackedOverlay
    ) {
        int i = pQuad.tintIndex();
        float f;
        float f1;
        float f2;
        if (i != -1) {
            int j;
            if (pRenderStorage.tintCacheIndex == i) {
                j = pRenderStorage.tintCacheValue;
            } else {
                j = this.blockColors.getColor(pState, pLevel, pPos, i);
                pRenderStorage.tintCacheIndex = i;
                pRenderStorage.tintCacheValue = j;
            }

            f = ARGB.redFloat(j);
            f1 = ARGB.greenFloat(j);
            f2 = ARGB.blueFloat(j);
        } else {
            f = 1.0F;
            f1 = 1.0F;
            f2 = 1.0F;
        }

        pConsumer.putBulkData(pPose, pQuad, pRenderStorage.brightness, f, f1, f2, 1.0F, pRenderStorage.lightmap, pPackedOverlay, true);
    }

    private static void calculateShape(
        BlockAndTintGetter pLevel,
        BlockState pState,
        BlockPos pPos,
        int[] pVerticies,
        Direction pDirection,
        ModelBlockRenderer.CommonRenderStorage pRenderStorage
    ) {
        float f = 32.0F;
        float f1 = 32.0F;
        float f2 = 32.0F;
        float f3 = -32.0F;
        float f4 = -32.0F;
        float f5 = -32.0F;

        for (int i = 0; i < 4; i++) {
            float f6 = Float.intBitsToFloat(pVerticies[i * 8]);
            float f7 = Float.intBitsToFloat(pVerticies[i * 8 + 1]);
            float f8 = Float.intBitsToFloat(pVerticies[i * 8 + 2]);
            f = Math.min(f, f6);
            f1 = Math.min(f1, f7);
            f2 = Math.min(f2, f8);
            f3 = Math.max(f3, f6);
            f4 = Math.max(f4, f7);
            f5 = Math.max(f5, f8);
        }

        if (pRenderStorage instanceof ModelBlockRenderer.AmbientOcclusionRenderStorage modelblockrenderer$ambientocclusionrenderstorage) {
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.WEST.index] = f;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.EAST.index] = f3;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.DOWN.index] = f1;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.UP.index] = f4;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.NORTH.index] = f2;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.SOUTH.index] = f5;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_WEST.index] = 1.0F - f;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_EAST.index] = 1.0F - f3;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_DOWN.index] = 1.0F - f1;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_UP.index] = 1.0F - f4;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_NORTH.index] = 1.0F - f2;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_SOUTH.index] = 1.0F - f5;
        }

        float f9 = 1.0E-4F;
        float f10 = 0.9999F;

        pRenderStorage.facePartial = switch (pDirection) {
            case DOWN, UP -> f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F;
            case NORTH, SOUTH -> f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F;
            case WEST, EAST -> f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F;
        };

        pRenderStorage.faceCubic = switch (pDirection) {
            case DOWN -> f1 == f4 && (f1 < 1.0E-4F || pState.isCollisionShapeFullBlock(pLevel, pPos));
            case UP -> f1 == f4 && (f4 > 0.9999F || pState.isCollisionShapeFullBlock(pLevel, pPos));
            case NORTH -> f2 == f5 && (f2 < 1.0E-4F || pState.isCollisionShapeFullBlock(pLevel, pPos));
            case SOUTH -> f2 == f5 && (f5 > 0.9999F || pState.isCollisionShapeFullBlock(pLevel, pPos));
            case WEST -> f == f3 && (f < 1.0E-4F || pState.isCollisionShapeFullBlock(pLevel, pPos));
            case EAST -> f == f3 && (f3 > 0.9999F || pState.isCollisionShapeFullBlock(pLevel, pPos));
        };
    }

    private void renderModelFaceFlat(
        BlockAndTintGetter pLevel,
        BlockState pState,
        BlockPos pPos,
        int pPackedLight,
        int pPackedOverlay,
        boolean pRepackLight,
        PoseStack pPoseStack,
        VertexConsumer pConsumer,
        List<BakedQuad> pQuads,
        ModelBlockRenderer.CommonRenderStorage pRenderStorage
    ) {
        for (BakedQuad bakedquad : pQuads) {
            if (pRepackLight) {
                calculateShape(pLevel, pState, pPos, bakedquad.vertices(), bakedquad.direction(), pRenderStorage);
                BlockPos blockpos = (BlockPos)(pRenderStorage.faceCubic ? pRenderStorage.scratchPos.setWithOffset(pPos, bakedquad.direction()) : pPos);
                pPackedLight = pRenderStorage.cache.getLightColor(pState, pLevel, blockpos);
            }

            float f = pLevel.getShade(bakedquad.direction(), bakedquad.shade());
            pRenderStorage.brightness[0] = f;
            pRenderStorage.brightness[1] = f;
            pRenderStorage.brightness[2] = f;
            pRenderStorage.brightness[3] = f;
            pRenderStorage.lightmap[0] = pPackedLight;
            pRenderStorage.lightmap[1] = pPackedLight;
            pRenderStorage.lightmap[2] = pPackedLight;
            pRenderStorage.lightmap[3] = pPackedLight;
            this.putQuadData(pLevel, pState, pPos, pConsumer, pPoseStack.last(), bakedquad, pRenderStorage, pPackedOverlay);
        }
    }

    @Deprecated //Forge: Model data and render type parameter
    public static void renderModel(PoseStack.Pose pPose, VertexConsumer pConsumer, BlockStateModel pModel, float pRed, float pGreen, float pBlue, int pPackedLight, int pPackedOverlay) {
        renderModel(pPose, pConsumer, pModel, pRed, pGreen, pBlue, pPackedLight, pPackedOverlay, net.minecraftforge.client.model.data.ModelData.EMPTY, null);
    }

    public static void renderModel(
        PoseStack.Pose pPose,
        VertexConsumer pConsumer,
        BlockStateModel pModel,
        float pRed,
        float pGreen,
        float pBlue,
        int pPackedLight,
        int pPackedOverlay,
        net.minecraftforge.client.model.data.ModelData modelData,
        @org.jetbrains.annotations.Nullable net.minecraft.client.renderer.RenderType renderType
    ) {
        for (BlockModelPart blockmodelpart : pModel.collectParts(RandomSource.create(42L), modelData, renderType)) {
            for (Direction direction : DIRECTIONS) {
                renderQuadList(pPose, pConsumer, pRed, pGreen, pBlue, blockmodelpart.getQuads(direction), pPackedLight, pPackedOverlay);
            }

            renderQuadList(pPose, pConsumer, pRed, pGreen, pBlue, blockmodelpart.getQuads(null), pPackedLight, pPackedOverlay);
        }
    }

    private static void renderQuadList(
        PoseStack.Pose pPose,
        VertexConsumer pConsumer,
        float pRed,
        float pGreen,
        float pBlue,
        List<BakedQuad> pQuads,
        int pPackedLight,
        int pPackedOverlay
    ) {
        for (BakedQuad bakedquad : pQuads) {
            float f;
            float f1;
            float f2;
            if (bakedquad.isTinted()) {
                f = Mth.clamp(pRed, 0.0F, 1.0F);
                f1 = Mth.clamp(pGreen, 0.0F, 1.0F);
                f2 = Mth.clamp(pBlue, 0.0F, 1.0F);
            } else {
                f = 1.0F;
                f1 = 1.0F;
                f2 = 1.0F;
            }

            pConsumer.putBulkData(pPose, bakedquad, f, f1, f2, 1.0F, pPackedLight, pPackedOverlay);
        }
    }

    public static void enableCaching() {
        CACHE.get().enable();
    }

    public static void clearCache() {
        CACHE.get().disable();
    }

    @OnlyIn(Dist.CLIENT)
    protected static enum AdjacencyInfo {
        DOWN(
            new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH},
            0.5F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.SOUTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.SOUTH
            }
        ),
        UP(
            new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH},
            1.0F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.SOUTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.SOUTH
            }
        ),
        NORTH(
            new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST},
            0.8F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_WEST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_EAST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_EAST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_WEST
            }
        ),
        SOUTH(
            new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP},
            0.8F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.WEST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.WEST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.EAST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.EAST
            }
        ),
        WEST(
            new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH},
            0.6F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.SOUTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.SOUTH
            }
        ),
        EAST(
            new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH},
            0.6F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.SOUTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.SOUTH
            }
        );

        final Direction[] corners;
        final boolean doNonCubicWeight;
        final ModelBlockRenderer.SizeInfo[] vert0Weights;
        final ModelBlockRenderer.SizeInfo[] vert1Weights;
        final ModelBlockRenderer.SizeInfo[] vert2Weights;
        final ModelBlockRenderer.SizeInfo[] vert3Weights;
        private static final ModelBlockRenderer.AdjacencyInfo[] BY_FACING = Util.make(new ModelBlockRenderer.AdjacencyInfo[6], p_111134_ -> {
            p_111134_[Direction.DOWN.get3DDataValue()] = DOWN;
            p_111134_[Direction.UP.get3DDataValue()] = UP;
            p_111134_[Direction.NORTH.get3DDataValue()] = NORTH;
            p_111134_[Direction.SOUTH.get3DDataValue()] = SOUTH;
            p_111134_[Direction.WEST.get3DDataValue()] = WEST;
            p_111134_[Direction.EAST.get3DDataValue()] = EAST;
        });

        private AdjacencyInfo(
            final Direction[] pCorners,
            final float pShadeBrightness,
            final boolean pDoNonCubicWeight,
            final ModelBlockRenderer.SizeInfo[] pVert0Weights,
            final ModelBlockRenderer.SizeInfo[] pVert1Weights,
            final ModelBlockRenderer.SizeInfo[] pVert2Weights,
            final ModelBlockRenderer.SizeInfo[] pVert3Weights
        ) {
            this.corners = pCorners;
            this.doNonCubicWeight = pDoNonCubicWeight;
            this.vert0Weights = pVert0Weights;
            this.vert1Weights = pVert1Weights;
            this.vert2Weights = pVert2Weights;
            this.vert3Weights = pVert3Weights;
        }

        public static ModelBlockRenderer.AdjacencyInfo fromFacing(Direction pFacing) {
            return BY_FACING[pFacing.get3DDataValue()];
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class AmbientOcclusionRenderStorage extends ModelBlockRenderer.CommonRenderStorage {
        final float[] faceShape = new float[ModelBlockRenderer.SizeInfo.COUNT];

        public AmbientOcclusionRenderStorage() {
        }

        public void calculate(BlockAndTintGetter pLevel, BlockState pState, BlockPos pPos, Direction pDirection, boolean pShade) {
            BlockPos blockpos = this.faceCubic ? pPos.relative(pDirection) : pPos;
            ModelBlockRenderer.AdjacencyInfo modelblockrenderer$adjacencyinfo = ModelBlockRenderer.AdjacencyInfo.fromFacing(pDirection);
            BlockPos.MutableBlockPos blockpos$mutableblockpos = this.scratchPos;
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[0]);
            BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos);
            int i = this.cache.getLightColor(blockstate, pLevel, blockpos$mutableblockpos);
            float f = this.cache.getShadeBrightness(blockstate, pLevel, blockpos$mutableblockpos);
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[1]);
            BlockState blockstate1 = pLevel.getBlockState(blockpos$mutableblockpos);
            int j = this.cache.getLightColor(blockstate1, pLevel, blockpos$mutableblockpos);
            float f1 = this.cache.getShadeBrightness(blockstate1, pLevel, blockpos$mutableblockpos);
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[2]);
            BlockState blockstate2 = pLevel.getBlockState(blockpos$mutableblockpos);
            int k = this.cache.getLightColor(blockstate2, pLevel, blockpos$mutableblockpos);
            float f2 = this.cache.getShadeBrightness(blockstate2, pLevel, blockpos$mutableblockpos);
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[3]);
            BlockState blockstate3 = pLevel.getBlockState(blockpos$mutableblockpos);
            int l = this.cache.getLightColor(blockstate3, pLevel, blockpos$mutableblockpos);
            float f3 = this.cache.getShadeBrightness(blockstate3, pLevel, blockpos$mutableblockpos);
            BlockState blockstate4 = pLevel.getBlockState(
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[0]).move(pDirection)
            );
            boolean flag = !blockstate4.isViewBlocking(pLevel, blockpos$mutableblockpos) || blockstate4.getLightBlock() == 0;
            BlockState blockstate5 = pLevel.getBlockState(
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[1]).move(pDirection)
            );
            boolean flag1 = !blockstate5.isViewBlocking(pLevel, blockpos$mutableblockpos) || blockstate5.getLightBlock() == 0;
            BlockState blockstate6 = pLevel.getBlockState(
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[2]).move(pDirection)
            );
            boolean flag2 = !blockstate6.isViewBlocking(pLevel, blockpos$mutableblockpos) || blockstate6.getLightBlock() == 0;
            BlockState blockstate7 = pLevel.getBlockState(
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[3]).move(pDirection)
            );
            boolean flag3 = !blockstate7.isViewBlocking(pLevel, blockpos$mutableblockpos) || blockstate7.getLightBlock() == 0;
            float f4;
            int i1;
            if (!flag2 && !flag) {
                f4 = f;
                i1 = i;
            } else {
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[0])
                    .move(modelblockrenderer$adjacencyinfo.corners[2]);
                BlockState blockstate8 = pLevel.getBlockState(blockpos$mutableblockpos);
                f4 = this.cache.getShadeBrightness(blockstate8, pLevel, blockpos$mutableblockpos);
                i1 = this.cache.getLightColor(blockstate8, pLevel, blockpos$mutableblockpos);
            }

            float f5;
            int j1;
            if (!flag3 && !flag) {
                f5 = f;
                j1 = i;
            } else {
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[0])
                    .move(modelblockrenderer$adjacencyinfo.corners[3]);
                BlockState blockstate10 = pLevel.getBlockState(blockpos$mutableblockpos);
                f5 = this.cache.getShadeBrightness(blockstate10, pLevel, blockpos$mutableblockpos);
                j1 = this.cache.getLightColor(blockstate10, pLevel, blockpos$mutableblockpos);
            }

            float f6;
            int k1;
            if (!flag2 && !flag1) {
                f6 = f;
                k1 = i;
            } else {
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[1])
                    .move(modelblockrenderer$adjacencyinfo.corners[2]);
                BlockState blockstate11 = pLevel.getBlockState(blockpos$mutableblockpos);
                f6 = this.cache.getShadeBrightness(blockstate11, pLevel, blockpos$mutableblockpos);
                k1 = this.cache.getLightColor(blockstate11, pLevel, blockpos$mutableblockpos);
            }

            float f7;
            int l1;
            if (!flag3 && !flag1) {
                f7 = f;
                l1 = i;
            } else {
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[1])
                    .move(modelblockrenderer$adjacencyinfo.corners[3]);
                BlockState blockstate12 = pLevel.getBlockState(blockpos$mutableblockpos);
                f7 = this.cache.getShadeBrightness(blockstate12, pLevel, blockpos$mutableblockpos);
                l1 = this.cache.getLightColor(blockstate12, pLevel, blockpos$mutableblockpos);
            }

            int i3 = this.cache.getLightColor(pState, pLevel, pPos);
            blockpos$mutableblockpos.setWithOffset(pPos, pDirection);
            BlockState blockstate9 = pLevel.getBlockState(blockpos$mutableblockpos);
            if (this.faceCubic || !blockstate9.isSolidRender()) {
                i3 = this.cache.getLightColor(blockstate9, pLevel, blockpos$mutableblockpos);
            }

            float f8 = this.faceCubic
                ? this.cache.getShadeBrightness(pLevel.getBlockState(blockpos), pLevel, blockpos)
                : this.cache.getShadeBrightness(pLevel.getBlockState(pPos), pLevel, pPos);
            ModelBlockRenderer.AmbientVertexRemap modelblockrenderer$ambientvertexremap = ModelBlockRenderer.AmbientVertexRemap.fromFacing(pDirection);
            if (this.facePartial && modelblockrenderer$adjacencyinfo.doNonCubicWeight) {
                float f29 = (f3 + f + f5 + f8) * 0.25F;
                float f31 = (f2 + f + f4 + f8) * 0.25F;
                float f32 = (f2 + f1 + f6 + f8) * 0.25F;
                float f33 = (f3 + f1 + f7 + f8) * 0.25F;
                float f13 = this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[0].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[1].index];
                float f14 = this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[2].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[3].index];
                float f15 = this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[4].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[5].index];
                float f16 = this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[6].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[7].index];
                float f17 = this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[0].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[1].index];
                float f18 = this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[2].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[3].index];
                float f19 = this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[4].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[5].index];
                float f20 = this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[6].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[7].index];
                float f21 = this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[0].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[1].index];
                float f22 = this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[2].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[3].index];
                float f23 = this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[4].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[5].index];
                float f24 = this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[6].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[7].index];
                float f25 = this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[0].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[1].index];
                float f26 = this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[2].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[3].index];
                float f27 = this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[4].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[5].index];
                float f28 = this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[6].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[7].index];
                this.brightness[modelblockrenderer$ambientvertexremap.vert0] = Math.clamp(f29 * f13 + f31 * f14 + f32 * f15 + f33 * f16, 0.0F, 1.0F);
                this.brightness[modelblockrenderer$ambientvertexremap.vert1] = Math.clamp(f29 * f17 + f31 * f18 + f32 * f19 + f33 * f20, 0.0F, 1.0F);
                this.brightness[modelblockrenderer$ambientvertexremap.vert2] = Math.clamp(f29 * f21 + f31 * f22 + f32 * f23 + f33 * f24, 0.0F, 1.0F);
                this.brightness[modelblockrenderer$ambientvertexremap.vert3] = Math.clamp(f29 * f25 + f31 * f26 + f32 * f27 + f33 * f28, 0.0F, 1.0F);
                int i2 = blend(l, i, j1, i3);
                int j2 = blend(k, i, i1, i3);
                int k2 = blend(k, j, k1, i3);
                int l2 = blend(l, j, l1, i3);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert0] = blend(i2, j2, k2, l2, f13, f14, f15, f16);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert1] = blend(i2, j2, k2, l2, f17, f18, f19, f20);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert2] = blend(i2, j2, k2, l2, f21, f22, f23, f24);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert3] = blend(i2, j2, k2, l2, f25, f26, f27, f28);
            } else {
                float f9 = (f3 + f + f5 + f8) * 0.25F;
                float f10 = (f2 + f + f4 + f8) * 0.25F;
                float f11 = (f2 + f1 + f6 + f8) * 0.25F;
                float f12 = (f3 + f1 + f7 + f8) * 0.25F;
                this.lightmap[modelblockrenderer$ambientvertexremap.vert0] = blend(l, i, j1, i3);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert1] = blend(k, i, i1, i3);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert2] = blend(k, j, k1, i3);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert3] = blend(l, j, l1, i3);
                this.brightness[modelblockrenderer$ambientvertexremap.vert0] = f9;
                this.brightness[modelblockrenderer$ambientvertexremap.vert1] = f10;
                this.brightness[modelblockrenderer$ambientvertexremap.vert2] = f11;
                this.brightness[modelblockrenderer$ambientvertexremap.vert3] = f12;
            }

            float f30 = pLevel.getShade(pDirection, pShade);

            for (int j3 = 0; j3 < this.brightness.length; j3++) {
                this.brightness[j3] = this.brightness[j3] * f30;
            }
        }

        private static int blend(int pColor1, int pColor2, int pColor3, int pCurrentBlockColor) {
            if (pColor1 == 0) {
                pColor1 = pCurrentBlockColor;
            }

            if (pColor2 == 0) {
                pColor2 = pCurrentBlockColor;
            }

            if (pColor3 == 0) {
                pColor3 = pCurrentBlockColor;
            }

            return pColor1 + pColor2 + pColor3 + pCurrentBlockColor >> 2 & 16711935;
        }

        private static int blend(
            int pColor1, int pColor2, int pColor3, int pBlockLight, float pColor1Weight, float pColor2Weight, float pColor3Weight, float pBlockLightWeight
        ) {
            int i = (int)(
                    (pColor1 >> 16 & 0xFF) * pColor1Weight
                        + (pColor2 >> 16 & 0xFF) * pColor2Weight
                        + (pColor3 >> 16 & 0xFF) * pColor3Weight
                        + (pBlockLight >> 16 & 0xFF) * pBlockLightWeight
                )
                & 0xFF;
            int j = (int)((pColor1 & 0xFF) * pColor1Weight + (pColor2 & 0xFF) * pColor2Weight + (pColor3 & 0xFF) * pColor3Weight + (pBlockLight & 0xFF) * pBlockLightWeight)
                & 0xFF;
            return i << 16 | j;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static enum AmbientVertexRemap {
        DOWN(0, 1, 2, 3),
        UP(2, 3, 0, 1),
        NORTH(3, 0, 1, 2),
        SOUTH(0, 1, 2, 3),
        WEST(3, 0, 1, 2),
        EAST(1, 2, 3, 0);

        final int vert0;
        final int vert1;
        final int vert2;
        final int vert3;
        private static final ModelBlockRenderer.AmbientVertexRemap[] BY_FACING = Util.make(new ModelBlockRenderer.AmbientVertexRemap[6], p_111204_ -> {
            p_111204_[Direction.DOWN.get3DDataValue()] = DOWN;
            p_111204_[Direction.UP.get3DDataValue()] = UP;
            p_111204_[Direction.NORTH.get3DDataValue()] = NORTH;
            p_111204_[Direction.SOUTH.get3DDataValue()] = SOUTH;
            p_111204_[Direction.WEST.get3DDataValue()] = WEST;
            p_111204_[Direction.EAST.get3DDataValue()] = EAST;
        });

        private AmbientVertexRemap(final int pVert0, final int pVert1, final int pVert2, final int pVert3) {
            this.vert0 = pVert0;
            this.vert1 = pVert1;
            this.vert2 = pVert2;
            this.vert3 = pVert3;
        }

        public static ModelBlockRenderer.AmbientVertexRemap fromFacing(Direction pFacing) {
            return BY_FACING[pFacing.get3DDataValue()];
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Cache {
        private boolean enabled;
        private final Long2IntLinkedOpenHashMap colorCache = Util.make(() -> {
            Long2IntLinkedOpenHashMap long2intlinkedopenhashmap = new Long2IntLinkedOpenHashMap(100, 0.25F) {
                @Override
                protected void rehash(int p_111238_) {
                }
            };
            long2intlinkedopenhashmap.defaultReturnValue(Integer.MAX_VALUE);
            return long2intlinkedopenhashmap;
        });
        private final Long2FloatLinkedOpenHashMap brightnessCache = Util.make(() -> {
            Long2FloatLinkedOpenHashMap long2floatlinkedopenhashmap = new Long2FloatLinkedOpenHashMap(100, 0.25F) {
                @Override
                protected void rehash(int p_111245_) {
                }
            };
            long2floatlinkedopenhashmap.defaultReturnValue(Float.NaN);
            return long2floatlinkedopenhashmap;
        });
        private final LevelRenderer.BrightnessGetter cachedBrightnessGetter = (p_398128_, p_398129_) -> {
            long i = p_398129_.asLong();
            int j = this.colorCache.get(i);
            if (j != Integer.MAX_VALUE) {
                return j;
            } else {
                int k = LevelRenderer.BrightnessGetter.DEFAULT.packedBrightness(p_398128_, p_398129_);
                if (this.colorCache.size() == 100) {
                    this.colorCache.removeFirstInt();
                }

                this.colorCache.put(i, k);
                return k;
            }
        };

        private Cache() {
        }

        public void enable() {
            this.enabled = true;
        }

        public void disable() {
            this.enabled = false;
            this.colorCache.clear();
            this.brightnessCache.clear();
        }

        public int getLightColor(BlockState pState, BlockAndTintGetter pLevel, BlockPos pPos) {
            return LevelRenderer.getLightColor(this.enabled ? this.cachedBrightnessGetter : LevelRenderer.BrightnessGetter.DEFAULT, pLevel, pState, pPos);
        }

        public float getShadeBrightness(BlockState pState, BlockAndTintGetter pLevel, BlockPos pPos) {
            long i = pPos.asLong();
            if (this.enabled) {
                float f = this.brightnessCache.get(i);
                if (!Float.isNaN(f)) {
                    return f;
                }
            }

            float f1 = pState.getShadeBrightness(pLevel, pPos);
            if (this.enabled) {
                if (this.brightnessCache.size() == 100) {
                    this.brightnessCache.removeFirstFloat();
                }

                this.brightnessCache.put(i, f1);
            }

            return f1;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class CommonRenderStorage {
        public final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
        public boolean faceCubic;
        public boolean facePartial;
        public final float[] brightness = new float[4];
        public final int[] lightmap = new int[4];
        public int tintCacheIndex = -1;
        public int tintCacheValue;
        public final ModelBlockRenderer.Cache cache = ModelBlockRenderer.CACHE.get();
    }

    @OnlyIn(Dist.CLIENT)
    protected static enum SizeInfo {
        DOWN(0),
        UP(1),
        NORTH(2),
        SOUTH(3),
        WEST(4),
        EAST(5),
        FLIP_DOWN(6),
        FLIP_UP(7),
        FLIP_NORTH(8),
        FLIP_SOUTH(9),
        FLIP_WEST(10),
        FLIP_EAST(11);

        public static final int COUNT = values().length;
        final int index;

        private SizeInfo(final int pIndex) {
            this.index = pIndex;
        }
    }
}
