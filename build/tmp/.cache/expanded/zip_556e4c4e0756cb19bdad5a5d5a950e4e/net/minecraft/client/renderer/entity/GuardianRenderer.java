package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.GuardianModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.GuardianRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuardianRenderer extends MobRenderer<Guardian, GuardianRenderState, GuardianModel> {
    private static final ResourceLocation GUARDIAN_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/guardian.png");
    private static final ResourceLocation GUARDIAN_BEAM_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/guardian_beam.png");
    private static final RenderType BEAM_RENDER_TYPE = RenderType.entityCutoutNoCull(GUARDIAN_BEAM_LOCATION);

    public GuardianRenderer(EntityRendererProvider.Context p_174159_) {
        this(p_174159_, 0.5F, ModelLayers.GUARDIAN);
    }

    protected GuardianRenderer(EntityRendererProvider.Context pContext, float pShadowRadius, ModelLayerLocation pLayer) {
        super(pContext, new GuardianModel(pContext.bakeLayer(pLayer)), pShadowRadius);
    }

    public boolean shouldRender(Guardian pLivingEntity, Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
        if (super.shouldRender(pLivingEntity, pCamera, pCamX, pCamY, pCamZ)) {
            return true;
        } else {
            if (pLivingEntity.hasActiveAttackTarget()) {
                LivingEntity livingentity = pLivingEntity.getActiveAttackTarget();
                if (livingentity != null) {
                    Vec3 vec3 = this.getPosition(livingentity, livingentity.getBbHeight() * 0.5, 1.0F);
                    Vec3 vec31 = this.getPosition(pLivingEntity, pLivingEntity.getEyeHeight(), 1.0F);
                    return pCamera.isVisible(new AABB(vec31.x, vec31.y, vec31.z, vec3.x, vec3.y, vec3.z));
                }
            }

            return false;
        }
    }

    private Vec3 getPosition(LivingEntity pLivingEntity, double pYOffset, float pPartialTick) {
        double d0 = Mth.lerp(pPartialTick, pLivingEntity.xOld, pLivingEntity.getX());
        double d1 = Mth.lerp(pPartialTick, pLivingEntity.yOld, pLivingEntity.getY()) + pYOffset;
        double d2 = Mth.lerp(pPartialTick, pLivingEntity.zOld, pLivingEntity.getZ());
        return new Vec3(d0, d1, d2);
    }

    public void render(GuardianRenderState p_369518_, PoseStack p_114793_, MultiBufferSource p_114794_, int p_114795_) {
        super.render(p_369518_, p_114793_, p_114794_, p_114795_);
        Vec3 vec3 = p_369518_.attackTargetPosition;
        if (vec3 != null) {
            float f = p_369518_.attackTime * 0.5F % 1.0F;
            p_114793_.pushPose();
            p_114793_.translate(0.0F, p_369518_.eyeHeight, 0.0F);
            renderBeam(p_114793_, p_114794_.getBuffer(BEAM_RENDER_TYPE), vec3.subtract(p_369518_.eyePosition), p_369518_.attackTime, p_369518_.attackScale, f);
            p_114793_.popPose();
        }
    }

    private static void renderBeam(PoseStack pPoseStack, VertexConsumer pBuffer, Vec3 pBeamVector, float pAttackTime, float pScale, float pAnimationTime) {
        float f = (float)(pBeamVector.length() + 1.0);
        pBeamVector = pBeamVector.normalize();
        float f1 = (float)Math.acos(pBeamVector.y);
        float f2 = (float) (Math.PI / 2) - (float)Math.atan2(pBeamVector.z, pBeamVector.x);
        pPoseStack.mulPose(Axis.YP.rotationDegrees(f2 * (180.0F / (float)Math.PI)));
        pPoseStack.mulPose(Axis.XP.rotationDegrees(f1 * (180.0F / (float)Math.PI)));
        float f3 = pAttackTime * 0.05F * -1.5F;
        float f4 = pScale * pScale;
        int i = 64 + (int)(f4 * 191.0F);
        int j = 32 + (int)(f4 * 191.0F);
        int k = 128 - (int)(f4 * 64.0F);
        float f5 = 0.2F;
        float f6 = 0.282F;
        float f7 = Mth.cos(f3 + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
        float f8 = Mth.sin(f3 + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
        float f9 = Mth.cos(f3 + (float) (Math.PI / 4)) * 0.282F;
        float f10 = Mth.sin(f3 + (float) (Math.PI / 4)) * 0.282F;
        float f11 = Mth.cos(f3 + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
        float f12 = Mth.sin(f3 + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
        float f13 = Mth.cos(f3 + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
        float f14 = Mth.sin(f3 + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
        float f15 = Mth.cos(f3 + (float) Math.PI) * 0.2F;
        float f16 = Mth.sin(f3 + (float) Math.PI) * 0.2F;
        float f17 = Mth.cos(f3 + 0.0F) * 0.2F;
        float f18 = Mth.sin(f3 + 0.0F) * 0.2F;
        float f19 = Mth.cos(f3 + (float) (Math.PI / 2)) * 0.2F;
        float f20 = Mth.sin(f3 + (float) (Math.PI / 2)) * 0.2F;
        float f21 = Mth.cos(f3 + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
        float f22 = Mth.sin(f3 + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
        float f23 = 0.0F;
        float f24 = 0.4999F;
        float f25 = -1.0F + pAnimationTime;
        float f26 = f25 + f * 2.5F;
        PoseStack.Pose posestack$pose = pPoseStack.last();
        vertex(pBuffer, posestack$pose, f15, f, f16, i, j, k, 0.4999F, f26);
        vertex(pBuffer, posestack$pose, f15, 0.0F, f16, i, j, k, 0.4999F, f25);
        vertex(pBuffer, posestack$pose, f17, 0.0F, f18, i, j, k, 0.0F, f25);
        vertex(pBuffer, posestack$pose, f17, f, f18, i, j, k, 0.0F, f26);
        vertex(pBuffer, posestack$pose, f19, f, f20, i, j, k, 0.4999F, f26);
        vertex(pBuffer, posestack$pose, f19, 0.0F, f20, i, j, k, 0.4999F, f25);
        vertex(pBuffer, posestack$pose, f21, 0.0F, f22, i, j, k, 0.0F, f25);
        vertex(pBuffer, posestack$pose, f21, f, f22, i, j, k, 0.0F, f26);
        float f27 = Mth.floor(pAttackTime) % 2 == 0 ? 0.5F : 0.0F;
        vertex(pBuffer, posestack$pose, f7, f, f8, i, j, k, 0.5F, f27 + 0.5F);
        vertex(pBuffer, posestack$pose, f9, f, f10, i, j, k, 1.0F, f27 + 0.5F);
        vertex(pBuffer, posestack$pose, f13, f, f14, i, j, k, 1.0F, f27);
        vertex(pBuffer, posestack$pose, f11, f, f12, i, j, k, 0.5F, f27);
    }

    private static void vertex(
        VertexConsumer pConsumer,
        PoseStack.Pose pPose,
        float pX,
        float pY,
        float pZ,
        int pRed,
        int pGreen,
        int pBlue,
        float pU,
        float pV
    ) {
        pConsumer.addVertex(pPose, pX, pY, pZ)
            .setColor(pRed, pGreen, pBlue, 255)
            .setUv(pU, pV)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(pPose, 0.0F, 1.0F, 0.0F);
    }

    public ResourceLocation getTextureLocation(GuardianRenderState p_361264_) {
        return GUARDIAN_LOCATION;
    }

    public GuardianRenderState createRenderState() {
        return new GuardianRenderState();
    }

    public void extractRenderState(Guardian p_365802_, GuardianRenderState p_365304_, float p_367592_) {
        super.extractRenderState(p_365802_, p_365304_, p_367592_);
        p_365304_.spikesAnimation = p_365802_.getSpikesAnimation(p_367592_);
        p_365304_.tailAnimation = p_365802_.getTailAnimation(p_367592_);
        p_365304_.eyePosition = p_365802_.getEyePosition(p_367592_);
        Entity entity = getEntityToLookAt(p_365802_);
        if (entity != null) {
            p_365304_.lookDirection = p_365802_.getViewVector(p_367592_);
            p_365304_.lookAtPosition = entity.getEyePosition(p_367592_);
        } else {
            p_365304_.lookDirection = null;
            p_365304_.lookAtPosition = null;
        }

        LivingEntity livingentity = p_365802_.getActiveAttackTarget();
        if (livingentity != null) {
            p_365304_.attackScale = p_365802_.getAttackAnimationScale(p_367592_);
            p_365304_.attackTime = p_365802_.getClientSideAttackTime() + p_367592_;
            p_365304_.attackTargetPosition = this.getPosition(livingentity, livingentity.getBbHeight() * 0.5, p_367592_);
        } else {
            p_365304_.attackTargetPosition = null;
        }
    }

    @Nullable
    private static Entity getEntityToLookAt(Guardian pGuardian) {
        Entity entity = Minecraft.getInstance().getCameraEntity();
        return (Entity)(pGuardian.hasActiveAttackTarget() ? pGuardian.getActiveAttackTarget() : entity);
    }
}