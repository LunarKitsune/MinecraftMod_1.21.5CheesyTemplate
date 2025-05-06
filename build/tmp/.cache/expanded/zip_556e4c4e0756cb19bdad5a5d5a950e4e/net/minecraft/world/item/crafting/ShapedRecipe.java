package net.minecraft.world.item.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

public class ShapedRecipe implements CraftingRecipe, net.minecraftforge.common.crafting.IShapedRecipe<CraftingInput> {
    static int MAX_WIDTH = 3;
    static int MAX_HEIGHT = 3;
    /**
     * Expand the max width and height allowed in the deserializer.
     * This should be called by modders who add custom crafting tables that are larger than the vanilla 3x3.
     * @param width your max recipe width
     * @param height your max recipe height
     */
    public static void setCraftingSize(int width, int height) {
        if (MAX_WIDTH < width) MAX_WIDTH = width;
        if (MAX_HEIGHT < height) MAX_HEIGHT = height;
    }
    final ShapedRecipePattern pattern;
    final ItemStack result;
    final String group;
    final CraftingBookCategory category;
    final boolean showNotification;
    @Nullable
    private PlacementInfo placementInfo;

    public ShapedRecipe(String pGroup, CraftingBookCategory pCategory, ShapedRecipePattern pPattern, ItemStack pResult, boolean pShowNotification) {
        this.group = pGroup;
        this.category = pCategory;
        this.pattern = pPattern;
        this.result = pResult;
        this.showNotification = pShowNotification;
    }

    public ShapedRecipe(String pGroup, CraftingBookCategory pCategory, ShapedRecipePattern pPattern, ItemStack pResult) {
        this(pGroup, pCategory, pPattern, pResult, true);
    }

    @Override
    public RecipeSerializer<? extends ShapedRecipe> getSerializer() {
        return RecipeSerializer.SHAPED_RECIPE;
    }

    @Override
    public String group() {
        return this.group;
    }

    @Override
    public CraftingBookCategory category() {
        return this.category;
    }

    @VisibleForTesting
    public List<Optional<Ingredient>> getIngredients() {
        return this.pattern.ingredients();
    }

    @Override
    public PlacementInfo placementInfo() {
        if (this.placementInfo == null) {
            this.placementInfo = PlacementInfo.createFromOptionals(this.pattern.ingredients());
        }

        return this.placementInfo;
    }

    @Override
    public int getRecipeWidth() {
        return getWidth();
    }

    @Override
    public int getRecipeHeight() {
        return getHeight();
    }

    @Override
    public boolean showNotification() {
        return this.showNotification;
    }

    public boolean matches(CraftingInput p_345171_, Level p_44177_) {
        return this.pattern.matches(p_345171_);
    }

    public ItemStack assemble(CraftingInput p_345083_, HolderLookup.Provider p_333236_) {
        return this.result.copy();
    }

    public int getWidth() {
        return this.pattern.width();
    }

    public int getHeight() {
        return this.pattern.height();
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(
            new ShapedCraftingRecipeDisplay(
                this.pattern.width(),
                this.pattern.height(),
                this.pattern.ingredients().stream().map(p_359851_ -> p_359851_.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE)).toList(),
                new SlotDisplay.ItemStackSlotDisplay(this.result),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
            )
        );
    }

    public static class Serializer implements RecipeSerializer<ShapedRecipe> {
        public static final MapCodec<ShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(
            p_327208_ -> p_327208_.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(p_309251_ -> p_309251_.group),
                    CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(p_309253_ -> p_309253_.category),
                    ShapedRecipePattern.MAP_CODEC.forGetter(p_309254_ -> p_309254_.pattern),
                    ItemStack.STRICT_CODEC.fieldOf("result").forGetter(p_309252_ -> p_309252_.result),
                    Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(p_309255_ -> p_309255_.showNotification)
                )
                .apply(p_327208_, ShapedRecipe::new)
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, ShapedRecipe> STREAM_CODEC = StreamCodec.of(
            ShapedRecipe.Serializer::toNetwork, ShapedRecipe.Serializer::fromNetwork
        );

        @Override
        public MapCodec<ShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ShapedRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static ShapedRecipe fromNetwork(RegistryFriendlyByteBuf pBuffer) {
            String s = pBuffer.readUtf();
            CraftingBookCategory craftingbookcategory = pBuffer.readEnum(CraftingBookCategory.class);
            ShapedRecipePattern shapedrecipepattern = ShapedRecipePattern.STREAM_CODEC.decode(pBuffer);
            ItemStack itemstack = ItemStack.STREAM_CODEC.decode(pBuffer);
            boolean flag = pBuffer.readBoolean();
            return new ShapedRecipe(s, craftingbookcategory, shapedrecipepattern, itemstack, flag);
        }

        private static void toNetwork(RegistryFriendlyByteBuf pBuffer, ShapedRecipe pRecipe) {
            pBuffer.writeUtf(pRecipe.group);
            pBuffer.writeEnum(pRecipe.category);
            ShapedRecipePattern.STREAM_CODEC.encode(pBuffer, pRecipe.pattern);
            ItemStack.STREAM_CODEC.encode(pBuffer, pRecipe.result);
            pBuffer.writeBoolean(pRecipe.showNotification);
        }
    }
}
