package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.DonkeyModel;
import net.minecraft.client.model.EquineSaddleModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.DonkeyRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DonkeyRenderer<T extends AbstractChestedHorse> extends AbstractHorseRenderer<T, DonkeyRenderState, DonkeyModel> {
    private final ResourceLocation texture;

    public DonkeyRenderer(EntityRendererProvider.Context pContext, DonkeyRenderer.Type pType) {
        super(pContext, new DonkeyModel(pContext.bakeLayer(pType.model)), new DonkeyModel(pContext.bakeLayer(pType.babyModel)));
        this.texture = pType.texture;
        this.addLayer(
            new SimpleEquipmentLayer<>(
                this,
                pContext.getEquipmentRenderer(),
                pType.saddleLayer,
                p_397593_ -> p_397593_.saddle,
                new EquineSaddleModel(pContext.bakeLayer(pType.saddleModel)),
                new EquineSaddleModel(pContext.bakeLayer(pType.babySaddleModel))
            )
        );
    }

    public ResourceLocation getTextureLocation(DonkeyRenderState p_367902_) {
        return this.texture;
    }

    public DonkeyRenderState createRenderState() {
        return new DonkeyRenderState();
    }

    public void extractRenderState(T p_363167_, DonkeyRenderState p_369827_, float p_366107_) {
        super.extractRenderState(p_363167_, p_369827_, p_366107_);
        p_369827_.hasChest = p_363167_.hasChest();
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Type {
        DONKEY(
            ResourceLocation.withDefaultNamespace("textures/entity/horse/donkey.png"),
            ModelLayers.DONKEY,
            ModelLayers.DONKEY_BABY,
            EquipmentClientInfo.LayerType.DONKEY_SADDLE,
            ModelLayers.DONKEY_SADDLE,
            ModelLayers.DONKEY_BABY_SADDLE
        ),
        MULE(
            ResourceLocation.withDefaultNamespace("textures/entity/horse/mule.png"),
            ModelLayers.MULE,
            ModelLayers.MULE_BABY,
            EquipmentClientInfo.LayerType.MULE_SADDLE,
            ModelLayers.MULE_SADDLE,
            ModelLayers.MULE_BABY_SADDLE
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