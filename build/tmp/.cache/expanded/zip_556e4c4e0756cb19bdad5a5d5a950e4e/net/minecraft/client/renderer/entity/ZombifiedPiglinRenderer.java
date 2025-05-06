package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.ZombifiedPiglinModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ZombifiedPiglinRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombifiedPiglinRenderer extends HumanoidMobRenderer<ZombifiedPiglin, ZombifiedPiglinRenderState, ZombifiedPiglinModel> {
    private static final ResourceLocation ZOMBIFIED_PIGLIN_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/piglin/zombified_piglin.png");

    public ZombifiedPiglinRenderer(
        EntityRendererProvider.Context pContext,
        ModelLayerLocation pAdultModel,
        ModelLayerLocation pBabyModel,
        ModelLayerLocation pInnerArmorLayer,
        ModelLayerLocation pOuterArmorLayer,
        ModelLayerLocation pInnerArmorBaby,
        ModelLayerLocation pOuterArmorBaby
    ) {
        super(
            pContext,
            new ZombifiedPiglinModel(pContext.bakeLayer(pAdultModel)),
            new ZombifiedPiglinModel(pContext.bakeLayer(pBabyModel)),
            0.5F,
            PiglinRenderer.PIGLIN_CUSTOM_HEAD_TRANSFORMS
        );
        this.addLayer(
            new HumanoidArmorLayer<>(
                this,
                new HumanoidArmorModel(pContext.bakeLayer(pInnerArmorLayer)),
                new HumanoidArmorModel(pContext.bakeLayer(pOuterArmorLayer)),
                new HumanoidArmorModel(pContext.bakeLayer(pInnerArmorBaby)),
                new HumanoidArmorModel(pContext.bakeLayer(pOuterArmorBaby)),
                pContext.getEquipmentRenderer()
            )
        );
    }

    public ResourceLocation getTextureLocation(ZombifiedPiglinRenderState p_369247_) {
        return ZOMBIFIED_PIGLIN_LOCATION;
    }

    public ZombifiedPiglinRenderState createRenderState() {
        return new ZombifiedPiglinRenderState();
    }

    public void extractRenderState(ZombifiedPiglin p_365896_, ZombifiedPiglinRenderState p_360783_, float p_367145_) {
        super.extractRenderState(p_365896_, p_360783_, p_367145_);
        p_360783_.isAggressive = p_365896_.isAggressive();
    }
}