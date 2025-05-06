package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractSkeletonRenderer<T extends AbstractSkeleton, S extends SkeletonRenderState> extends HumanoidMobRenderer<T, S, SkeletonModel<S>> {
    public AbstractSkeletonRenderer(
        EntityRendererProvider.Context pContext, ModelLayerLocation pModelLayer, ModelLayerLocation pSkeletonLayer, ModelLayerLocation pInnerModelLayer
    ) {
        this(pContext, pSkeletonLayer, pInnerModelLayer, new SkeletonModel<>(pContext.bakeLayer(pModelLayer)));
    }

    public AbstractSkeletonRenderer(
        EntityRendererProvider.Context pContext, ModelLayerLocation pSkeletonLayer, ModelLayerLocation pInnerModelLayer, SkeletonModel<S> pModel
    ) {
        super(pContext, pModel, 0.5F);
        this.addLayer(
            new HumanoidArmorLayer<>(
                this, new SkeletonModel(pContext.bakeLayer(pSkeletonLayer)), new SkeletonModel(pContext.bakeLayer(pInnerModelLayer)), pContext.getEquipmentRenderer()
            )
        );
    }

    public void extractRenderState(T p_369278_, S p_363603_, float p_362928_) {
        super.extractRenderState(p_369278_, p_363603_, p_362928_);
        p_363603_.isAggressive = p_369278_.isAggressive();
        p_363603_.isShaking = p_369278_.isShaking();
        p_363603_.isHoldingBow = p_369278_.getMainHandItem().is(Items.BOW);
    }

    protected boolean isShaking(S p_366804_) {
        return p_366804_.isShaking;
    }

    protected HumanoidModel.ArmPose getArmPose(AbstractSkeleton p_378753_, HumanoidArm p_376655_) {
        return p_378753_.getMainArm() == p_376655_ && p_378753_.isAggressive() && p_378753_.getMainHandItem().is(Items.BOW)
            ? HumanoidModel.ArmPose.BOW_AND_ARROW
            : HumanoidModel.ArmPose.EMPTY;
    }
}