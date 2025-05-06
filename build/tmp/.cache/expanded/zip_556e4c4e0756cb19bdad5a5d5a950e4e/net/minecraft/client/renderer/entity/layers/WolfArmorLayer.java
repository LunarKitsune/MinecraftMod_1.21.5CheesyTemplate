package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Crackiness;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WolfArmorLayer extends RenderLayer<WolfRenderState, WolfModel> {
    private final WolfModel adultModel;
    private final WolfModel babyModel;
    private final EquipmentLayerRenderer equipmentRenderer;
    private static final Map<Crackiness.Level, ResourceLocation> ARMOR_CRACK_LOCATIONS = Map.of(
        Crackiness.Level.LOW,
        ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_low.png"),
        Crackiness.Level.MEDIUM,
        ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_medium.png"),
        Crackiness.Level.HIGH,
        ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_high.png")
    );

    public WolfArmorLayer(RenderLayerParent<WolfRenderState, WolfModel> pRenderer, EntityModelSet pEntityModels, EquipmentLayerRenderer pEquipmentRenderer) {
        super(pRenderer);
        this.adultModel = new WolfModel(pEntityModels.bakeLayer(ModelLayers.WOLF_ARMOR));
        this.babyModel = new WolfModel(pEntityModels.bakeLayer(ModelLayers.WOLF_BABY_ARMOR));
        this.equipmentRenderer = pEquipmentRenderer;
    }

    public void render(PoseStack p_332681_, MultiBufferSource p_332805_, int p_332676_, WolfRenderState p_361287_, float p_334070_, float p_332543_) {
        ItemStack itemstack = p_361287_.bodyArmorItem;
        Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && !equippable.assetId().isEmpty()) {
            WolfModel wolfmodel = p_361287_.isBaby ? this.babyModel : this.adultModel;
            wolfmodel.setupAnim(p_361287_);
            this.equipmentRenderer
                .renderLayers(EquipmentClientInfo.LayerType.WOLF_BODY, equippable.assetId().get(), wolfmodel, itemstack, p_332681_, p_332805_, p_332676_);
            this.maybeRenderCracks(p_332681_, p_332805_, p_332676_, itemstack, wolfmodel);
        }
    }

    private void maybeRenderCracks(PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, ItemStack pArmorItem, Model pModel) {
        Crackiness.Level crackiness$level = Crackiness.WOLF_ARMOR.byDamage(pArmorItem);
        if (crackiness$level != Crackiness.Level.NONE) {
            ResourceLocation resourcelocation = ARMOR_CRACK_LOCATIONS.get(crackiness$level);
            VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.armorTranslucent(resourcelocation));
            pModel.renderToBuffer(pPoseStack, vertexconsumer, pPackedLight, OverlayTexture.NO_OVERLAY);
        }
    }
}