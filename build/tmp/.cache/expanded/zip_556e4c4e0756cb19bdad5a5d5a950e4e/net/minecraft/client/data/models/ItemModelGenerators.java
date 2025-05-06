package net.minecraft.client.data.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import net.minecraft.client.color.item.Dye;
import net.minecraft.client.color.item.Firework;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.MapColor;
import net.minecraft.client.color.item.Potion;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.item.BundleSelectedItemSpecialRenderer;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.properties.conditional.Broken;
import net.minecraft.client.renderer.item.properties.conditional.BundleHasSelectedItem;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.conditional.FishingRodCast;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngle;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngleState;
import net.minecraft.client.renderer.item.properties.numeric.CrossbowPull;
import net.minecraft.client.renderer.item.properties.numeric.Time;
import net.minecraft.client.renderer.item.properties.numeric.UseCycle;
import net.minecraft.client.renderer.item.properties.numeric.UseDuration;
import net.minecraft.client.renderer.item.properties.select.Charge;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.client.renderer.item.properties.select.TrimMaterialProperty;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.client.renderer.special.TridentSpecialRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.trim.MaterialAssetGroup;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemModelGenerators {
    private static final ItemTintSource BLANK_LAYER = ItemModelUtils.constantTint(-1);
    public static final ResourceLocation TRIM_PREFIX_HELMET = prefixForSlotTrim("helmet");
    public static final ResourceLocation TRIM_PREFIX_CHESTPLATE = prefixForSlotTrim("chestplate");
    public static final ResourceLocation TRIM_PREFIX_LEGGINGS = prefixForSlotTrim("leggings");
    public static final ResourceLocation TRIM_PREFIX_BOOTS = prefixForSlotTrim("boots");
    public static final List<ItemModelGenerators.TrimMaterialData> TRIM_MATERIAL_MODELS = List.of(
        new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.QUARTZ, TrimMaterials.QUARTZ),
        new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.IRON, TrimMaterials.IRON),
        new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.NETHERITE, TrimMaterials.NETHERITE),
        new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.REDSTONE, TrimMaterials.REDSTONE),
        new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.COPPER, TrimMaterials.COPPER),
        new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.GOLD, TrimMaterials.GOLD),
        new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.EMERALD, TrimMaterials.EMERALD),
        new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.DIAMOND, TrimMaterials.DIAMOND),
        new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.LAPIS, TrimMaterials.LAPIS),
        new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.AMETHYST, TrimMaterials.AMETHYST),
        new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.RESIN, TrimMaterials.RESIN)
    );
    protected final ItemModelOutput itemModelOutput;
    protected final BiConsumer<ResourceLocation, ModelInstance> modelOutput;

    public static ResourceLocation prefixForSlotTrim(String pName) {
        return ResourceLocation.withDefaultNamespace("trims/items/" + pName + "_trim");
    }

    public ItemModelGenerators(ItemModelOutput pItemModelOutput, BiConsumer<ResourceLocation, ModelInstance> pModelOutput) {
        this.itemModelOutput = pItemModelOutput;
        this.modelOutput = pModelOutput;
    }

    protected void declareCustomModelItem(Item pItem) {
        this.itemModelOutput.accept(pItem, ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(pItem)));
    }

    protected ResourceLocation createFlatItemModel(Item pItem, ModelTemplate pModelTemplate) {
        return pModelTemplate.create(ModelLocationUtils.getModelLocation(pItem), TextureMapping.layer0(pItem), this.modelOutput);
    }

    protected void generateFlatItem(Item pItem, ModelTemplate pModelTemplate) {
        this.itemModelOutput.accept(pItem, ItemModelUtils.plainModel(this.createFlatItemModel(pItem, pModelTemplate)));
    }

    protected ResourceLocation createFlatItemModel(Item pItem, String pSuffix, ModelTemplate pModelTemplate) {
        return pModelTemplate.create(
            ModelLocationUtils.getModelLocation(pItem, pSuffix), TextureMapping.layer0(TextureMapping.getItemTexture(pItem, pSuffix)), this.modelOutput
        );
    }

    protected ResourceLocation createFlatItemModel(Item pItem, Item pLayerZeroItem, ModelTemplate pModelTemplate) {
        return pModelTemplate.create(ModelLocationUtils.getModelLocation(pItem), TextureMapping.layer0(pLayerZeroItem), this.modelOutput);
    }

    protected void generateFlatItem(Item pItem, Item pLayerZeroItem, ModelTemplate pModelTemplate) {
        this.itemModelOutput.accept(pItem, ItemModelUtils.plainModel(this.createFlatItemModel(pItem, pLayerZeroItem, pModelTemplate)));
    }

    protected void generateItemWithTintedOverlay(Item pItem, ItemTintSource pTintSource) {
        this.generateItemWithTintedOverlay(pItem, "_overlay", pTintSource);
    }

    protected void generateItemWithTintedOverlay(Item pItem, String pSuffix, ItemTintSource pTintSource) {
        ResourceLocation resourcelocation = this.generateLayeredItem(pItem, TextureMapping.getItemTexture(pItem), TextureMapping.getItemTexture(pItem, pSuffix));
        this.itemModelOutput.accept(pItem, ItemModelUtils.tintedModel(resourcelocation, BLANK_LAYER, pTintSource));
    }

    protected List<RangeSelectItemModel.Entry> createCompassModels(Item pItem) {
        List<RangeSelectItemModel.Entry> list = new ArrayList<>();
        ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(this.createFlatItemModel(pItem, "_16", ModelTemplates.FLAT_ITEM));
        list.add(ItemModelUtils.override(itemmodel$unbaked, 0.0F));

        for (int i = 1; i < 32; i++) {
            int j = Mth.positiveModulo(i - 16, 32);
            ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.plainModel(
                this.createFlatItemModel(pItem, String.format(Locale.ROOT, "_%02d", j), ModelTemplates.FLAT_ITEM)
            );
            list.add(ItemModelUtils.override(itemmodel$unbaked1, i - 0.5F));
        }

        list.add(ItemModelUtils.override(itemmodel$unbaked, 31.5F));
        return list;
    }

    protected void generateStandardCompassItem(Item pItem) {
        List<RangeSelectItemModel.Entry> list = this.createCompassModels(pItem);
        this.itemModelOutput
            .accept(
                pItem,
                ItemModelUtils.conditional(
                    ItemModelUtils.hasComponent(DataComponents.LODESTONE_TRACKER),
                    ItemModelUtils.rangeSelect(new CompassAngle(true, CompassAngleState.CompassTarget.LODESTONE), 32.0F, list),
                    ItemModelUtils.inOverworld(
                        ItemModelUtils.rangeSelect(new CompassAngle(true, CompassAngleState.CompassTarget.SPAWN), 32.0F, list),
                        ItemModelUtils.rangeSelect(new CompassAngle(true, CompassAngleState.CompassTarget.NONE), 32.0F, list)
                    )
                )
            );
    }

    protected void generateRecoveryCompassItem(Item pItem) {
        this.itemModelOutput
            .accept(pItem, ItemModelUtils.rangeSelect(new CompassAngle(true, CompassAngleState.CompassTarget.RECOVERY), 32.0F, this.createCompassModels(pItem)));
    }

    protected void generateClockItem(Item pItem) {
        List<RangeSelectItemModel.Entry> list = new ArrayList<>();
        ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(this.createFlatItemModel(pItem, "_00", ModelTemplates.FLAT_ITEM));
        list.add(ItemModelUtils.override(itemmodel$unbaked, 0.0F));

        for (int i = 1; i < 64; i++) {
            ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.plainModel(
                this.createFlatItemModel(pItem, String.format(Locale.ROOT, "_%02d", i), ModelTemplates.FLAT_ITEM)
            );
            list.add(ItemModelUtils.override(itemmodel$unbaked1, i - 0.5F));
        }

        list.add(ItemModelUtils.override(itemmodel$unbaked, 63.5F));
        this.itemModelOutput
            .accept(
                pItem,
                ItemModelUtils.inOverworld(
                    ItemModelUtils.rangeSelect(new Time(true, Time.TimeSource.DAYTIME), 64.0F, list),
                    ItemModelUtils.rangeSelect(new Time(true, Time.TimeSource.RANDOM), 64.0F, list)
                )
            );
    }

    protected ResourceLocation generateLayeredItem(Item pItem, ResourceLocation pLayer0, ResourceLocation pLayer1) {
        return ModelTemplates.TWO_LAYERED_ITEM.create(pItem, TextureMapping.layered(pLayer0, pLayer1), this.modelOutput);
    }

    protected ResourceLocation generateLayeredItem(ResourceLocation pModelLocation, ResourceLocation pLayer0, ResourceLocation pLayer1) {
        return ModelTemplates.TWO_LAYERED_ITEM.create(pModelLocation, TextureMapping.layered(pLayer0, pLayer1), this.modelOutput);
    }

    protected void generateLayeredItem(ResourceLocation pModelLocation, ResourceLocation pLayer0, ResourceLocation pLayer1, ResourceLocation pLayer2) {
        ModelTemplates.THREE_LAYERED_ITEM.create(pModelLocation, TextureMapping.layered(pLayer0, pLayer1, pLayer2), this.modelOutput);
    }

    protected void generateTrimmableItem(Item pItem, ResourceKey<EquipmentAsset> pEquipmentAsset, ResourceLocation pModelId, boolean pUsesSecondLayer) {
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(pItem);
        ResourceLocation resourcelocation1 = TextureMapping.getItemTexture(pItem);
        ResourceLocation resourcelocation2 = TextureMapping.getItemTexture(pItem, "_overlay");
        List<SelectItemModel.SwitchCase<ResourceKey<TrimMaterial>>> list = new ArrayList<>(TRIM_MATERIAL_MODELS.size());

        for (ItemModelGenerators.TrimMaterialData itemmodelgenerators$trimmaterialdata : TRIM_MATERIAL_MODELS) {
            ResourceLocation resourcelocation3 = resourcelocation.withSuffix(
                "_" + itemmodelgenerators$trimmaterialdata.assets().base().suffix() + "_trim"
            );
            ResourceLocation resourcelocation4 = pModelId.withSuffix("_" + itemmodelgenerators$trimmaterialdata.assets().assetId(pEquipmentAsset).suffix());
            ItemModel.Unbaked itemmodel$unbaked;
            if (pUsesSecondLayer) {
                this.generateLayeredItem(resourcelocation3, resourcelocation1, resourcelocation2, resourcelocation4);
                itemmodel$unbaked = ItemModelUtils.tintedModel(resourcelocation3, new Dye(-6265536));
            } else {
                this.generateLayeredItem(resourcelocation3, resourcelocation1, resourcelocation4);
                itemmodel$unbaked = ItemModelUtils.plainModel(resourcelocation3);
            }

            list.add(ItemModelUtils.when(itemmodelgenerators$trimmaterialdata.materialKey, itemmodel$unbaked));
        }

        ItemModel.Unbaked itemmodel$unbaked1;
        if (pUsesSecondLayer) {
            ModelTemplates.TWO_LAYERED_ITEM.create(resourcelocation, TextureMapping.layered(resourcelocation1, resourcelocation2), this.modelOutput);
            itemmodel$unbaked1 = ItemModelUtils.tintedModel(resourcelocation, new Dye(-6265536));
        } else {
            ModelTemplates.FLAT_ITEM.create(resourcelocation, TextureMapping.layer0(resourcelocation1), this.modelOutput);
            itemmodel$unbaked1 = ItemModelUtils.plainModel(resourcelocation);
        }

        this.itemModelOutput.accept(pItem, ItemModelUtils.select(new TrimMaterialProperty(), itemmodel$unbaked1, list));
    }

    protected void generateBundleModels(Item pBundleItem) {
        ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(this.createFlatItemModel(pBundleItem, ModelTemplates.FLAT_ITEM));
        ResourceLocation resourcelocation = this.generateBundleCoverModel(pBundleItem, ModelTemplates.BUNDLE_OPEN_BACK_INVENTORY, "_open_back");
        ResourceLocation resourcelocation1 = this.generateBundleCoverModel(pBundleItem, ModelTemplates.BUNDLE_OPEN_FRONT_INVENTORY, "_open_front");
        ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.composite(
            ItemModelUtils.plainModel(resourcelocation), new BundleSelectedItemSpecialRenderer.Unbaked(), ItemModelUtils.plainModel(resourcelocation1)
        );
        ItemModel.Unbaked itemmodel$unbaked2 = ItemModelUtils.conditional(new BundleHasSelectedItem(), itemmodel$unbaked1, itemmodel$unbaked);
        this.itemModelOutput
            .accept(
                pBundleItem,
                ItemModelUtils.select(new DisplayContext(), itemmodel$unbaked, ItemModelUtils.when(ItemDisplayContext.GUI, itemmodel$unbaked2))
            );
    }

    protected ResourceLocation generateBundleCoverModel(Item pBundleItem, ModelTemplate pModelTemplate, String pSuffix) {
        ResourceLocation resourcelocation = TextureMapping.getItemTexture(pBundleItem, pSuffix);
        return pModelTemplate.create(pBundleItem, TextureMapping.layer0(resourcelocation), this.modelOutput);
    }

    protected void generateBow(Item pBowItem) {
        ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(pBowItem));
        ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.plainModel(this.createFlatItemModel(pBowItem, "_pulling_0", ModelTemplates.BOW));
        ItemModel.Unbaked itemmodel$unbaked2 = ItemModelUtils.plainModel(this.createFlatItemModel(pBowItem, "_pulling_1", ModelTemplates.BOW));
        ItemModel.Unbaked itemmodel$unbaked3 = ItemModelUtils.plainModel(this.createFlatItemModel(pBowItem, "_pulling_2", ModelTemplates.BOW));
        this.itemModelOutput
            .accept(
                pBowItem,
                ItemModelUtils.conditional(
                    ItemModelUtils.isUsingItem(),
                    ItemModelUtils.rangeSelect(
                        new UseDuration(false),
                        0.05F,
                        itemmodel$unbaked1,
                        ItemModelUtils.override(itemmodel$unbaked2, 0.65F),
                        ItemModelUtils.override(itemmodel$unbaked3, 0.9F)
                    ),
                    itemmodel$unbaked
                )
            );
    }

    protected void generateCrossbow(Item pCrossbowItem) {
        ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(pCrossbowItem));
        ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.plainModel(this.createFlatItemModel(pCrossbowItem, "_pulling_0", ModelTemplates.CROSSBOW));
        ItemModel.Unbaked itemmodel$unbaked2 = ItemModelUtils.plainModel(this.createFlatItemModel(pCrossbowItem, "_pulling_1", ModelTemplates.CROSSBOW));
        ItemModel.Unbaked itemmodel$unbaked3 = ItemModelUtils.plainModel(this.createFlatItemModel(pCrossbowItem, "_pulling_2", ModelTemplates.CROSSBOW));
        ItemModel.Unbaked itemmodel$unbaked4 = ItemModelUtils.plainModel(this.createFlatItemModel(pCrossbowItem, "_arrow", ModelTemplates.CROSSBOW));
        ItemModel.Unbaked itemmodel$unbaked5 = ItemModelUtils.plainModel(this.createFlatItemModel(pCrossbowItem, "_firework", ModelTemplates.CROSSBOW));
        this.itemModelOutput
            .accept(
                pCrossbowItem,
                ItemModelUtils.select(
                    new Charge(),
                    ItemModelUtils.conditional(
                        ItemModelUtils.isUsingItem(),
                        ItemModelUtils.rangeSelect(
                            new CrossbowPull(),
                            itemmodel$unbaked1,
                            ItemModelUtils.override(itemmodel$unbaked2, 0.58F),
                            ItemModelUtils.override(itemmodel$unbaked3, 1.0F)
                        ),
                        itemmodel$unbaked
                    ),
                    ItemModelUtils.when(CrossbowItem.ChargeType.ARROW, itemmodel$unbaked4),
                    ItemModelUtils.when(CrossbowItem.ChargeType.ROCKET, itemmodel$unbaked5)
                )
            );
    }

    protected void generateBooleanDispatch(Item pItem, ConditionalItemModelProperty pProperty, ItemModel.Unbaked pTrueModel, ItemModel.Unbaked pFalseModel) {
        this.itemModelOutput.accept(pItem, ItemModelUtils.conditional(pProperty, pTrueModel, pFalseModel));
    }

    protected void generateElytra(Item pElytraItem) {
        ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(this.createFlatItemModel(pElytraItem, ModelTemplates.FLAT_ITEM));
        ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.plainModel(this.createFlatItemModel(pElytraItem, "_broken", ModelTemplates.FLAT_ITEM));
        this.generateBooleanDispatch(pElytraItem, new Broken(), itemmodel$unbaked1, itemmodel$unbaked);
    }

    protected void generateBrush(Item pBrushItem) {
        ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(pBrushItem));
        ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(pBrushItem, "_brushing_0"));
        ItemModel.Unbaked itemmodel$unbaked2 = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(pBrushItem, "_brushing_1"));
        ItemModel.Unbaked itemmodel$unbaked3 = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(pBrushItem, "_brushing_2"));
        this.itemModelOutput
            .accept(
                pBrushItem,
                ItemModelUtils.rangeSelect(
                    new UseCycle(10.0F),
                    0.1F,
                    itemmodel$unbaked,
                    ItemModelUtils.override(itemmodel$unbaked1, 0.25F),
                    ItemModelUtils.override(itemmodel$unbaked2, 0.5F),
                    ItemModelUtils.override(itemmodel$unbaked3, 0.75F)
                )
            );
    }

    protected void generateFishingRod(Item pFishingRodItem) {
        ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(this.createFlatItemModel(pFishingRodItem, ModelTemplates.FLAT_HANDHELD_ROD_ITEM));
        ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.plainModel(this.createFlatItemModel(pFishingRodItem, "_cast", ModelTemplates.FLAT_HANDHELD_ROD_ITEM));
        this.generateBooleanDispatch(pFishingRodItem, new FishingRodCast(), itemmodel$unbaked1, itemmodel$unbaked);
    }

    protected void generateGoatHorn(Item pGoatHornItem) {
        ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(pGoatHornItem));
        ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.plainModel(ModelLocationUtils.decorateItemModelLocation("tooting_goat_horn"));
        this.generateBooleanDispatch(pGoatHornItem, ItemModelUtils.isUsingItem(), itemmodel$unbaked1, itemmodel$unbaked);
    }

    protected void generateShield(Item pShieldItem) {
        ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.specialModel(ModelLocationUtils.getModelLocation(pShieldItem), new ShieldSpecialRenderer.Unbaked());
        ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.specialModel(
            ModelLocationUtils.getModelLocation(pShieldItem, "_blocking"), new ShieldSpecialRenderer.Unbaked()
        );
        this.generateBooleanDispatch(pShieldItem, ItemModelUtils.isUsingItem(), itemmodel$unbaked1, itemmodel$unbaked);
    }

    protected static ItemModel.Unbaked createFlatModelDispatch(ItemModel.Unbaked pItemModel, ItemModel.Unbaked pHoldingModel) {
        return ItemModelUtils.select(
            new DisplayContext(),
            pHoldingModel,
            ItemModelUtils.when(List.of(ItemDisplayContext.GUI, ItemDisplayContext.GROUND, ItemDisplayContext.FIXED), pItemModel)
        );
    }

    protected void generateSpyglass(Item pSpyglassItem) {
        ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(this.createFlatItemModel(pSpyglassItem, ModelTemplates.FLAT_ITEM));
        ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(pSpyglassItem, "_in_hand"));
        this.itemModelOutput.accept(pSpyglassItem, createFlatModelDispatch(itemmodel$unbaked, itemmodel$unbaked1));
    }

    protected void generateTrident(Item pTridentItem) {
        ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(this.createFlatItemModel(pTridentItem, ModelTemplates.FLAT_ITEM));
        ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.specialModel(
            ModelLocationUtils.getModelLocation(pTridentItem, "_in_hand"), new TridentSpecialRenderer.Unbaked()
        );
        ItemModel.Unbaked itemmodel$unbaked2 = ItemModelUtils.specialModel(
            ModelLocationUtils.getModelLocation(pTridentItem, "_throwing"), new TridentSpecialRenderer.Unbaked()
        );
        ItemModel.Unbaked itemmodel$unbaked3 = ItemModelUtils.conditional(ItemModelUtils.isUsingItem(), itemmodel$unbaked2, itemmodel$unbaked1);
        this.itemModelOutput.accept(pTridentItem, createFlatModelDispatch(itemmodel$unbaked, itemmodel$unbaked3));
    }

    protected void addPotionTint(Item pPotionItem, ResourceLocation pModel) {
        this.itemModelOutput.accept(pPotionItem, ItemModelUtils.tintedModel(pModel, new Potion()));
    }

    protected void generatePotion(Item pPotionItem) {
        ResourceLocation resourcelocation = this.generateLayeredItem(pPotionItem, ModelLocationUtils.decorateItemModelLocation("potion_overlay"), ModelLocationUtils.getModelLocation(pPotionItem));
        this.addPotionTint(pPotionItem, resourcelocation);
    }

    protected void generateTippedArrow(Item pArrowItem) {
        ResourceLocation resourcelocation = this.generateLayeredItem(
            pArrowItem, ModelLocationUtils.getModelLocation(pArrowItem, "_head"), ModelLocationUtils.getModelLocation(pArrowItem, "_base")
        );
        this.addPotionTint(pArrowItem, resourcelocation);
    }

    protected void generateDyedItem(Item pItem, int pColor) {
        ResourceLocation resourcelocation = this.createFlatItemModel(pItem, ModelTemplates.FLAT_ITEM);
        this.itemModelOutput.accept(pItem, ItemModelUtils.tintedModel(resourcelocation, new Dye(pColor)));
    }

    protected void generateWolfArmor(Item pArmorItem) {
        ResourceLocation resourcelocation = TextureMapping.getItemTexture(pArmorItem);
        ResourceLocation resourcelocation1 = TextureMapping.getItemTexture(pArmorItem, "_overlay");
        ResourceLocation resourcelocation2 = ModelTemplates.FLAT_ITEM.create(pArmorItem, TextureMapping.layer0(resourcelocation), this.modelOutput);
        ResourceLocation resourcelocation3 = ModelLocationUtils.getModelLocation(pArmorItem, "_dyed");
        ModelTemplates.TWO_LAYERED_ITEM.create(resourcelocation3, TextureMapping.layered(resourcelocation, resourcelocation1), this.modelOutput);
        this.itemModelOutput
            .accept(
                pArmorItem,
                ItemModelUtils.conditional(
                    ItemModelUtils.hasComponent(DataComponents.DYED_COLOR),
                    ItemModelUtils.tintedModel(resourcelocation3, BLANK_LAYER, new Dye(0)),
                    ItemModelUtils.plainModel(resourcelocation2)
                )
            );
    }

    public void run() {
        this.generateFlatItem(Items.ACACIA_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.CHERRY_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ACACIA_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.CHERRY_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.AMETHYST_SHARD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.APPLE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ARMADILLO_SCUTE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ARMOR_STAND, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ARROW, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BAKED_POTATO, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BAMBOO, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.BEEF, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BEETROOT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BEETROOT_SOUP, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BIRCH_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BIRCH_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BLACK_DYE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BLAZE_POWDER, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BLAZE_ROD, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.BLUE_DYE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BONE_MEAL, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BORDURE_INDENTED_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BOOK, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BOWL, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BREAD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BRICK, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BREEZE_ROD, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.BROWN_DYE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BUCKET, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.CARROT_ON_A_STICK, ModelTemplates.FLAT_HANDHELD_ROD_ITEM);
        this.generateFlatItem(Items.WARPED_FUNGUS_ON_A_STICK, ModelTemplates.FLAT_HANDHELD_ROD_ITEM);
        this.generateFlatItem(Items.CHARCOAL, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.CHEST_MINECART, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.CHICKEN, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.CHORUS_FRUIT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.CLAY_BALL, ModelTemplates.FLAT_ITEM);
        this.generateClockItem(Items.CLOCK);
        this.generateFlatItem(Items.COAL, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.COD_BUCKET, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.COMMAND_BLOCK_MINECART, ModelTemplates.FLAT_ITEM);
        this.generateStandardCompassItem(Items.COMPASS);
        this.generateRecoveryCompassItem(Items.RECOVERY_COMPASS);
        this.generateFlatItem(Items.COOKED_BEEF, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.COOKED_CHICKEN, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.COOKED_COD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.COOKED_MUTTON, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.COOKED_PORKCHOP, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.COOKED_RABBIT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.COOKED_SALMON, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.COOKIE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.RAW_COPPER, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.COPPER_INGOT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.CREEPER_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.CYAN_DYE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.DARK_OAK_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.DARK_OAK_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.DIAMOND, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.DIAMOND_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.DIAMOND_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.DIAMOND_HORSE_ARMOR, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.DIAMOND_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.DIAMOND_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.DIAMOND_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.DRAGON_BREATH, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.DRIED_KELP, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BLUE_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BROWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.EMERALD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ENCHANTED_BOOK, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ENDER_EYE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ENDER_PEARL, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.END_CRYSTAL, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.EXPERIENCE_BOTTLE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.FERMENTED_SPIDER_EYE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.FIELD_MASONED_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.FIREWORK_ROCKET, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.FIRE_CHARGE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.FLINT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.FLINT_AND_STEEL, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.FLOW_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.FLOWER_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.FURNACE_MINECART, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GHAST_TEAR, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GLASS_BOTTLE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GLISTERING_MELON_SLICE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GLOBE_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GLOW_BERRIES, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GLOWSTONE_DUST, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GLOW_INK_SAC, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GLOW_ITEM_FRAME, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.RAW_GOLD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GOLDEN_APPLE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GOLDEN_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.GOLDEN_CARROT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GOLDEN_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.GOLDEN_HORSE_ARMOR, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GOLDEN_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.GOLDEN_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.GOLDEN_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.GOLD_INGOT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GOLD_NUGGET, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GRAY_DYE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GREEN_DYE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GUNPOWDER, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GUSTER_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.HEART_OF_THE_SEA, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.HONEYCOMB, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.HONEY_BOTTLE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.HOPPER_MINECART, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.INK_SAC, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.RAW_IRON, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.IRON_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.IRON_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.IRON_HORSE_ARMOR, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.IRON_INGOT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.IRON_NUGGET, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.IRON_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.IRON_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.IRON_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.ITEM_FRAME, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.JUNGLE_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.JUNGLE_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.KNOWLEDGE_BOOK, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.LAPIS_LAZULI, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.LAVA_BUCKET, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.LEATHER, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.LIGHT_BLUE_DYE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.LIGHT_GRAY_DYE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.LIME_DYE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.MAGENTA_DYE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.MAGMA_CREAM, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.MANGROVE_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.MANGROVE_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BAMBOO_RAFT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BAMBOO_CHEST_RAFT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.MAP, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.MELON_SLICE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.MILK_BUCKET, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.MINECART, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.MOJANG_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.MUSHROOM_STEW, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.DISC_FRAGMENT_5, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.MUSIC_DISC_11, ModelTemplates.MUSIC_DISC);
        this.generateFlatItem(Items.MUSIC_DISC_13, ModelTemplates.MUSIC_DISC);
        this.generateFlatItem(Items.MUSIC_DISC_BLOCKS, ModelTemplates.MUSIC_DISC);
        this.generateFlatItem(Items.MUSIC_DISC_CAT, ModelTemplates.MUSIC_DISC);
        this.generateFlatItem(Items.MUSIC_DISC_CHIRP, ModelTemplates.MUSIC_DISC);
        this.generateFlatItem(Items.MUSIC_DISC_CREATOR, ModelTemplates.MUSIC_DISC);
        this.generateFlatItem(Items.MUSIC_DISC_CREATOR_MUSIC_BOX, ModelTemplates.MUSIC_DISC);
        this.generateFlatItem(Items.MUSIC_DISC_FAR, ModelTemplates.MUSIC_DISC);
        this.generateFlatItem(Items.MUSIC_DISC_MALL, ModelTemplates.MUSIC_DISC);
        this.generateFlatItem(Items.MUSIC_DISC_MELLOHI, ModelTemplates.MUSIC_DISC);
        this.generateFlatItem(Items.MUSIC_DISC_PIGSTEP, ModelTemplates.MUSIC_DISC);
        this.generateFlatItem(Items.MUSIC_DISC_PRECIPICE, ModelTemplates.MUSIC_DISC);
        this.generateFlatItem(Items.MUSIC_DISC_STAL, ModelTemplates.MUSIC_DISC);
        this.generateFlatItem(Items.MUSIC_DISC_STRAD, ModelTemplates.MUSIC_DISC);
        this.generateFlatItem(Items.MUSIC_DISC_WAIT, ModelTemplates.MUSIC_DISC);
        this.generateFlatItem(Items.MUSIC_DISC_WARD, ModelTemplates.MUSIC_DISC);
        this.generateFlatItem(Items.MUSIC_DISC_OTHERSIDE, ModelTemplates.MUSIC_DISC);
        this.generateFlatItem(Items.MUSIC_DISC_RELIC, ModelTemplates.MUSIC_DISC);
        this.generateFlatItem(Items.MUSIC_DISC_5, ModelTemplates.MUSIC_DISC);
        this.generateFlatItem(Items.MUTTON, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.NAME_TAG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.NAUTILUS_SHELL, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.NETHERITE_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.NETHERITE_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.NETHERITE_INGOT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.NETHERITE_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.NETHERITE_SCRAP, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.NETHERITE_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.NETHERITE_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.NETHER_BRICK, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.RESIN_BRICK, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.NETHER_STAR, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.OAK_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.OAK_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ORANGE_DYE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PAINTING, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PALE_OAK_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PALE_OAK_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PAPER, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PHANTOM_MEMBRANE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PIGLIN_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PINK_DYE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.POISONOUS_POTATO, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.POPPED_CHORUS_FRUIT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PORKCHOP, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.POWDER_SNOW_BUCKET, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PRISMARINE_CRYSTALS, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PRISMARINE_SHARD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PUFFERFISH, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PUFFERFISH_BUCKET, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PUMPKIN_PIE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PURPLE_DYE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.QUARTZ, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.RABBIT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.RABBIT_FOOT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.RABBIT_HIDE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.RABBIT_STEW, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.RED_DYE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ROTTEN_FLESH, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SADDLE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SALMON, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SALMON_BUCKET, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.TURTLE_SCUTE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SHEARS, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SHULKER_SHELL, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SKULL_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SLIME_BALL, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SNOWBALL, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ECHO_SHARD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SPECTRAL_ARROW, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SPIDER_EYE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SPRUCE_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SPRUCE_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.STICK, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.STONE_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.STONE_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.STONE_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.STONE_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.STONE_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.SUGAR, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SUSPICIOUS_STEW, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.TNT_MINECART, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.TOTEM_OF_UNDYING, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.TROPICAL_FISH, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.TROPICAL_FISH_BUCKET, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.AXOLOTL_BUCKET, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.TADPOLE_BUCKET, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.WATER_BUCKET, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.WHEAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.WHITE_DYE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.WIND_CHARGE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.MACE, ModelTemplates.FLAT_HANDHELD_MACE_ITEM);
        this.generateFlatItem(Items.WOODEN_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.WOODEN_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.WOODEN_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.WOODEN_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.WOODEN_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.WRITABLE_BOOK, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.WRITTEN_BOOK, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.YELLOW_DYE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.DEBUG_STICK, Items.STICK, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE, ModelTemplates.FLAT_ITEM);
        this.generateTrimmableItem(Items.TURTLE_HELMET, EquipmentAssets.TURTLE_SCUTE, TRIM_PREFIX_HELMET, false);
        this.generateTrimmableItem(Items.LEATHER_HELMET, EquipmentAssets.LEATHER, TRIM_PREFIX_HELMET, true);
        this.generateTrimmableItem(Items.LEATHER_CHESTPLATE, EquipmentAssets.LEATHER, TRIM_PREFIX_CHESTPLATE, true);
        this.generateTrimmableItem(Items.LEATHER_LEGGINGS, EquipmentAssets.LEATHER, TRIM_PREFIX_LEGGINGS, true);
        this.generateTrimmableItem(Items.LEATHER_BOOTS, EquipmentAssets.LEATHER, TRIM_PREFIX_BOOTS, true);
        this.generateTrimmableItem(Items.CHAINMAIL_HELMET, EquipmentAssets.CHAINMAIL, TRIM_PREFIX_HELMET, false);
        this.generateTrimmableItem(Items.CHAINMAIL_CHESTPLATE, EquipmentAssets.CHAINMAIL, TRIM_PREFIX_CHESTPLATE, false);
        this.generateTrimmableItem(Items.CHAINMAIL_LEGGINGS, EquipmentAssets.CHAINMAIL, TRIM_PREFIX_LEGGINGS, false);
        this.generateTrimmableItem(Items.CHAINMAIL_BOOTS, EquipmentAssets.CHAINMAIL, TRIM_PREFIX_BOOTS, false);
        this.generateTrimmableItem(Items.IRON_HELMET, EquipmentAssets.IRON, TRIM_PREFIX_HELMET, false);
        this.generateTrimmableItem(Items.IRON_CHESTPLATE, EquipmentAssets.IRON, TRIM_PREFIX_CHESTPLATE, false);
        this.generateTrimmableItem(Items.IRON_LEGGINGS, EquipmentAssets.IRON, TRIM_PREFIX_LEGGINGS, false);
        this.generateTrimmableItem(Items.IRON_BOOTS, EquipmentAssets.IRON, TRIM_PREFIX_BOOTS, false);
        this.generateTrimmableItem(Items.DIAMOND_HELMET, EquipmentAssets.DIAMOND, TRIM_PREFIX_HELMET, false);
        this.generateTrimmableItem(Items.DIAMOND_CHESTPLATE, EquipmentAssets.DIAMOND, TRIM_PREFIX_CHESTPLATE, false);
        this.generateTrimmableItem(Items.DIAMOND_LEGGINGS, EquipmentAssets.DIAMOND, TRIM_PREFIX_LEGGINGS, false);
        this.generateTrimmableItem(Items.DIAMOND_BOOTS, EquipmentAssets.DIAMOND, TRIM_PREFIX_BOOTS, false);
        this.generateTrimmableItem(Items.GOLDEN_HELMET, EquipmentAssets.GOLD, TRIM_PREFIX_HELMET, false);
        this.generateTrimmableItem(Items.GOLDEN_CHESTPLATE, EquipmentAssets.GOLD, TRIM_PREFIX_CHESTPLATE, false);
        this.generateTrimmableItem(Items.GOLDEN_LEGGINGS, EquipmentAssets.GOLD, TRIM_PREFIX_LEGGINGS, false);
        this.generateTrimmableItem(Items.GOLDEN_BOOTS, EquipmentAssets.GOLD, TRIM_PREFIX_BOOTS, false);
        this.generateTrimmableItem(Items.NETHERITE_HELMET, EquipmentAssets.NETHERITE, TRIM_PREFIX_HELMET, false);
        this.generateTrimmableItem(Items.NETHERITE_CHESTPLATE, EquipmentAssets.NETHERITE, TRIM_PREFIX_CHESTPLATE, false);
        this.generateTrimmableItem(Items.NETHERITE_LEGGINGS, EquipmentAssets.NETHERITE, TRIM_PREFIX_LEGGINGS, false);
        this.generateTrimmableItem(Items.NETHERITE_BOOTS, EquipmentAssets.NETHERITE, TRIM_PREFIX_BOOTS, false);
        this.generateDyedItem(Items.LEATHER_HORSE_ARMOR, -6265536);
        this.generateFlatItem(Items.ANGLER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ARCHER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ARMS_UP_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BLADE_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BREWER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BURN_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.DANGER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.EXPLORER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.FLOW_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.FRIEND_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GUSTER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.HEART_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.HEARTBREAK_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.HOWL_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.MINER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.MOURNER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PLENTY_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PRIZE_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SCRAPE_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SHEAF_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SHELTER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SKULL_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SNORT_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.TRIAL_KEY, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.OMINOUS_TRIAL_KEY, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.OMINOUS_BOTTLE, ModelTemplates.FLAT_ITEM);
        this.generateItemWithTintedOverlay(Items.FIREWORK_STAR, new Firework());
        this.generateItemWithTintedOverlay(Items.FILLED_MAP, "_markings", new MapColor());
        this.generateBundleModels(Items.BUNDLE);
        this.generateBundleModels(Items.BLACK_BUNDLE);
        this.generateBundleModels(Items.WHITE_BUNDLE);
        this.generateBundleModels(Items.GRAY_BUNDLE);
        this.generateBundleModels(Items.LIGHT_GRAY_BUNDLE);
        this.generateBundleModels(Items.LIGHT_BLUE_BUNDLE);
        this.generateBundleModels(Items.BLUE_BUNDLE);
        this.generateBundleModels(Items.CYAN_BUNDLE);
        this.generateBundleModels(Items.YELLOW_BUNDLE);
        this.generateBundleModels(Items.RED_BUNDLE);
        this.generateBundleModels(Items.PURPLE_BUNDLE);
        this.generateBundleModels(Items.MAGENTA_BUNDLE);
        this.generateBundleModels(Items.PINK_BUNDLE);
        this.generateBundleModels(Items.GREEN_BUNDLE);
        this.generateBundleModels(Items.LIME_BUNDLE);
        this.generateBundleModels(Items.BROWN_BUNDLE);
        this.generateBundleModels(Items.ORANGE_BUNDLE);
        this.generateSpyglass(Items.SPYGLASS);
        this.generateTrident(Items.TRIDENT);
        this.generateWolfArmor(Items.WOLF_ARMOR);
        this.generateBow(Items.BOW);
        this.generateCrossbow(Items.CROSSBOW);
        this.generateElytra(Items.ELYTRA);
        this.generateBrush(Items.BRUSH);
        this.generateFishingRod(Items.FISHING_ROD);
        this.generateGoatHorn(Items.GOAT_HORN);
        this.generateShield(Items.SHIELD);
        this.generateTippedArrow(Items.TIPPED_ARROW);
        this.generatePotion(Items.POTION);
        this.generatePotion(Items.SPLASH_POTION);
        this.generatePotion(Items.LINGERING_POTION);
        this.generateFlatItem(Items.ARMADILLO_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ALLAY_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.AXOLOTL_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BAT_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BEE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BLAZE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BOGGED_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BREEZE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.CAT_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.CAMEL_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.CAVE_SPIDER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.CHICKEN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.COD_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.COW_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.CREEPER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.DOLPHIN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.DONKEY_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.DROWNED_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ELDER_GUARDIAN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ENDER_DRAGON_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ENDERMAN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ENDERMITE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.EVOKER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.FOX_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.FROG_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GHAST_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GLOW_SQUID_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GOAT_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.GUARDIAN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.HOGLIN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.HORSE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.HUSK_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.IRON_GOLEM_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.LLAMA_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.MAGMA_CUBE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.MOOSHROOM_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.MULE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.OCELOT_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PANDA_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PARROT_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PHANTOM_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PIG_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PIGLIN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PIGLIN_BRUTE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PILLAGER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.POLAR_BEAR_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PUFFERFISH_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.RABBIT_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.RAVAGER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SALMON_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SHEEP_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SHULKER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SILVERFISH_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SKELETON_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SKELETON_HORSE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SLIME_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SNIFFER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SNOW_GOLEM_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SPIDER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.SQUID_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.STRAY_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.STRIDER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.TADPOLE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.TRADER_LLAMA_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.TROPICAL_FISH_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.TURTLE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.VEX_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.VILLAGER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.VINDICATOR_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.WANDERING_TRADER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.WARDEN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.WITCH_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.WITHER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.WITHER_SKELETON_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.WOLF_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ZOGLIN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.CREAKING_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ZOMBIE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ZOMBIE_HORSE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ZOMBIE_VILLAGER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.ZOMBIFIED_PIGLIN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
        this.declareCustomModelItem(Items.AIR);
        this.declareCustomModelItem(Items.AMETHYST_CLUSTER);
        this.declareCustomModelItem(Items.SMALL_AMETHYST_BUD);
        this.declareCustomModelItem(Items.MEDIUM_AMETHYST_BUD);
        this.declareCustomModelItem(Items.LARGE_AMETHYST_BUD);
        this.declareCustomModelItem(Items.SMALL_DRIPLEAF);
        this.declareCustomModelItem(Items.BIG_DRIPLEAF);
        this.declareCustomModelItem(Items.HANGING_ROOTS);
        this.declareCustomModelItem(Items.POINTED_DRIPSTONE);
        this.declareCustomModelItem(Items.BONE);
        this.declareCustomModelItem(Items.COD);
        this.declareCustomModelItem(Items.FEATHER);
        this.declareCustomModelItem(Items.LEAD);
        // moved from validate to here, because it's just vanilla default blocks
        if (this.itemModelOutput instanceof ModelProvider.ItemInfoCollector collector)
            collector.generateDefaultBlockModels();
    }

    @OnlyIn(Dist.CLIENT)
    public record TrimMaterialData(MaterialAssetGroup assets, ResourceKey<TrimMaterial> materialKey) {
    }
}
