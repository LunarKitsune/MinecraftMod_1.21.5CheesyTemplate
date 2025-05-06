package net.minecraft.world.item.equipment.trim;

import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ProvidesTrimMaterial;

public class TrimMaterials {
    public static final ResourceKey<TrimMaterial> QUARTZ = registryKey("quartz");
    public static final ResourceKey<TrimMaterial> IRON = registryKey("iron");
    public static final ResourceKey<TrimMaterial> NETHERITE = registryKey("netherite");
    public static final ResourceKey<TrimMaterial> REDSTONE = registryKey("redstone");
    public static final ResourceKey<TrimMaterial> COPPER = registryKey("copper");
    public static final ResourceKey<TrimMaterial> GOLD = registryKey("gold");
    public static final ResourceKey<TrimMaterial> EMERALD = registryKey("emerald");
    public static final ResourceKey<TrimMaterial> DIAMOND = registryKey("diamond");
    public static final ResourceKey<TrimMaterial> LAPIS = registryKey("lapis");
    public static final ResourceKey<TrimMaterial> AMETHYST = registryKey("amethyst");
    public static final ResourceKey<TrimMaterial> RESIN = registryKey("resin");

    public static void bootstrap(BootstrapContext<TrimMaterial> pContext) {
        register(pContext, QUARTZ, Style.EMPTY.withColor(14931140), MaterialAssetGroup.QUARTZ);
        register(pContext, IRON, Style.EMPTY.withColor(15527148), MaterialAssetGroup.IRON);
        register(pContext, NETHERITE, Style.EMPTY.withColor(6445145), MaterialAssetGroup.NETHERITE);
        register(pContext, REDSTONE, Style.EMPTY.withColor(9901575), MaterialAssetGroup.REDSTONE);
        register(pContext, COPPER, Style.EMPTY.withColor(11823181), MaterialAssetGroup.COPPER);
        register(pContext, GOLD, Style.EMPTY.withColor(14594349), MaterialAssetGroup.GOLD);
        register(pContext, EMERALD, Style.EMPTY.withColor(1155126), MaterialAssetGroup.EMERALD);
        register(pContext, DIAMOND, Style.EMPTY.withColor(7269586), MaterialAssetGroup.DIAMOND);
        register(pContext, LAPIS, Style.EMPTY.withColor(4288151), MaterialAssetGroup.LAPIS);
        register(pContext, AMETHYST, Style.EMPTY.withColor(10116294), MaterialAssetGroup.AMETHYST);
        register(pContext, RESIN, Style.EMPTY.withColor(16545810), MaterialAssetGroup.RESIN);
    }

    public static Optional<Holder<TrimMaterial>> getFromIngredient(HolderLookup.Provider pRegistries, ItemStack pIngredient) {
        ProvidesTrimMaterial providestrimmaterial = pIngredient.get(DataComponents.PROVIDES_TRIM_MATERIAL);
        return providestrimmaterial != null ? providestrimmaterial.unwrap(pRegistries) : Optional.empty();
    }

    private static void register(BootstrapContext<TrimMaterial> pContext, ResourceKey<TrimMaterial> pKey, Style pStyle, MaterialAssetGroup pAssets) {
        Component component = Component.translatable(Util.makeDescriptionId("trim_material", pKey.location())).withStyle(pStyle);
        pContext.register(pKey, new TrimMaterial(pAssets, component));
    }

    private static ResourceKey<TrimMaterial> registryKey(String pName) {
        return ResourceKey.create(Registries.TRIM_MATERIAL, ResourceLocation.withDefaultNamespace(pName));
    }
}