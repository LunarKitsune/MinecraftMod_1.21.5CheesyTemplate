package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombieRenderer extends AbstractZombieRenderer<Zombie, ZombieRenderState, ZombieModel<ZombieRenderState>> {
    public ZombieRenderer(EntityRendererProvider.Context p_174456_) {
        this(
            p_174456_, ModelLayers.ZOMBIE, ModelLayers.ZOMBIE_BABY, ModelLayers.ZOMBIE_INNER_ARMOR, ModelLayers.ZOMBIE_OUTER_ARMOR, ModelLayers.ZOMBIE_BABY_INNER_ARMOR, ModelLayers.ZOMBIE_BABY_OUTER_ARMOR
        );
    }

    public ZombieRenderState createRenderState() {
        return new ZombieRenderState();
    }

    public ZombieRenderer(
        EntityRendererProvider.Context pContext,
        ModelLayerLocation pAdultModel,
        ModelLayerLocation pBabyModel,
        ModelLayerLocation pInnerModel,
        ModelLayerLocation pOuterModel,
        ModelLayerLocation pInnerModelBaby,
        ModelLayerLocation pOuterModelBaby
    ) {
        super(
            pContext,
            new ZombieModel<>(pContext.bakeLayer(pAdultModel)),
            new ZombieModel<>(pContext.bakeLayer(pBabyModel)),
            new ZombieModel<>(pContext.bakeLayer(pInnerModel)),
            new ZombieModel<>(pContext.bakeLayer(pOuterModel)),
            new ZombieModel<>(pContext.bakeLayer(pInnerModelBaby)),
            new ZombieModel<>(pContext.bakeLayer(pOuterModelBaby))
        );
    }
}