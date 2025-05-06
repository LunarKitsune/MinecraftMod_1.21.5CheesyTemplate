package net.minecraft.client.data.models.model;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TextureMapping {
    private final Map<TextureSlot, ResourceLocation> slots = Maps.newHashMap();
    private final Set<TextureSlot> forcedSlots = Sets.newHashSet();

    public TextureMapping put(TextureSlot pSlot, ResourceLocation pTexture) {
        this.slots.put(pSlot, pTexture);
        return this;
    }

    public TextureMapping putForced(TextureSlot pSlot, ResourceLocation pTexture) {
        this.slots.put(pSlot, pTexture);
        this.forcedSlots.add(pSlot);
        return this;
    }

    public Stream<TextureSlot> getForced() {
        return this.forcedSlots.stream();
    }

    public TextureMapping copySlot(TextureSlot pSource, TextureSlot pDestination) {
        this.slots.put(pDestination, this.slots.get(pSource));
        return this;
    }

    public TextureMapping copyForced(TextureSlot pSource, TextureSlot pDestination) {
        this.slots.put(pDestination, this.slots.get(pSource));
        this.forcedSlots.add(pDestination);
        return this;
    }

    public ResourceLocation get(TextureSlot pSlot) {
        for (TextureSlot textureslot = pSlot; textureslot != null; textureslot = textureslot.getParent()) {
            ResourceLocation resourcelocation = this.slots.get(textureslot);
            if (resourcelocation != null) {
                return resourcelocation;
            }
        }

        throw new IllegalStateException("Can't find texture for slot " + pSlot);
    }

    public TextureMapping copyAndUpdate(TextureSlot pSlot, ResourceLocation pTexture) {
        TextureMapping texturemapping = new TextureMapping();
        texturemapping.slots.putAll(this.slots);
        texturemapping.forcedSlots.addAll(this.forcedSlots);
        texturemapping.put(pSlot, pTexture);
        return texturemapping;
    }

    public static TextureMapping cube(Block pBlock) {
        ResourceLocation resourcelocation = getBlockTexture(pBlock);
        return cube(resourcelocation);
    }

    public static TextureMapping defaultTexture(Block pBlock) {
        ResourceLocation resourcelocation = getBlockTexture(pBlock);
        return defaultTexture(resourcelocation);
    }

    public static TextureMapping defaultTexture(ResourceLocation pTexture) {
        return new TextureMapping().put(TextureSlot.TEXTURE, pTexture);
    }

    public static TextureMapping cube(ResourceLocation pTexture) {
        return new TextureMapping().put(TextureSlot.ALL, pTexture);
    }

    public static TextureMapping cross(Block pBlock) {
        return singleSlot(TextureSlot.CROSS, getBlockTexture(pBlock));
    }

    public static TextureMapping side(Block pBlock) {
        return singleSlot(TextureSlot.SIDE, getBlockTexture(pBlock));
    }

    public static TextureMapping crossEmissive(Block pBlock) {
        return new TextureMapping().put(TextureSlot.CROSS, getBlockTexture(pBlock)).put(TextureSlot.CROSS_EMISSIVE, getBlockTexture(pBlock, "_emissive"));
    }

    public static TextureMapping cross(ResourceLocation pTexture) {
        return singleSlot(TextureSlot.CROSS, pTexture);
    }

    public static TextureMapping plant(Block pBlock) {
        return singleSlot(TextureSlot.PLANT, getBlockTexture(pBlock));
    }

    public static TextureMapping plantEmissive(Block pBlock) {
        return new TextureMapping().put(TextureSlot.PLANT, getBlockTexture(pBlock)).put(TextureSlot.CROSS_EMISSIVE, getBlockTexture(pBlock, "_emissive"));
    }

    public static TextureMapping plant(ResourceLocation pTexture) {
        return singleSlot(TextureSlot.PLANT, pTexture);
    }

    public static TextureMapping rail(Block pBlock) {
        return singleSlot(TextureSlot.RAIL, getBlockTexture(pBlock));
    }

    public static TextureMapping rail(ResourceLocation pTexture) {
        return singleSlot(TextureSlot.RAIL, pTexture);
    }

    public static TextureMapping wool(Block pBlock) {
        return singleSlot(TextureSlot.WOOL, getBlockTexture(pBlock));
    }

    public static TextureMapping flowerbed(Block pBlock) {
        return new TextureMapping().put(TextureSlot.FLOWERBED, getBlockTexture(pBlock)).put(TextureSlot.STEM, getBlockTexture(pBlock, "_stem"));
    }

    public static TextureMapping wool(ResourceLocation pTexture) {
        return singleSlot(TextureSlot.WOOL, pTexture);
    }

    public static TextureMapping stem(Block pBlock) {
        return singleSlot(TextureSlot.STEM, getBlockTexture(pBlock));
    }

    public static TextureMapping attachedStem(Block pStemBlock, Block pUpperStemBlock) {
        return new TextureMapping().put(TextureSlot.STEM, getBlockTexture(pStemBlock)).put(TextureSlot.UPPER_STEM, getBlockTexture(pUpperStemBlock));
    }

    public static TextureMapping pattern(Block pBlock) {
        return singleSlot(TextureSlot.PATTERN, getBlockTexture(pBlock));
    }

    public static TextureMapping fan(Block pBlock) {
        return singleSlot(TextureSlot.FAN, getBlockTexture(pBlock));
    }

    public static TextureMapping crop(ResourceLocation pBlock) {
        return singleSlot(TextureSlot.CROP, pBlock);
    }

    public static TextureMapping pane(Block pBlock, Block pEdgeBlock) {
        return new TextureMapping().put(TextureSlot.PANE, getBlockTexture(pBlock)).put(TextureSlot.EDGE, getBlockTexture(pEdgeBlock, "_top"));
    }

    public static TextureMapping singleSlot(TextureSlot pSlot, ResourceLocation pTexture) {
        return new TextureMapping().put(pSlot, pTexture);
    }

    public static TextureMapping column(Block pBlock) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(pBlock, "_side"))
            .put(TextureSlot.END, getBlockTexture(pBlock, "_top"));
    }

    public static TextureMapping cubeTop(Block pBlock) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(pBlock, "_side"))
            .put(TextureSlot.TOP, getBlockTexture(pBlock, "_top"));
    }

    public static TextureMapping pottedAzalea(Block pBlock) {
        return new TextureMapping()
            .put(TextureSlot.PLANT, getBlockTexture(pBlock, "_plant"))
            .put(TextureSlot.SIDE, getBlockTexture(pBlock, "_side"))
            .put(TextureSlot.TOP, getBlockTexture(pBlock, "_top"));
    }

    public static TextureMapping logColumn(Block pBlock) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(pBlock))
            .put(TextureSlot.END, getBlockTexture(pBlock, "_top"))
            .put(TextureSlot.PARTICLE, getBlockTexture(pBlock));
    }

    public static TextureMapping column(ResourceLocation pSide, ResourceLocation pEnd) {
        return new TextureMapping().put(TextureSlot.SIDE, pSide).put(TextureSlot.END, pEnd);
    }

    public static TextureMapping fence(Block pBlock) {
        return new TextureMapping()
            .put(TextureSlot.TEXTURE, getBlockTexture(pBlock))
            .put(TextureSlot.SIDE, getBlockTexture(pBlock, "_side"))
            .put(TextureSlot.TOP, getBlockTexture(pBlock, "_top"));
    }

    public static TextureMapping customParticle(Block pBlock) {
        return new TextureMapping().put(TextureSlot.TEXTURE, getBlockTexture(pBlock)).put(TextureSlot.PARTICLE, getBlockTexture(pBlock, "_particle"));
    }

    public static TextureMapping cubeBottomTop(Block pBlock) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(pBlock, "_side"))
            .put(TextureSlot.TOP, getBlockTexture(pBlock, "_top"))
            .put(TextureSlot.BOTTOM, getBlockTexture(pBlock, "_bottom"));
    }

    public static TextureMapping cubeBottomTopWithWall(Block pBlock) {
        ResourceLocation resourcelocation = getBlockTexture(pBlock);
        return new TextureMapping()
            .put(TextureSlot.WALL, resourcelocation)
            .put(TextureSlot.SIDE, resourcelocation)
            .put(TextureSlot.TOP, getBlockTexture(pBlock, "_top"))
            .put(TextureSlot.BOTTOM, getBlockTexture(pBlock, "_bottom"));
    }

    public static TextureMapping columnWithWall(Block pBlock) {
        ResourceLocation resourcelocation = getBlockTexture(pBlock);
        return new TextureMapping()
            .put(TextureSlot.TEXTURE, resourcelocation)
            .put(TextureSlot.WALL, resourcelocation)
            .put(TextureSlot.SIDE, resourcelocation)
            .put(TextureSlot.END, getBlockTexture(pBlock, "_top"));
    }

    public static TextureMapping door(ResourceLocation pTop, ResourceLocation pBottom) {
        return new TextureMapping().put(TextureSlot.TOP, pTop).put(TextureSlot.BOTTOM, pBottom);
    }

    public static TextureMapping door(Block pBlock) {
        return new TextureMapping()
            .put(TextureSlot.TOP, getBlockTexture(pBlock, "_top"))
            .put(TextureSlot.BOTTOM, getBlockTexture(pBlock, "_bottom"));
    }

    public static TextureMapping particle(Block pBlock) {
        return new TextureMapping().put(TextureSlot.PARTICLE, getBlockTexture(pBlock));
    }

    public static TextureMapping particle(ResourceLocation pTexture) {
        return new TextureMapping().put(TextureSlot.PARTICLE, pTexture);
    }

    public static TextureMapping fire0(Block pBlock) {
        return new TextureMapping().put(TextureSlot.FIRE, getBlockTexture(pBlock, "_0"));
    }

    public static TextureMapping fire1(Block pBlock) {
        return new TextureMapping().put(TextureSlot.FIRE, getBlockTexture(pBlock, "_1"));
    }

    public static TextureMapping lantern(Block pBlock) {
        return new TextureMapping().put(TextureSlot.LANTERN, getBlockTexture(pBlock));
    }

    public static TextureMapping torch(Block pBlock) {
        return new TextureMapping().put(TextureSlot.TORCH, getBlockTexture(pBlock));
    }

    public static TextureMapping torch(ResourceLocation pTexture) {
        return new TextureMapping().put(TextureSlot.TORCH, pTexture);
    }

    public static TextureMapping trialSpawner(Block pBlock, String pSideSuffix, String pTopSuffix) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(pBlock, pSideSuffix))
            .put(TextureSlot.TOP, getBlockTexture(pBlock, pTopSuffix))
            .put(TextureSlot.BOTTOM, getBlockTexture(pBlock, "_bottom"));
    }

    public static TextureMapping vault(Block pBlock, String pFrontSuffix, String pSideSuffix, String pTopSuffix, String pBottomSuffix) {
        return new TextureMapping()
            .put(TextureSlot.FRONT, getBlockTexture(pBlock, pFrontSuffix))
            .put(TextureSlot.SIDE, getBlockTexture(pBlock, pSideSuffix))
            .put(TextureSlot.TOP, getBlockTexture(pBlock, pTopSuffix))
            .put(TextureSlot.BOTTOM, getBlockTexture(pBlock, pBottomSuffix));
    }

    public static TextureMapping particleFromItem(Item pItem) {
        return new TextureMapping().put(TextureSlot.PARTICLE, getItemTexture(pItem));
    }

    public static TextureMapping commandBlock(Block pBlock) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(pBlock, "_side"))
            .put(TextureSlot.FRONT, getBlockTexture(pBlock, "_front"))
            .put(TextureSlot.BACK, getBlockTexture(pBlock, "_back"));
    }

    public static TextureMapping orientableCube(Block pBlock) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(pBlock, "_side"))
            .put(TextureSlot.FRONT, getBlockTexture(pBlock, "_front"))
            .put(TextureSlot.TOP, getBlockTexture(pBlock, "_top"))
            .put(TextureSlot.BOTTOM, getBlockTexture(pBlock, "_bottom"));
    }

    public static TextureMapping orientableCubeOnlyTop(Block pBlock) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(pBlock, "_side"))
            .put(TextureSlot.FRONT, getBlockTexture(pBlock, "_front"))
            .put(TextureSlot.TOP, getBlockTexture(pBlock, "_top"));
    }

    public static TextureMapping orientableCubeSameEnds(Block pBlock) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(pBlock, "_side"))
            .put(TextureSlot.FRONT, getBlockTexture(pBlock, "_front"))
            .put(TextureSlot.END, getBlockTexture(pBlock, "_end"));
    }

    public static TextureMapping top(Block pBlock) {
        return new TextureMapping().put(TextureSlot.TOP, getBlockTexture(pBlock, "_top"));
    }

    public static TextureMapping craftingTable(Block pBlock, Block pBottom) {
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(pBlock, "_front"))
            .put(TextureSlot.DOWN, getBlockTexture(pBottom))
            .put(TextureSlot.UP, getBlockTexture(pBlock, "_top"))
            .put(TextureSlot.NORTH, getBlockTexture(pBlock, "_front"))
            .put(TextureSlot.EAST, getBlockTexture(pBlock, "_side"))
            .put(TextureSlot.SOUTH, getBlockTexture(pBlock, "_side"))
            .put(TextureSlot.WEST, getBlockTexture(pBlock, "_front"));
    }

    public static TextureMapping fletchingTable(Block pBlock, Block pBottom) {
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(pBlock, "_front"))
            .put(TextureSlot.DOWN, getBlockTexture(pBottom))
            .put(TextureSlot.UP, getBlockTexture(pBlock, "_top"))
            .put(TextureSlot.NORTH, getBlockTexture(pBlock, "_front"))
            .put(TextureSlot.SOUTH, getBlockTexture(pBlock, "_front"))
            .put(TextureSlot.EAST, getBlockTexture(pBlock, "_side"))
            .put(TextureSlot.WEST, getBlockTexture(pBlock, "_side"));
    }

    public static TextureMapping snifferEgg(String pName) {
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(Blocks.SNIFFER_EGG, pName + "_north"))
            .put(TextureSlot.BOTTOM, getBlockTexture(Blocks.SNIFFER_EGG, pName + "_bottom"))
            .put(TextureSlot.TOP, getBlockTexture(Blocks.SNIFFER_EGG, pName + "_top"))
            .put(TextureSlot.NORTH, getBlockTexture(Blocks.SNIFFER_EGG, pName + "_north"))
            .put(TextureSlot.SOUTH, getBlockTexture(Blocks.SNIFFER_EGG, pName + "_south"))
            .put(TextureSlot.EAST, getBlockTexture(Blocks.SNIFFER_EGG, pName + "_east"))
            .put(TextureSlot.WEST, getBlockTexture(Blocks.SNIFFER_EGG, pName + "_west"));
    }

    public static TextureMapping campfire(Block pBlock) {
        return new TextureMapping()
            .put(TextureSlot.LIT_LOG, getBlockTexture(pBlock, "_log_lit"))
            .put(TextureSlot.FIRE, getBlockTexture(pBlock, "_fire"));
    }

    public static TextureMapping candleCake(Block pBlock, boolean pLit) {
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(Blocks.CAKE, "_side"))
            .put(TextureSlot.BOTTOM, getBlockTexture(Blocks.CAKE, "_bottom"))
            .put(TextureSlot.TOP, getBlockTexture(Blocks.CAKE, "_top"))
            .put(TextureSlot.SIDE, getBlockTexture(Blocks.CAKE, "_side"))
            .put(TextureSlot.CANDLE, getBlockTexture(pBlock, pLit ? "_lit" : ""));
    }

    public static TextureMapping cauldron(ResourceLocation pTexture) {
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(Blocks.CAULDRON, "_side"))
            .put(TextureSlot.SIDE, getBlockTexture(Blocks.CAULDRON, "_side"))
            .put(TextureSlot.TOP, getBlockTexture(Blocks.CAULDRON, "_top"))
            .put(TextureSlot.BOTTOM, getBlockTexture(Blocks.CAULDRON, "_bottom"))
            .put(TextureSlot.INSIDE, getBlockTexture(Blocks.CAULDRON, "_inner"))
            .put(TextureSlot.CONTENT, pTexture);
    }

    public static TextureMapping sculkShrieker(boolean pCanSummon) {
        String s = pCanSummon ? "_can_summon" : "";
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(Blocks.SCULK_SHRIEKER, "_bottom"))
            .put(TextureSlot.SIDE, getBlockTexture(Blocks.SCULK_SHRIEKER, "_side"))
            .put(TextureSlot.TOP, getBlockTexture(Blocks.SCULK_SHRIEKER, "_top"))
            .put(TextureSlot.INNER_TOP, getBlockTexture(Blocks.SCULK_SHRIEKER, s + "_inner_top"))
            .put(TextureSlot.BOTTOM, getBlockTexture(Blocks.SCULK_SHRIEKER, "_bottom"));
    }

    public static TextureMapping layer0(Item pItem) {
        return new TextureMapping().put(TextureSlot.LAYER0, getItemTexture(pItem));
    }

    public static TextureMapping layer0(Block pBlock) {
        return new TextureMapping().put(TextureSlot.LAYER0, getBlockTexture(pBlock));
    }

    public static TextureMapping layer0(ResourceLocation pTexture) {
        return new TextureMapping().put(TextureSlot.LAYER0, pTexture);
    }

    public static TextureMapping layered(ResourceLocation pLayer0, ResourceLocation pLayer1) {
        return new TextureMapping().put(TextureSlot.LAYER0, pLayer0).put(TextureSlot.LAYER1, pLayer1);
    }

    public static TextureMapping layered(ResourceLocation pLayer0, ResourceLocation pLayer1, ResourceLocation pLayer2) {
        return new TextureMapping()
            .put(TextureSlot.LAYER0, pLayer0)
            .put(TextureSlot.LAYER1, pLayer1)
            .put(TextureSlot.LAYER2, pLayer2);
    }

    public static ResourceLocation getBlockTexture(Block pBlock) {
        ResourceLocation resourcelocation = BuiltInRegistries.BLOCK.getKey(pBlock);
        return resourcelocation.withPrefix("block/");
    }

    public static ResourceLocation getBlockTexture(Block pBlock, String pSuffix) {
        ResourceLocation resourcelocation = BuiltInRegistries.BLOCK.getKey(pBlock);
        return resourcelocation.withPath(p_377089_ -> "block/" + p_377089_ + pSuffix);
    }

    public static ResourceLocation getItemTexture(Item pItem) {
        ResourceLocation resourcelocation = BuiltInRegistries.ITEM.getKey(pItem);
        return resourcelocation.withPrefix("item/");
    }

    public static ResourceLocation getItemTexture(Item pItem, String pSuffix) {
        ResourceLocation resourcelocation = BuiltInRegistries.ITEM.getKey(pItem);
        return resourcelocation.withPath(p_377151_ -> "item/" + p_377151_ + pSuffix);
    }
}