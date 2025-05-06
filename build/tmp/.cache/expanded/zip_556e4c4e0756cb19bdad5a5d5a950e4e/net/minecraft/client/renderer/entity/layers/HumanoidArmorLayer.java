package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HumanoidArmorLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> extends RenderLayer<S, M> {
    private final A innerModel;
    private final A outerModel;
    private final A innerModelBaby;
    private final A outerModelBaby;
    private final EquipmentLayerRenderer equipmentRenderer;

    public HumanoidArmorLayer(RenderLayerParent<S, M> pRenderer, A pInnerModel, A pOuterModel, EquipmentLayerRenderer pEquipmentRenderer) {
        this(pRenderer, pInnerModel, pOuterModel, pInnerModel, pOuterModel, pEquipmentRenderer);
    }

    public HumanoidArmorLayer(RenderLayerParent<S, M> pRenderer, A pInnerModel, A pOuterModel, A pInnerModelBaby, A pOuterModelBaby, EquipmentLayerRenderer pEquipmentRenderer) {
        super(pRenderer);
        this.innerModel = pInnerModel;
        this.outerModel = pOuterModel;
        this.innerModelBaby = pInnerModelBaby;
        this.outerModelBaby = pOuterModelBaby;
        this.equipmentRenderer = pEquipmentRenderer;
    }

    public static boolean shouldRender(ItemStack pStack, EquipmentSlot pSlot) {
        Equippable equippable = pStack.get(DataComponents.EQUIPPABLE);
        return equippable != null && shouldRender(equippable, pSlot);
    }

    private static boolean shouldRender(Equippable pEquippable, EquipmentSlot pSlot) {
        return pEquippable.assetId().isPresent() && pEquippable.slot() == pSlot;
    }

    public void render(PoseStack p_117085_, MultiBufferSource p_117086_, int p_117087_, S p_364101_, float p_117089_, float p_117090_) {
        this.renderArmorPiece(p_117085_, p_117086_, p_364101_.chestEquipment, EquipmentSlot.CHEST, p_117087_, this.getArmorModel(p_364101_, EquipmentSlot.CHEST), p_364101_);
        this.renderArmorPiece(p_117085_, p_117086_, p_364101_.legsEquipment, EquipmentSlot.LEGS, p_117087_, this.getArmorModel(p_364101_, EquipmentSlot.LEGS), p_364101_);
        this.renderArmorPiece(p_117085_, p_117086_, p_364101_.feetEquipment, EquipmentSlot.FEET, p_117087_, this.getArmorModel(p_364101_, EquipmentSlot.FEET), p_364101_);
        this.renderArmorPiece(p_117085_, p_117086_, p_364101_.headEquipment, EquipmentSlot.HEAD, p_117087_, this.getArmorModel(p_364101_, EquipmentSlot.HEAD), p_364101_);
    }

    private void renderArmorPiece(PoseStack pPoseStack, MultiBufferSource pBufferSource, ItemStack pArmorItem, EquipmentSlot pSlot, int pPackedLight, A pModel, S state) {
        Equippable equippable = pArmorItem.get(DataComponents.EQUIPPABLE);
        if (equippable != null && shouldRender(equippable, pSlot)) {
            this.getParentModel().copyPropertiesTo(pModel);
            this.setPartVisibility(pModel, pSlot);
            var model = this.getArmorModel(state, pSlot, pArmorItem, pModel);
            EquipmentClientInfo.LayerType equipmentclientinfo$layertype = this.usesInnerModel(pSlot)
                ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
                : EquipmentClientInfo.LayerType.HUMANOID;
            this.equipmentRenderer
                .renderLayers(equipmentclientinfo$layertype, equippable.assetId().orElseThrow(), model, pArmorItem, pPoseStack, pBufferSource, pPackedLight);
        }
    }

    protected void setPartVisibility(A pModel, EquipmentSlot pSlot) {
        pModel.setAllVisible(false);
        switch (pSlot) {
            case HEAD:
                pModel.head.visible = true;
                pModel.hat.visible = true;
                break;
            case CHEST:
                pModel.body.visible = true;
                pModel.rightArm.visible = true;
                pModel.leftArm.visible = true;
                break;
            case LEGS:
                pModel.body.visible = true;
                pModel.rightLeg.visible = true;
                pModel.leftLeg.visible = true;
                break;
            case FEET:
                pModel.rightLeg.visible = true;
                pModel.leftLeg.visible = true;
        }
    }

    protected A getArmorModel(S pRenderState, EquipmentSlot pSlot) {
        if (this.usesInnerModel(pSlot)) {
            return pRenderState.isBaby ? this.innerModelBaby : this.innerModel;
        } else {
            return pRenderState.isBaby ? this.outerModelBaby : this.outerModel;
        }
    }

    private boolean usesInnerModel(EquipmentSlot pSlot) {
        return pSlot == EquipmentSlot.LEGS;
    }

    /**
     * Hook to allow item-sensitive armor model. for HumanoidArmorLayer.
     */
    protected net.minecraft.client.model.Model getArmorModel(S state, EquipmentSlot slot, ItemStack itemStack, A _default) {
       return net.minecraftforge.client.ForgeHooksClient.getArmorModel(state, itemStack, slot, _default);
    }
}
