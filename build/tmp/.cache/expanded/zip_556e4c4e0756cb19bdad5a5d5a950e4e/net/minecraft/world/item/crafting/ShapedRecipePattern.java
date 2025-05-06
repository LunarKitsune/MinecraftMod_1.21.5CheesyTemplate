package net.minecraft.world.item.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.chars.CharArraySet;
import it.unimi.dsi.fastutil.chars.CharSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

public final class ShapedRecipePattern {
    private static final int MAX_SIZE = 3;
    public static final char EMPTY_SLOT = ' ';
    public static final MapCodec<ShapedRecipePattern> MAP_CODEC = ShapedRecipePattern.Data.MAP_CODEC
        .flatXmap(
            ShapedRecipePattern::unpack,
            p_341595_ -> p_341595_.data.map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Cannot encode unpacked recipe"))
        );
    public static final StreamCodec<RegistryFriendlyByteBuf, ShapedRecipePattern> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        p_359853_ -> p_359853_.width,
        ByteBufCodecs.VAR_INT,
        p_359854_ -> p_359854_.height,
        Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),
        p_359852_ -> p_359852_.ingredients,
        ShapedRecipePattern::createFromNetwork
    );
    private final int width;
    private final int height;
    private final List<Optional<Ingredient>> ingredients;
    private final Optional<ShapedRecipePattern.Data> data;
    private final int ingredientCount;
    private final boolean symmetrical;

    public ShapedRecipePattern(int pWidth, int pHeight, List<Optional<Ingredient>> pIngredients, Optional<ShapedRecipePattern.Data> pData) {
        this.width = pWidth;
        this.height = pHeight;
        this.ingredients = pIngredients;
        this.data = pData;
        this.ingredientCount = (int)pIngredients.stream().flatMap(Optional::stream).count();
        this.symmetrical = Util.isSymmetrical(pWidth, pHeight, pIngredients);
    }

    private static ShapedRecipePattern createFromNetwork(Integer pWidth, Integer pHeight, List<Optional<Ingredient>> pIngredients) {
        return new ShapedRecipePattern(pWidth, pHeight, pIngredients, Optional.empty());
    }

    public static ShapedRecipePattern of(Map<Character, Ingredient> pKey, String... pPattern) {
        return of(pKey, List.of(pPattern));
    }

    public static ShapedRecipePattern of(Map<Character, Ingredient> pKey, List<String> pPattern) {
        ShapedRecipePattern.Data shapedrecipepattern$data = new ShapedRecipePattern.Data(pKey, pPattern);
        return unpack(shapedrecipepattern$data).getOrThrow();
    }

    private static DataResult<ShapedRecipePattern> unpack(ShapedRecipePattern.Data pData) {
        String[] astring = shrink(pData.pattern);
        int i = astring[0].length();
        int j = astring.length;
        List<Optional<Ingredient>> list = new ArrayList<>(i * j);
        CharSet charset = new CharArraySet(pData.key.keySet());

        for (String s : astring) {
            for (int k = 0; k < s.length(); k++) {
                char c0 = s.charAt(k);
                Optional<Ingredient> optional;
                if (c0 == ' ') {
                    optional = Optional.empty();
                } else {
                    Ingredient ingredient = pData.key.get(c0);
                    if (ingredient == null) {
                        return DataResult.error(() -> "Pattern references symbol '" + c0 + "' but it's not defined in the key");
                    }

                    optional = Optional.of(ingredient);
                }

                charset.remove(c0);
                list.add(optional);
            }
        }

        return !charset.isEmpty()
            ? DataResult.error(() -> "Key defines symbols that aren't used in pattern: " + charset)
            : DataResult.success(new ShapedRecipePattern(i, j, list, Optional.of(pData)));
    }

    @VisibleForTesting
    static String[] shrink(List<String> pPattern) {
        int i = Integer.MAX_VALUE;
        int j = 0;
        int k = 0;
        int l = 0;

        for (int i1 = 0; i1 < pPattern.size(); i1++) {
            String s = pPattern.get(i1);
            i = Math.min(i, firstNonEmpty(s));
            int j1 = lastNonEmpty(s);
            j = Math.max(j, j1);
            if (j1 < 0) {
                if (k == i1) {
                    k++;
                }

                l++;
            } else {
                l = 0;
            }
        }

        if (pPattern.size() == l) {
            return new String[0];
        } else {
            String[] astring = new String[pPattern.size() - l - k];

            for (int k1 = 0; k1 < astring.length; k1++) {
                astring[k1] = pPattern.get(k1 + k).substring(i, j + 1);
            }

            return astring;
        }
    }

    private static int firstNonEmpty(String pRow) {
        int i = 0;

        while (i < pRow.length() && pRow.charAt(i) == ' ') {
            i++;
        }

        return i;
    }

    private static int lastNonEmpty(String pRow) {
        int i = pRow.length() - 1;

        while (i >= 0 && pRow.charAt(i) == ' ') {
            i--;
        }

        return i;
    }

    public boolean matches(CraftingInput pInput) {
        if (pInput.ingredientCount() != this.ingredientCount) {
            return false;
        } else {
            if (pInput.width() == this.width && pInput.height() == this.height) {
                if (!this.symmetrical && this.matches(pInput, true)) {
                    return true;
                }

                if (this.matches(pInput, false)) {
                    return true;
                }
            }

            return false;
        }
    }

    private boolean matches(CraftingInput pInput, boolean pSymmetrical) {
        for (int i = 0; i < this.height; i++) {
            for (int j = 0; j < this.width; j++) {
                Optional<Ingredient> optional;
                if (pSymmetrical) {
                    optional = this.ingredients.get(this.width - j - 1 + i * this.width);
                } else {
                    optional = this.ingredients.get(j + i * this.width);
                }

                ItemStack itemstack = pInput.getItem(j, i);
                if (!Ingredient.testOptionalIngredient(optional, itemstack)) {
                    return false;
                }
            }
        }

        return true;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public List<Optional<Ingredient>> ingredients() {
        return this.ingredients;
    }

    public Optional<ShapedRecipePattern.Data> data() {
        return this.data;
    }

    public record Data(Map<Character, Ingredient> key, List<String> pattern) {
        private static final Codec<List<String>> PATTERN_CODEC = Codec.STRING.listOf().comapFlatMap(p_311191_ -> {
            if (p_311191_.size() > ShapedRecipe.MAX_HEIGHT) {
                return DataResult.error(() -> "Invalid pattern: too many rows, " + ShapedRecipe.MAX_HEIGHT + " is maximum");
            } else if (p_311191_.isEmpty()) {
                return DataResult.error(() -> "Invalid pattern: empty pattern not allowed");
            } else {
                int i = p_311191_.getFirst().length();

                for (String s : p_311191_) {
                    if (s.length() > ShapedRecipe.MAX_HEIGHT) {
                        return DataResult.error(() -> "Invalid pattern: too many columns, " + ShapedRecipe.MAX_HEIGHT + " is maximum");
                    }

                    if (i != s.length()) {
                        return DataResult.error(() -> "Invalid pattern: each row must be the same width");
                    }
                }

                return DataResult.success(p_311191_);
            }
        }, Function.identity());
        private static final Codec<Character> SYMBOL_CODEC = Codec.STRING.comapFlatMap(p_313217_ -> {
            if (p_313217_.length() != 1) {
                return DataResult.error(() -> "Invalid key entry: '" + p_313217_ + "' is an invalid symbol (must be 1 character only).");
            } else {
                return " ".equals(p_313217_) ? DataResult.error(() -> "Invalid key entry: ' ' is a reserved symbol.") : DataResult.success(p_313217_.charAt(0));
            }
        }, String::valueOf);
        public static final MapCodec<ShapedRecipePattern.Data> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_359855_ -> p_359855_.group(
                    ExtraCodecs.strictUnboundedMap(SYMBOL_CODEC, Ingredient.CODEC).fieldOf("key").forGetter(p_311797_ -> p_311797_.key),
                    PATTERN_CODEC.fieldOf("pattern").forGetter(p_309770_ -> p_309770_.pattern)
                )
                .apply(p_359855_, ShapedRecipePattern.Data::new)
        );
    }
}
