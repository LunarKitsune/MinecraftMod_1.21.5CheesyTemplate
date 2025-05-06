package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MapDecorationTextureManager;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class MapRenderer {
    private static final float MAP_Z_OFFSET = -0.01F;
    private static final float DECORATION_Z_OFFSET = -0.001F;
    private static final int WIDTH = 128;
    private static final int HEIGHT = 128;
    private final MapTextureManager mapTextureManager;
    private final MapDecorationTextureManager decorationTextures;

    public MapRenderer(MapDecorationTextureManager pDecorationTextures, MapTextureManager pMapTextureManager) {
        this.decorationTextures = pDecorationTextures;
        this.mapTextureManager = pMapTextureManager;
    }

    public void render(MapRenderState pRenderState, PoseStack pPoseStack, MultiBufferSource pBufferSource, boolean pActive, int pPackedLight) {
        Matrix4f matrix4f = pPoseStack.last().pose();
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.text(pRenderState.texture));
        vertexconsumer.addVertex(matrix4f, 0.0F, 128.0F, -0.01F).setColor(-1).setUv(0.0F, 1.0F).setLight(pPackedLight);
        vertexconsumer.addVertex(matrix4f, 128.0F, 128.0F, -0.01F).setColor(-1).setUv(1.0F, 1.0F).setLight(pPackedLight);
        vertexconsumer.addVertex(matrix4f, 128.0F, 0.0F, -0.01F).setColor(-1).setUv(1.0F, 0.0F).setLight(pPackedLight);
        vertexconsumer.addVertex(matrix4f, 0.0F, 0.0F, -0.01F).setColor(-1).setUv(0.0F, 0.0F).setLight(pPackedLight);
        int i = 0;

        for (MapRenderState.MapDecorationRenderState maprenderstate$mapdecorationrenderstate : pRenderState.decorations) {
            if (!pActive || maprenderstate$mapdecorationrenderstate.renderOnFrame) {
                pPoseStack.pushPose();
                pPoseStack.translate(
                    maprenderstate$mapdecorationrenderstate.x / 2.0F + 64.0F, maprenderstate$mapdecorationrenderstate.y / 2.0F + 64.0F, -0.02F
                );
                pPoseStack.mulPose(Axis.ZP.rotationDegrees(maprenderstate$mapdecorationrenderstate.rot * 360 / 16.0F));
                pPoseStack.scale(4.0F, 4.0F, 3.0F);
                pPoseStack.translate(-0.125F, 0.125F, 0.0F);
                Matrix4f matrix4f1 = pPoseStack.last().pose();
                TextureAtlasSprite textureatlassprite = maprenderstate$mapdecorationrenderstate.atlasSprite;
                if (textureatlassprite != null) {
                    VertexConsumer vertexconsumer1 = pBufferSource.getBuffer(RenderType.text(textureatlassprite.atlasLocation()));
                    vertexconsumer1.addVertex(matrix4f1, -1.0F, 1.0F, i * -0.001F)
                        .setColor(-1)
                        .setUv(textureatlassprite.getU0(), textureatlassprite.getV0())
                        .setLight(pPackedLight);
                    vertexconsumer1.addVertex(matrix4f1, 1.0F, 1.0F, i * -0.001F)
                        .setColor(-1)
                        .setUv(textureatlassprite.getU1(), textureatlassprite.getV0())
                        .setLight(pPackedLight);
                    vertexconsumer1.addVertex(matrix4f1, 1.0F, -1.0F, i * -0.001F)
                        .setColor(-1)
                        .setUv(textureatlassprite.getU1(), textureatlassprite.getV1())
                        .setLight(pPackedLight);
                    vertexconsumer1.addVertex(matrix4f1, -1.0F, -1.0F, i * -0.001F)
                        .setColor(-1)
                        .setUv(textureatlassprite.getU0(), textureatlassprite.getV1())
                        .setLight(pPackedLight);
                    pPoseStack.popPose();
                }

                if (maprenderstate$mapdecorationrenderstate.name != null) {
                    Font font = Minecraft.getInstance().font;
                    float f = font.width(maprenderstate$mapdecorationrenderstate.name);
                    float f1 = Mth.clamp(25.0F / f, 0.0F, 6.0F / 9.0F);
                    pPoseStack.pushPose();
                    pPoseStack.translate(
                        maprenderstate$mapdecorationrenderstate.x / 2.0F + 64.0F - f * f1 / 2.0F,
                        maprenderstate$mapdecorationrenderstate.y / 2.0F + 64.0F + 4.0F,
                        -0.025F
                    );
                    pPoseStack.scale(f1, f1, 1.0F);
                    pPoseStack.translate(0.0F, 0.0F, -0.1F);
                    font.drawInBatch(
                        maprenderstate$mapdecorationrenderstate.name,
                        0.0F,
                        0.0F,
                        -1,
                        false,
                        pPoseStack.last().pose(),
                        pBufferSource,
                        Font.DisplayMode.NORMAL,
                        Integer.MIN_VALUE,
                        pPackedLight,
                        false
                    );
                    pPoseStack.popPose();
                }

                i++;
            }
        }
    }

    public void extractRenderState(MapId pId, MapItemSavedData pSavedData, MapRenderState pRenderState) {
        pRenderState.texture = this.mapTextureManager.prepareMapTexture(pId, pSavedData);
        pRenderState.decorations.clear();

        for (MapDecoration mapdecoration : pSavedData.getDecorations()) {
            pRenderState.decorations.add(this.extractDecorationRenderState(mapdecoration));
        }
    }

    private MapRenderState.MapDecorationRenderState extractDecorationRenderState(MapDecoration pDecoration) {
        MapRenderState.MapDecorationRenderState maprenderstate$mapdecorationrenderstate = new MapRenderState.MapDecorationRenderState();
        maprenderstate$mapdecorationrenderstate.atlasSprite = this.decorationTextures.get(pDecoration);
        maprenderstate$mapdecorationrenderstate.x = pDecoration.x();
        maprenderstate$mapdecorationrenderstate.y = pDecoration.y();
        maprenderstate$mapdecorationrenderstate.rot = pDecoration.rot();
        maprenderstate$mapdecorationrenderstate.name = pDecoration.name().orElse(null);
        maprenderstate$mapdecorationrenderstate.renderOnFrame = pDecoration.renderOnFrame();
        return maprenderstate$mapdecorationrenderstate;
    }
}