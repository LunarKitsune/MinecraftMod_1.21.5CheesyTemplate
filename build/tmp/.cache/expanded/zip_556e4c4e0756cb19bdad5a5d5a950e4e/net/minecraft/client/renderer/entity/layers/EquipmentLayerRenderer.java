package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EquipmentLayerRenderer {
    private static final int NO_LAYER_COLOR = 0;
    private final EquipmentAssetManager equipmentAssets;
    private final Function<EquipmentLayerRenderer.LayerTextureKey, ResourceLocation> layerTextureLookup;
    private final Function<EquipmentLayerRenderer.TrimSpriteKey, TextureAtlasSprite> trimSpriteLookup;

    public EquipmentLayerRenderer(EquipmentAssetManager pEquipmentAssets, TextureAtlas pAtlas) {
        this.equipmentAssets = pEquipmentAssets;
        this.layerTextureLookup = Util.memoize(p_374656_ -> p_374656_.layer.getTextureLocation(p_374656_.layerType));
        this.trimSpriteLookup = Util.memoize(p_389518_ -> pAtlas.getSprite(p_389518_.spriteId()));
    }

    public void renderLayers(
        EquipmentClientInfo.LayerType pLayerType,
        ResourceKey<EquipmentAsset> pEquipmentAsset,
        Model pArmorModel,
        ItemStack pItem,
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        int pPackedLight
    ) {
        this.renderLayers(pLayerType, pEquipmentAsset, pArmorModel, pItem, pPoseStack, pBufferSource, pPackedLight, null);
    }

    public void renderLayers(
        EquipmentClientInfo.LayerType pLayerType,
        ResourceKey<EquipmentAsset> pEquipmentAsset,
        Model pArmorModel,
        ItemStack pItem,
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        int pPackedLight,
        @Nullable ResourceLocation pPlayerTexture
    ) {
        List<EquipmentClientInfo.Layer> list = this.equipmentAssets.get(pEquipmentAsset).getLayers(pLayerType);
        if (!list.isEmpty()) {
            int i = DyedItemColor.getOrDefault(pItem, 0);
            boolean flag = pItem.hasFoil();

            for (EquipmentClientInfo.Layer equipmentclientinfo$layer : list) {
                int j = getColorForLayer(equipmentclientinfo$layer, i);
                if (j != 0) {
                    ResourceLocation resourcelocation = equipmentclientinfo$layer.usePlayerTexture() && pPlayerTexture != null
                        ? pPlayerTexture
                        : this.layerTextureLookup.apply(new EquipmentLayerRenderer.LayerTextureKey(pLayerType, equipmentclientinfo$layer));
                    VertexConsumer vertexconsumer = ItemRenderer.getArmorFoilBuffer(pBufferSource, RenderType.armorCutoutNoCull(resourcelocation), flag);
                    pArmorModel.renderToBuffer(pPoseStack, vertexconsumer, pPackedLight, OverlayTexture.NO_OVERLAY, j);
                    flag = false;
                }
            }

            ArmorTrim armortrim = pItem.get(DataComponents.TRIM);
            if (armortrim != null) {
                TextureAtlasSprite textureatlassprite = this.trimSpriteLookup.apply(new EquipmentLayerRenderer.TrimSpriteKey(armortrim, pLayerType, pEquipmentAsset));
                VertexConsumer vertexconsumer1 = textureatlassprite.wrap(
                    pBufferSource.getBuffer(Sheets.armorTrimsSheet(armortrim.pattern().value().decal()))
                );
                pArmorModel.renderToBuffer(pPoseStack, vertexconsumer1, pPackedLight, OverlayTexture.NO_OVERLAY);
            }
        }
    }

    private static int getColorForLayer(EquipmentClientInfo.Layer pLayer, int pColor) {
        Optional<EquipmentClientInfo.Dyeable> optional = pLayer.dyeable();
        if (optional.isPresent()) {
            int i = optional.get().colorWhenUndyed().map(ARGB::opaque).orElse(0);
            return pColor != 0 ? pColor : i;
        } else {
            return -1;
        }
    }

    @OnlyIn(Dist.CLIENT)
    record LayerTextureKey(EquipmentClientInfo.LayerType layerType, EquipmentClientInfo.Layer layer) {
    }

    @OnlyIn(Dist.CLIENT)
    record TrimSpriteKey(ArmorTrim trim, EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> equipmentAssetId) {
        public ResourceLocation spriteId() {
            return this.trim.layerAssetId(this.layerType.trimAssetPrefix(), this.equipmentAssetId);
        }
    }
}