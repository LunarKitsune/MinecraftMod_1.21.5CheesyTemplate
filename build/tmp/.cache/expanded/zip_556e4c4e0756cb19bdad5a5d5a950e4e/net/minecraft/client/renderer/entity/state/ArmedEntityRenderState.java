package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArmedEntityRenderState extends LivingEntityRenderState {
    public HumanoidArm mainArm = HumanoidArm.RIGHT;
    public HumanoidModel.ArmPose rightArmPose = HumanoidModel.ArmPose.EMPTY;
    public final ItemStackRenderState rightHandItem = new ItemStackRenderState();
    public HumanoidModel.ArmPose leftArmPose = HumanoidModel.ArmPose.EMPTY;
    public final ItemStackRenderState leftHandItem = new ItemStackRenderState();

    public ItemStackRenderState getMainHandItem() {
        return this.mainArm == HumanoidArm.RIGHT ? this.rightHandItem : this.leftHandItem;
    }

    public static void extractArmedEntityRenderState(LivingEntity pEntity, ArmedEntityRenderState pReusedState, ItemModelResolver pItemModelResolver) {
        pReusedState.mainArm = pEntity.getMainArm();
        pItemModelResolver.updateForLiving(pReusedState.rightHandItem, pEntity.getItemHeldByArm(HumanoidArm.RIGHT), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, pEntity);
        pItemModelResolver.updateForLiving(pReusedState.leftHandItem, pEntity.getItemHeldByArm(HumanoidArm.LEFT), ItemDisplayContext.THIRD_PERSON_LEFT_HAND, pEntity);
    }
}