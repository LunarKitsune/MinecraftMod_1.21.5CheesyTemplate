package net.minecraft.client.data.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EquipmentAssetProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public EquipmentAssetProvider(PackOutput pOutput) {
        this.pathProvider = pOutput.createPathProvider(PackOutput.Target.RESOURCE_PACK, "equipment");
    }

    private static void bootstrap(BiConsumer<ResourceKey<EquipmentAsset>, EquipmentClientInfo> pOutput) {
        pOutput.accept(
            EquipmentAssets.LEATHER,
            EquipmentClientInfo.builder()
                .addHumanoidLayers(ResourceLocation.withDefaultNamespace("leather"), true)
                .addHumanoidLayers(ResourceLocation.withDefaultNamespace("leather_overlay"), false)
                .addLayers(EquipmentClientInfo.LayerType.HORSE_BODY, EquipmentClientInfo.Layer.leatherDyeable(ResourceLocation.withDefaultNamespace("leather"), true))
                .build()
        );
        pOutput.accept(EquipmentAssets.CHAINMAIL, onlyHumanoid("chainmail"));
        pOutput.accept(EquipmentAssets.IRON, humanoidAndHorse("iron"));
        pOutput.accept(EquipmentAssets.GOLD, humanoidAndHorse("gold"));
        pOutput.accept(EquipmentAssets.DIAMOND, humanoidAndHorse("diamond"));
        pOutput.accept(EquipmentAssets.TURTLE_SCUTE, EquipmentClientInfo.builder().addMainHumanoidLayer(ResourceLocation.withDefaultNamespace("turtle_scute"), false).build());
        pOutput.accept(EquipmentAssets.NETHERITE, onlyHumanoid("netherite"));
        pOutput.accept(
            EquipmentAssets.ARMADILLO_SCUTE,
            EquipmentClientInfo.builder()
                .addLayers(EquipmentClientInfo.LayerType.WOLF_BODY, EquipmentClientInfo.Layer.onlyIfDyed(ResourceLocation.withDefaultNamespace("armadillo_scute"), false))
                .addLayers(
                    EquipmentClientInfo.LayerType.WOLF_BODY, EquipmentClientInfo.Layer.onlyIfDyed(ResourceLocation.withDefaultNamespace("armadillo_scute_overlay"), true)
                )
                .build()
        );
        pOutput.accept(
            EquipmentAssets.ELYTRA,
            EquipmentClientInfo.builder()
                .addLayers(EquipmentClientInfo.LayerType.WINGS, new EquipmentClientInfo.Layer(ResourceLocation.withDefaultNamespace("elytra"), Optional.empty(), true))
                .build()
        );
        EquipmentClientInfo.Layer equipmentclientinfo$layer = new EquipmentClientInfo.Layer(ResourceLocation.withDefaultNamespace("saddle"));
        pOutput.accept(
            EquipmentAssets.SADDLE,
            EquipmentClientInfo.builder()
                .addLayers(EquipmentClientInfo.LayerType.PIG_SADDLE, equipmentclientinfo$layer)
                .addLayers(EquipmentClientInfo.LayerType.STRIDER_SADDLE, equipmentclientinfo$layer)
                .addLayers(EquipmentClientInfo.LayerType.CAMEL_SADDLE, equipmentclientinfo$layer)
                .addLayers(EquipmentClientInfo.LayerType.HORSE_SADDLE, equipmentclientinfo$layer)
                .addLayers(EquipmentClientInfo.LayerType.DONKEY_SADDLE, equipmentclientinfo$layer)
                .addLayers(EquipmentClientInfo.LayerType.MULE_SADDLE, equipmentclientinfo$layer)
                .addLayers(EquipmentClientInfo.LayerType.SKELETON_HORSE_SADDLE, equipmentclientinfo$layer)
                .addLayers(EquipmentClientInfo.LayerType.ZOMBIE_HORSE_SADDLE, equipmentclientinfo$layer)
                .build()
        );

        for (Entry<DyeColor, ResourceKey<EquipmentAsset>> entry : EquipmentAssets.CARPETS.entrySet()) {
            DyeColor dyecolor = entry.getKey();
            ResourceKey<EquipmentAsset> resourcekey = entry.getValue();
            pOutput.accept(
                resourcekey,
                EquipmentClientInfo.builder()
                    .addLayers(EquipmentClientInfo.LayerType.LLAMA_BODY, new EquipmentClientInfo.Layer(ResourceLocation.withDefaultNamespace(dyecolor.getSerializedName())))
                    .build()
            );
        }

        pOutput.accept(
            EquipmentAssets.TRADER_LLAMA,
            EquipmentClientInfo.builder()
                .addLayers(EquipmentClientInfo.LayerType.LLAMA_BODY, new EquipmentClientInfo.Layer(ResourceLocation.withDefaultNamespace("trader_llama")))
                .build()
        );
    }

    private static EquipmentClientInfo onlyHumanoid(String pName) {
        return EquipmentClientInfo.builder().addHumanoidLayers(ResourceLocation.withDefaultNamespace(pName)).build();
    }

    private static EquipmentClientInfo humanoidAndHorse(String pName) {
        return EquipmentClientInfo.builder()
            .addHumanoidLayers(ResourceLocation.withDefaultNamespace(pName))
            .addLayers(EquipmentClientInfo.LayerType.HORSE_BODY, EquipmentClientInfo.Layer.leatherDyeable(ResourceLocation.withDefaultNamespace(pName), false))
            .build();
    }

    @Override
    public CompletableFuture<?> run(CachedOutput p_376319_) {
        Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> map = new HashMap<>();
        bootstrap((p_376477_, p_377690_) -> {
            if (map.putIfAbsent(p_376477_, p_377690_) != null) {
                throw new IllegalStateException("Tried to register equipment asset twice for id: " + p_376477_);
            }
        });
        return DataProvider.saveAll(p_376319_, EquipmentClientInfo.CODEC, this.pathProvider::json, map);
    }

    @Override
    public String getName() {
        return "Equipment Asset Definitions";
    }
}