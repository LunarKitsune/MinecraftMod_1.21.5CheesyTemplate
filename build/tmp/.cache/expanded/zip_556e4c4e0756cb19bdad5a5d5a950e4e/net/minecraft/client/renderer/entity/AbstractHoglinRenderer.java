package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HoglinModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.state.HoglinRenderState;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractHoglinRenderer<T extends Mob & HoglinBase> extends AgeableMobRenderer<T, HoglinRenderState, HoglinModel> {
    public AbstractHoglinRenderer(EntityRendererProvider.Context pContext, ModelLayerLocation pAdultModel, ModelLayerLocation pBabyModel, float pShadowRadius) {
        super(pContext, new HoglinModel(pContext.bakeLayer(pAdultModel)), new HoglinModel(pContext.bakeLayer(pBabyModel)), pShadowRadius);
    }

    public HoglinRenderState createRenderState() {
        return new HoglinRenderState();
    }

    public void extractRenderState(T p_364762_, HoglinRenderState p_364775_, float p_365847_) {
        super.extractRenderState(p_364762_, p_364775_, p_365847_);
        p_364775_.attackAnimationRemainingTicks = p_364762_.getAttackAnimationRemainingTicks();
    }
}