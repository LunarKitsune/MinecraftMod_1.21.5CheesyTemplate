package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class ScreenEffectRenderer {
    private static final ResourceLocation UNDERWATER_LOCATION = ResourceLocation.withDefaultNamespace("textures/misc/underwater.png");

    public static void renderScreenEffect(Minecraft pMinecraft, PoseStack pPoseStack, MultiBufferSource pBufferSource) {
        Player player = pMinecraft.player;
        if (!player.noPhysics) {
            BlockState blockstate = getViewBlockingState(player);
            var overlay = getOverlayBlock(player);
            if (overlay != null) {
                if (!net.minecraftforge.client.ForgeHooksClient.renderBlockOverlay(player, pPoseStack, net.minecraftforge.client.event.RenderBlockScreenEffectEvent.OverlayType.BLOCK, overlay.getLeft(), overlay.getRight())) {
                    renderTex(pMinecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(overlay.getLeft(), pMinecraft.level, overlay.getRight()), pPoseStack, pBufferSource);
                }
            }
        }

        if (!pMinecraft.player.isSpectator()) {
            if (pMinecraft.player.isEyeInFluid(FluidTags.WATER)) {
                if (!net.minecraftforge.client.ForgeHooksClient.renderWaterOverlay(player, pPoseStack))
                renderWater(pMinecraft, pPoseStack, pBufferSource);
            } else if (!player.getEyeInFluidType().isAir()) {
                net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(player.getEyeInFluidType()).renderOverlay(pMinecraft, pPoseStack, pBufferSource);
            }

            if (pMinecraft.player.isOnFire()) {
                if (!net.minecraftforge.client.ForgeHooksClient.renderFireOverlay(player, pPoseStack))
                renderFire(pPoseStack, pBufferSource);
            }
        }
    }

    @Nullable
    private static BlockState getViewBlockingState(Player pPlayer) {
        var ret = getOverlayBlock(pPlayer);
        return ret == null ? null : ret.getLeft();
    }

    @Nullable
    private static org.apache.commons.lang3.tuple.Pair<BlockState, BlockPos> getOverlayBlock(Player pPlayer) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < 8; i++) {
            double d0 = pPlayer.getX() + ((i >> 0) % 2 - 0.5F) * pPlayer.getBbWidth() * 0.8F;
            double d1 = pPlayer.getEyeY() + ((i >> 1) % 2 - 0.5F) * 0.1F * pPlayer.getScale();
            double d2 = pPlayer.getZ() + ((i >> 2) % 2 - 0.5F) * pPlayer.getBbWidth() * 0.8F;
            blockpos$mutableblockpos.set(d0, d1, d2);
            BlockState blockstate = pPlayer.level().getBlockState(blockpos$mutableblockpos);
            if (blockstate.getRenderShape() != RenderShape.INVISIBLE && blockstate.isViewBlocking(pPlayer.level(), blockpos$mutableblockpos)) {
                return org.apache.commons.lang3.tuple.Pair.of(blockstate, blockpos$mutableblockpos.immutable());
            }
        }

        return null;
    }

    private static void renderTex(TextureAtlasSprite pTexture, PoseStack pPoseStack, MultiBufferSource pBufferSource) {
        float f = 0.1F;
        int i = ARGB.colorFromFloat(1.0F, 0.1F, 0.1F, 0.1F);
        float f1 = -1.0F;
        float f2 = 1.0F;
        float f3 = -1.0F;
        float f4 = 1.0F;
        float f5 = -0.5F;
        float f6 = pTexture.getU0();
        float f7 = pTexture.getU1();
        float f8 = pTexture.getV0();
        float f9 = pTexture.getV1();
        Matrix4f matrix4f = pPoseStack.last().pose();
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.blockScreenEffect(pTexture.atlasLocation()));
        vertexconsumer.addVertex(matrix4f, -1.0F, -1.0F, -0.5F).setUv(f7, f9).setColor(i);
        vertexconsumer.addVertex(matrix4f, 1.0F, -1.0F, -0.5F).setUv(f6, f9).setColor(i);
        vertexconsumer.addVertex(matrix4f, 1.0F, 1.0F, -0.5F).setUv(f6, f8).setColor(i);
        vertexconsumer.addVertex(matrix4f, -1.0F, 1.0F, -0.5F).setUv(f7, f8).setColor(i);
    }

    private static void renderWater(Minecraft pMnecraft, PoseStack pPoseStack, MultiBufferSource pBufferSource) {
        renderFluid(pMnecraft, pPoseStack, pBufferSource, UNDERWATER_LOCATION);
    }

    public static void renderFluid(Minecraft pMnecraft, PoseStack pPoseStack, MultiBufferSource pBufferSource, ResourceLocation texture) {
        BlockPos blockpos = BlockPos.containing(pMnecraft.player.getX(), pMnecraft.player.getEyeY(), pMnecraft.player.getZ());
        float f = LightTexture.getBrightness(pMnecraft.player.level().dimensionType(), pMnecraft.player.level().getMaxLocalRawBrightness(blockpos));
        int i = ARGB.colorFromFloat(0.1F, f, f, f);
        float f1 = 4.0F;
        float f2 = -1.0F;
        float f3 = 1.0F;
        float f4 = -1.0F;
        float f5 = 1.0F;
        float f6 = -0.5F;
        float f7 = -pMnecraft.player.getYRot() / 64.0F;
        float f8 = pMnecraft.player.getXRot() / 64.0F;
        Matrix4f matrix4f = pPoseStack.last().pose();
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.blockScreenEffect(texture));
        vertexconsumer.addVertex(matrix4f, -1.0F, -1.0F, -0.5F).setUv(4.0F + f7, 4.0F + f8).setColor(i);
        vertexconsumer.addVertex(matrix4f, 1.0F, -1.0F, -0.5F).setUv(0.0F + f7, 4.0F + f8).setColor(i);
        vertexconsumer.addVertex(matrix4f, 1.0F, 1.0F, -0.5F).setUv(0.0F + f7, 0.0F + f8).setColor(i);
        vertexconsumer.addVertex(matrix4f, -1.0F, 1.0F, -0.5F).setUv(4.0F + f7, 0.0F + f8).setColor(i);
    }

    private static void renderFire(PoseStack pPoseStack, MultiBufferSource pBufferSource) {
        TextureAtlasSprite textureatlassprite = ModelBakery.FIRE_1.sprite();
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.fireScreenEffect(textureatlassprite.atlasLocation()));
        float f = textureatlassprite.getU0();
        float f1 = textureatlassprite.getU1();
        float f2 = (f + f1) / 2.0F;
        float f3 = textureatlassprite.getV0();
        float f4 = textureatlassprite.getV1();
        float f5 = (f3 + f4) / 2.0F;
        float f6 = textureatlassprite.uvShrinkRatio();
        float f7 = Mth.lerp(f6, f, f2);
        float f8 = Mth.lerp(f6, f1, f2);
        float f9 = Mth.lerp(f6, f3, f5);
        float f10 = Mth.lerp(f6, f4, f5);
        float f11 = 1.0F;

        for (int i = 0; i < 2; i++) {
            pPoseStack.pushPose();
            float f12 = -0.5F;
            float f13 = 0.5F;
            float f14 = -0.5F;
            float f15 = 0.5F;
            float f16 = -0.5F;
            pPoseStack.translate(-(i * 2 - 1) * 0.24F, -0.3F, 0.0F);
            pPoseStack.mulPose(Axis.YP.rotationDegrees((i * 2 - 1) * 10.0F));
            Matrix4f matrix4f = pPoseStack.last().pose();
            vertexconsumer.addVertex(matrix4f, -0.5F, -0.5F, -0.5F).setUv(f8, f10).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            vertexconsumer.addVertex(matrix4f, 0.5F, -0.5F, -0.5F).setUv(f7, f10).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            vertexconsumer.addVertex(matrix4f, 0.5F, 0.5F, -0.5F).setUv(f7, f9).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            vertexconsumer.addVertex(matrix4f, -0.5F, 0.5F, -0.5F).setUv(f8, f9).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            pPoseStack.popPose();
        }
    }
}
