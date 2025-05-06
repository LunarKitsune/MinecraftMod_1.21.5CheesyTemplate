package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SimpleEquipmentLayer<S extends LivingEntityRenderState, RM extends EntityModel<? super S>, EM extends EntityModel<? super S>>
    extends RenderLayer<S, RM> {
    private final EquipmentLayerRenderer equipmentRenderer;
    private final EquipmentClientInfo.LayerType layer;
    private final Function<S, ItemStack> itemGetter;
    private final EM adultModel;
    private final EM babyModel;

    public SimpleEquipmentLayer(
        RenderLayerParent<S, RM> pRenderer,
        EquipmentLayerRenderer pEquipmentRenderer,
        EquipmentClientInfo.LayerType pLayer,
        Function<S, ItemStack> pItemGetter,
        EM pAdultModel,
        EM pBabyModel
    ) {
        super(pRenderer);
        this.equipmentRenderer = pEquipmentRenderer;
        this.layer = pLayer;
        this.itemGetter = pItemGetter;
        this.adultModel = pAdultModel;
        this.babyModel = pBabyModel;
    }

    public SimpleEquipmentLayer(
        RenderLayerParent<S, RM> pRenderer,
        EquipmentLayerRenderer pEquipmentRenderer,
        EM pModel,
        EquipmentClientInfo.LayerType pLayer,
        Function<S, ItemStack> pItemGetter
    ) {
        this(pRenderer, pEquipmentRenderer, pLayer, pItemGetter, pModel, pModel);
    }

    public void render(PoseStack p_392971_, MultiBufferSource p_393255_, int p_396211_, S p_396431_, float p_392529_, float p_395422_) {
        ItemStack itemstack = this.itemGetter.apply(p_396431_);
        Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && !equippable.assetId().isEmpty()) {
            EM em = p_396431_.isBaby ? this.babyModel : this.adultModel;
            em.setupAnim(p_396431_);
            this.equipmentRenderer.renderLayers(this.layer, equippable.assetId().get(), em, itemstack, p_392971_, p_393255_, p_396211_);
        }
    }
}