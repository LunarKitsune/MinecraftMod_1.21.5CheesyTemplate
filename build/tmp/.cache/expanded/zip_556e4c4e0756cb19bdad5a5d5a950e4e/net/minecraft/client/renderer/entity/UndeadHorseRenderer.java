package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.AbstractEquineModel;
import net.minecraft.client.model.EquineSaddleModel;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class UndeadHorseRenderer extends AbstractHorseRenderer<AbstractHorse, EquineRenderState, AbstractEquineModel<EquineRenderState>> {
    private final ResourceLocation texture;

    public UndeadHorseRenderer(EntityRendererProvider.Context pContext, UndeadHorseRenderer.Type pType) {
        super(pContext, new HorseModel(pContext.bakeLayer(pType.model)), new HorseModel(pContext.bakeLayer(pType.babyModel)));
        this.texture = pType.texture;
        this.addLayer(
            new SimpleEquipmentLayer<>(
                this,
                pContext.getEquipmentRenderer(),
                pType.saddleLayer,
                p_394269_ -> p_394269_.saddle,
                new EquineSaddleModel(pContext.bakeLayer(pType.saddleModel)),
                new EquineSaddleModel(pContext.bakeLayer(pType.babySaddleModel))
            )
        );
    }

    public ResourceLocation getTextureLocation(EquineRenderState p_369447_) {
        return this.texture;
    }

    public EquineRenderState createRenderState() {
        return new EquineRenderState();
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Type {
        SKELETON(
            ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_skeleton.png"),
            ModelLayers.SKELETON_HORSE,
            ModelLayers.SKELETON_HORSE_BABY,
            EquipmentClientInfo.LayerType.SKELETON_HORSE_SADDLE,
            ModelLayers.SKELETON_HORSE_SADDLE,
            ModelLayers.SKELETON_HORSE_BABY_SADDLE
        ),
        ZOMBIE(
            ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_zombie.png"),
            ModelLayers.ZOMBIE_HORSE,
            ModelLayers.ZOMBIE_HORSE_BABY,
            EquipmentClientInfo.LayerType.ZOMBIE_HORSE_SADDLE,
            ModelLayers.ZOMBIE_HORSE_SADDLE,
            ModelLayers.ZOMBIE_HORSE_BABY_SADDLE
        );

        final ResourceLocation texture;
        final ModelLayerLocation model;
        final ModelLayerLocation babyModel;
        final EquipmentClientInfo.LayerType saddleLayer;
        final ModelLayerLocation saddleModel;
        final ModelLayerLocation babySaddleModel;

        private Type(
            final ResourceLocation pTexture,
            final ModelLayerLocation pModel,
            final ModelLayerLocation pBabyModel,
            final EquipmentClientInfo.LayerType pSaddleLayer,
            final ModelLayerLocation pSaddleModel,
            final ModelLayerLocation pBabySaddleModel
        ) {
            this.texture = pTexture;
            this.model = pModel;
            this.babyModel = pBabyModel;
            this.saddleLayer = pSaddleLayer;
            this.saddleModel = pSaddleModel;
            this.babySaddleModel = pBabySaddleModel;
        }
    }
}