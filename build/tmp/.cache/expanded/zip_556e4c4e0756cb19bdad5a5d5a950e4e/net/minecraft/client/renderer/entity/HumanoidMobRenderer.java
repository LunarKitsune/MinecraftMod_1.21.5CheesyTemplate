package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class HumanoidMobRenderer<T extends Mob, S extends HumanoidRenderState, M extends HumanoidModel<S>> extends AgeableMobRenderer<T, S, M> {
    public HumanoidMobRenderer(EntityRendererProvider.Context pContext, M pModel, float pShadowRadius) {
        this(pContext, pModel, pModel, pShadowRadius);
    }

    public HumanoidMobRenderer(EntityRendererProvider.Context pContext, M pAdultModel, M pBabyModel, float pShadowRadius) {
        this(pContext, pAdultModel, pBabyModel, pShadowRadius, CustomHeadLayer.Transforms.DEFAULT);
    }

    public HumanoidMobRenderer(EntityRendererProvider.Context pContext, M pAdultModel, M pBabyModel, float pShadowRadius, CustomHeadLayer.Transforms pTransforms) {
        super(pContext, pAdultModel, pBabyModel, pShadowRadius);
        this.addLayer(new CustomHeadLayer<>(this, pContext.getModelSet(), pTransforms));
        this.addLayer(new WingsLayer<>(this, pContext.getModelSet(), pContext.getEquipmentRenderer()));
        this.addLayer(new ItemInHandLayer<>(this));
    }

    protected HumanoidModel.ArmPose getArmPose(T pMob, HumanoidArm pArm) {
        return HumanoidModel.ArmPose.EMPTY;
    }

    public void extractRenderState(T p_368012_, S p_365777_, float p_367477_) {
        super.extractRenderState(p_368012_, p_365777_, p_367477_);
        extractHumanoidRenderState(p_368012_, p_365777_, p_367477_, this.itemModelResolver);
        p_365777_.leftArmPose = this.getArmPose(p_368012_, HumanoidArm.LEFT);
        p_365777_.rightArmPose = this.getArmPose(p_368012_, HumanoidArm.RIGHT);
    }

    public static void extractHumanoidRenderState(LivingEntity pEntity, HumanoidRenderState pReusedState, float pPartialTick, ItemModelResolver pItemModelResolver) {
        ArmedEntityRenderState.extractArmedEntityRenderState(pEntity, pReusedState, pItemModelResolver);
        pReusedState.isCrouching = pEntity.isCrouching();
        pReusedState.isFallFlying = pEntity.isFallFlying();
        pReusedState.isVisuallySwimming = pEntity.isVisuallySwimming();
        pReusedState.isPassenger = pEntity.isPassenger() && (pEntity.getVehicle() != null && pEntity.getVehicle().shouldRiderSit());;
        pReusedState.speedValue = 1.0F;
        if (pReusedState.isFallFlying) {
            pReusedState.speedValue = (float)pEntity.getDeltaMovement().lengthSqr();
            pReusedState.speedValue /= 0.2F;
            pReusedState.speedValue = pReusedState.speedValue * (pReusedState.speedValue * pReusedState.speedValue);
        }

        if (pReusedState.speedValue < 1.0F) {
            pReusedState.speedValue = 1.0F;
        }

        pReusedState.attackTime = pEntity.getAttackAnim(pPartialTick);
        pReusedState.swimAmount = pEntity.getSwimAmount(pPartialTick);
        pReusedState.attackArm = getAttackArm(pEntity);
        pReusedState.useItemHand = pEntity.getUsedItemHand();
        pReusedState.maxCrossbowChargeDuration = CrossbowItem.getChargeDuration(pEntity.getUseItem(), pEntity);
        pReusedState.ticksUsingItem = pEntity.getTicksUsingItem();
        pReusedState.isUsingItem = pEntity.isUsingItem();
        pReusedState.elytraRotX = pEntity.elytraAnimationState.getRotX(pPartialTick);
        pReusedState.elytraRotY = pEntity.elytraAnimationState.getRotY(pPartialTick);
        pReusedState.elytraRotZ = pEntity.elytraAnimationState.getRotZ(pPartialTick);
        pReusedState.headEquipment = getEquipmentIfRenderable(pEntity, EquipmentSlot.HEAD);
        pReusedState.chestEquipment = getEquipmentIfRenderable(pEntity, EquipmentSlot.CHEST);
        pReusedState.legsEquipment = getEquipmentIfRenderable(pEntity, EquipmentSlot.LEGS);
        pReusedState.feetEquipment = getEquipmentIfRenderable(pEntity, EquipmentSlot.FEET);
    }

    private static ItemStack getEquipmentIfRenderable(LivingEntity pEntity, EquipmentSlot pSlot) {
        ItemStack itemstack = pEntity.getItemBySlot(pSlot);
        return HumanoidArmorLayer.shouldRender(itemstack, pSlot) ? itemstack.copy() : ItemStack.EMPTY;
    }

    private static HumanoidArm getAttackArm(LivingEntity pEntity) {
        HumanoidArm humanoidarm = pEntity.getMainArm();
        return pEntity.swingingArm == InteractionHand.MAIN_HAND ? humanoidarm : humanoidarm.getOpposite();
    }
}
