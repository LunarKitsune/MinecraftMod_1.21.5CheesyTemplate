package net.minecraft.stats;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookRemovePacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookSettingsPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.slf4j.Logger;

public class ServerRecipeBook extends RecipeBook {
    public static final String RECIPE_BOOK_TAG = "recipeBook";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Codec<List<ResourceKey<Recipe<?>>>> RECIPE_LIST_CODEC = Recipe.KEY_CODEC.listOf();
    private final ServerRecipeBook.DisplayResolver displayResolver;
    @VisibleForTesting
    protected final Set<ResourceKey<Recipe<?>>> known = Sets.newIdentityHashSet();
    @VisibleForTesting
    protected final Set<ResourceKey<Recipe<?>>> highlight = Sets.newIdentityHashSet();

    public ServerRecipeBook(ServerRecipeBook.DisplayResolver pDisplayResolver) {
        this.displayResolver = pDisplayResolver;
    }

    public void add(ResourceKey<Recipe<?>> pRecipe) {
        this.known.add(pRecipe);
    }

    public boolean contains(ResourceKey<Recipe<?>> pRecipe) {
        return this.known.contains(pRecipe);
    }

    public void remove(ResourceKey<Recipe<?>> pRecipe) {
        this.known.remove(pRecipe);
        this.highlight.remove(pRecipe);
    }

    public void removeHighlight(ResourceKey<Recipe<?>> pRecipe) {
        this.highlight.remove(pRecipe);
    }

    private void addHighlight(ResourceKey<Recipe<?>> pRecipe) {
        this.highlight.add(pRecipe);
    }

    public int addRecipes(Collection<RecipeHolder<?>> pRecipes, ServerPlayer pPlayer) {
        List<ClientboundRecipeBookAddPacket.Entry> list = new ArrayList<>();

        for (RecipeHolder<?> recipeholder : pRecipes) {
            ResourceKey<Recipe<?>> resourcekey = recipeholder.id();
            if (!this.known.contains(resourcekey) && !recipeholder.value().isSpecial()) {
                this.add(resourcekey);
                this.addHighlight(resourcekey);
                this.displayResolver
                    .displaysForRecipe(
                        resourcekey, p_363687_ -> list.add(new ClientboundRecipeBookAddPacket.Entry(p_363687_, recipeholder.value().showNotification(), true))
                    );
                CriteriaTriggers.RECIPE_UNLOCKED.trigger(pPlayer, recipeholder);
            }
        }

        if (!list.isEmpty()) {
            pPlayer.connection.send(new ClientboundRecipeBookAddPacket(list, false));
        }

        return list.size();
    }

    public int removeRecipes(Collection<RecipeHolder<?>> pRecipes, ServerPlayer pPlayer) {
        List<RecipeDisplayId> list = Lists.newArrayList();

        for (RecipeHolder<?> recipeholder : pRecipes) {
            ResourceKey<Recipe<?>> resourcekey = recipeholder.id();
            if (this.known.contains(resourcekey)) {
                this.remove(resourcekey);
                this.displayResolver.displaysForRecipe(resourcekey, p_364401_ -> list.add(p_364401_.id()));
            }
        }

        if (!list.isEmpty()) {
            pPlayer.connection.send(new ClientboundRecipeBookRemovePacket(list));
        }

        return list.size();
    }

    public CompoundTag toNbt() {
        CompoundTag compoundtag = new CompoundTag();
        this.getBookSettings().write(compoundtag);
        ListTag listtag = new ListTag();

        for (ResourceKey<Recipe<?>> resourcekey : this.known) {
            listtag.add(StringTag.valueOf(resourcekey.location().toString()));
        }

        compoundtag.put("recipes", listtag);
        ListTag listtag1 = new ListTag();

        for (ResourceKey<Recipe<?>> resourcekey1 : this.highlight) {
            listtag1.add(StringTag.valueOf(resourcekey1.location().toString()));
        }

        compoundtag.put("toBeDisplayed", listtag1);
        return compoundtag;
    }

    public void fromNbt(CompoundTag pTag, Predicate<ResourceKey<Recipe<?>>> pIsRecognized) {
        this.setBookSettings(RecipeBookSettings.read(pTag));
        List<ResourceKey<Recipe<?>>> list = pTag.read("recipes", RECIPE_LIST_CODEC).orElse(List.of());
        this.loadRecipes(list, this::add, pIsRecognized);
        List<ResourceKey<Recipe<?>>> list1 = pTag.read("toBeDisplayed", RECIPE_LIST_CODEC).orElse(List.of());
        this.loadRecipes(list1, this::addHighlight, pIsRecognized);
    }

    private void loadRecipes(List<ResourceKey<Recipe<?>>> pRecipes, Consumer<ResourceKey<Recipe<?>>> pOutput, Predicate<ResourceKey<Recipe<?>>> pIsRecognized) {
        for (ResourceKey<Recipe<?>> resourcekey : pRecipes) {
            if (!pIsRecognized.test(resourcekey)) {
                LOGGER.error("Tried to load unrecognized recipe: {} removed now.", resourcekey);
            } else {
                pOutput.accept(resourcekey);
            }
        }
    }

    public void sendInitialRecipeBook(ServerPlayer pPlayer) {
        pPlayer.connection.send(new ClientboundRecipeBookSettingsPacket(this.getBookSettings()));
        List<ClientboundRecipeBookAddPacket.Entry> list = new ArrayList<>(this.known.size());

        for (ResourceKey<Recipe<?>> resourcekey : this.known) {
            this.displayResolver
                .displaysForRecipe(resourcekey, p_369028_ -> list.add(new ClientboundRecipeBookAddPacket.Entry(p_369028_, false, this.highlight.contains(resourcekey))));
        }

        pPlayer.connection.send(new ClientboundRecipeBookAddPacket(list, true));
    }

    public void copyOverData(ServerRecipeBook pOther) {
        this.known.clear();
        this.highlight.clear();
        this.bookSettings.replaceFrom(pOther.bookSettings);
        this.known.addAll(pOther.known);
        this.highlight.addAll(pOther.highlight);
    }

    @FunctionalInterface
    public interface DisplayResolver {
        void displaysForRecipe(ResourceKey<Recipe<?>> pRecipe, Consumer<RecipeDisplayEntry> pOutput);
    }
}