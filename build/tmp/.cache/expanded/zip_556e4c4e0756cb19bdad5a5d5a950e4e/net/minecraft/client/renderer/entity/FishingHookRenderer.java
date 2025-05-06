package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FishingHookRenderer extends EntityRenderer<FishingHook, FishingHookRenderState> {
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/fishing_hook.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutout(TEXTURE_LOCATION);
    private static final double VIEW_BOBBING_SCALE = 960.0;

    public FishingHookRenderer(EntityRendererProvider.Context p_174117_) {
        super(p_174117_);
    }

    public boolean shouldRender(FishingHook p_364485_, Frustum p_366882_, double p_369405_, double p_366566_, double p_370201_) {
        return super.shouldRender(p_364485_, p_366882_, p_369405_, p_366566_, p_370201_) && p_364485_.getPlayerOwner() != null;
    }

    public void render(FishingHookRenderState p_362917_, PoseStack p_114699_, MultiBufferSource p_114700_, int p_114701_) {
        p_114699_.pushPose();
        p_114699_.pushPose();
        p_114699_.scale(0.5F, 0.5F, 0.5F);
        p_114699_.mulPose(this.entityRenderDispatcher.cameraOrientation());
        PoseStack.Pose posestack$pose = p_114699_.last();
        VertexConsumer vertexconsumer = p_114700_.getBuffer(RENDER_TYPE);
        vertex(vertexconsumer, posestack$pose, p_114701_, 0.0F, 0, 0, 1);
        vertex(vertexconsumer, posestack$pose, p_114701_, 1.0F, 0, 1, 1);
        vertex(vertexconsumer, posestack$pose, p_114701_, 1.0F, 1, 1, 0);
        vertex(vertexconsumer, posestack$pose, p_114701_, 0.0F, 1, 0, 0);
        p_114699_.popPose();
        float f = (float)p_362917_.lineOriginOffset.x;
        float f1 = (float)p_362917_.lineOriginOffset.y;
        float f2 = (float)p_362917_.lineOriginOffset.z;
        VertexConsumer vertexconsumer1 = p_114700_.getBuffer(RenderType.lineStrip());
        PoseStack.Pose posestack$pose1 = p_114699_.last();
        int i = 16;

        for (int j = 0; j <= 16; j++) {
            stringVertex(f, f1, f2, vertexconsumer1, posestack$pose1, fraction(j, 16), fraction(j + 1, 16));
        }

        p_114699_.popPose();
        super.render(p_362917_, p_114699_, p_114700_, p_114701_);
    }

    public static HumanoidArm getHoldingArm(Player pPlayer) {
        return pPlayer.getMainHandItem().getItem() instanceof FishingRodItem ? pPlayer.getMainArm() : pPlayer.getMainArm().getOpposite();
    }

    private Vec3 getPlayerHandPos(Player pPlayer, float pHandAngle, float pPartialTick) {
        int i = getHoldingArm(pPlayer) == HumanoidArm.RIGHT ? 1 : -1;
        if (this.entityRenderDispatcher.options.getCameraType().isFirstPerson() && pPlayer == Minecraft.getInstance().player) {
            double d4 = 960.0 / this.entityRenderDispatcher.options.fov().get().intValue();
            Vec3 vec3 = this.entityRenderDispatcher.camera.getNearPlane().getPointOnPlane(i * 0.525F, -0.1F).scale(d4).yRot(pHandAngle * 0.5F).xRot(-pHandAngle * 0.7F);
            return pPlayer.getEyePosition(pPartialTick).add(vec3);
        } else {
            float f = Mth.lerp(pPartialTick, pPlayer.yBodyRotO, pPlayer.yBodyRot) * (float) (Math.PI / 180.0);
            double d0 = Mth.sin(f);
            double d1 = Mth.cos(f);
            float f1 = pPlayer.getScale();
            double d2 = i * 0.35 * f1;
            double d3 = 0.8 * f1;
            float f2 = pPlayer.isCrouching() ? -0.1875F : 0.0F;
            return pPlayer.getEyePosition(pPartialTick).add(-d1 * d2 - d0 * d3, f2 - 0.45 * f1, -d0 * d2 + d1 * d3);
        }
    }

    private static float fraction(int pNumerator, int pDenominator) {
        return (float)pNumerator / pDenominator;
    }

    private static void vertex(
        VertexConsumer pConsumer, PoseStack.Pose pPose, int pPackedLight, float pX, int pY, int pU, int pV
    ) {
        pConsumer.addVertex(pPose, pX - 0.5F, pY - 0.5F, 0.0F)
            .setColor(-1)
            .setUv(pU, pV)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(pPackedLight)
            .setNormal(pPose, 0.0F, 1.0F, 0.0F);
    }

    private static void stringVertex(
        float pX, float pY, float pZ, VertexConsumer pConsumer, PoseStack.Pose pPose, float pStringFraction, float pNextStringFraction
    ) {
        float f = pX * pStringFraction;
        float f1 = pY * (pStringFraction * pStringFraction + pStringFraction) * 0.5F + 0.25F;
        float f2 = pZ * pStringFraction;
        float f3 = pX * pNextStringFraction - f;
        float f4 = pY * (pNextStringFraction * pNextStringFraction + pNextStringFraction) * 0.5F + 0.25F - f1;
        float f5 = pZ * pNextStringFraction - f2;
        float f6 = Mth.sqrt(f3 * f3 + f4 * f4 + f5 * f5);
        f3 /= f6;
        f4 /= f6;
        f5 /= f6;
        pConsumer.addVertex(pPose, f, f1, f2).setColor(-16777216).setNormal(pPose, f3, f4, f5);
    }

    public FishingHookRenderState createRenderState() {
        return new FishingHookRenderState();
    }

    public void extractRenderState(FishingHook p_363636_, FishingHookRenderState p_369118_, float p_368947_) {
        super.extractRenderState(p_363636_, p_369118_, p_368947_);
        Player player = p_363636_.getPlayerOwner();
        if (player == null) {
            p_369118_.lineOriginOffset = Vec3.ZERO;
        } else {
            float f = player.getAttackAnim(p_368947_);
            float f1 = Mth.sin(Mth.sqrt(f) * (float) Math.PI);
            Vec3 vec3 = this.getPlayerHandPos(player, f1, p_368947_);
            Vec3 vec31 = p_363636_.getPosition(p_368947_).add(0.0, 0.25, 0.0);
            p_369118_.lineOriginOffset = vec3.subtract(vec31);
        }
    }

    protected boolean affectedByCulling(FishingHook p_361671_) {
        return false;
    }
}