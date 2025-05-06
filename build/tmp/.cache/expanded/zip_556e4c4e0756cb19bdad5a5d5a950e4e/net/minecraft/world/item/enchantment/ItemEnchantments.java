package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

public class ItemEnchantments implements TooltipProvider {
    public static final ItemEnchantments EMPTY = new ItemEnchantments(new Object2IntOpenHashMap<>());
    private static final Codec<Integer> LEVEL_CODEC = Codec.intRange(1, 255);
    public static final Codec<ItemEnchantments> CODEC = Codec.unboundedMap(Enchantment.CODEC, LEVEL_CODEC)
        .xmap(
            p_390857_ -> new ItemEnchantments(new Object2IntOpenHashMap<>((Map<? extends Holder<Enchantment>, ? extends Integer>)p_390857_)),
            p_334450_ -> p_334450_.enchantments
        );
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemEnchantments> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(Object2IntOpenHashMap::new, Enchantment.STREAM_CODEC, ByteBufCodecs.VAR_INT),
        p_334569_ -> p_334569_.enchantments,
        ItemEnchantments::new
    );
    final Object2IntOpenHashMap<Holder<Enchantment>> enchantments;

    ItemEnchantments(Object2IntOpenHashMap<Holder<Enchantment>> pEnchantments) {
        this.enchantments = pEnchantments;

        for (Entry<Holder<Enchantment>> entry : pEnchantments.object2IntEntrySet()) {
            int i = entry.getIntValue();
            if (i < 0 || i > 255) {
                throw new IllegalArgumentException("Enchantment " + entry.getKey() + " has invalid level " + i);
            }
        }
    }

    public int getLevel(Holder<Enchantment> pEnchantment) {
        return this.enchantments.getInt(pEnchantment);
    }

    @Override
    public void addToTooltip(Item.TooltipContext p_332503_, Consumer<Component> p_333731_, TooltipFlag p_332196_, DataComponentGetter p_396769_) {
        HolderLookup.Provider holderlookup$provider = p_332503_.registries();
        HolderSet<Enchantment> holderset = getTagOrEmpty(holderlookup$provider, Registries.ENCHANTMENT, EnchantmentTags.TOOLTIP_ORDER);

        for (Holder<Enchantment> holder : holderset) {
            int i = this.enchantments.getInt(holder);
            if (i > 0) {
                p_333731_.accept(Enchantment.getFullname(holder, i));
            }
        }

        for (Entry<Holder<Enchantment>> entry : this.enchantments.object2IntEntrySet()) {
            Holder<Enchantment> holder1 = entry.getKey();
            if (!holderset.contains(holder1)) {
                p_333731_.accept(Enchantment.getFullname(entry.getKey(), entry.getIntValue()));
            }
        }
    }

    private static <T> HolderSet<T> getTagOrEmpty(@Nullable HolderLookup.Provider pRegistries, ResourceKey<Registry<T>> pRegistryKey, TagKey<T> pKey) {
        if (pRegistries != null) {
            Optional<HolderSet.Named<T>> optional = pRegistries.lookupOrThrow(pRegistryKey).get(pKey);
            if (optional.isPresent()) {
                return optional.get();
            }
        }

        return HolderSet.direct();
    }

    public Set<Holder<Enchantment>> keySet() {
        return Collections.unmodifiableSet(this.enchantments.keySet());
    }

    public Set<Entry<Holder<Enchantment>>> entrySet() {
        return Collections.unmodifiableSet(this.enchantments.object2IntEntrySet());
    }

    public int size() {
        return this.enchantments.size();
    }

    public boolean isEmpty() {
        return this.enchantments.isEmpty();
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return pOther instanceof ItemEnchantments itemenchantments ? this.enchantments.equals(itemenchantments.enchantments) : false;
        }
    }

    @Override
    public int hashCode() {
        return this.enchantments.hashCode();
    }

    @Override
    public String toString() {
        return "ItemEnchantments{enchantments=" + this.enchantments + "}";
    }

    public static class Mutable {
        private final Object2IntOpenHashMap<Holder<Enchantment>> enchantments = new Object2IntOpenHashMap<>();

        public Mutable(ItemEnchantments pEnchantments) {
            this.enchantments.putAll(pEnchantments.enchantments);
        }

        public void set(Holder<Enchantment> pEnchantment, int pLevel) {
            if (pLevel <= 0) {
                this.enchantments.removeInt(pEnchantment);
            } else {
                this.enchantments.put(pEnchantment, Math.min(pLevel, 255));
            }
        }

        public void upgrade(Holder<Enchantment> pEnchantment, int pLevel) {
            if (pLevel > 0) {
                this.enchantments.merge(pEnchantment, Math.min(pLevel, 255), Integer::max);
            }
        }

        public void removeIf(Predicate<Holder<Enchantment>> pPredicate) {
            this.enchantments.keySet().removeIf(pPredicate);
        }

        public int getLevel(Holder<Enchantment> pEnchantment) {
            return this.enchantments.getOrDefault(pEnchantment, 0);
        }

        public Set<Holder<Enchantment>> keySet() {
            return this.enchantments.keySet();
        }

        public ItemEnchantments toImmutable() {
            return new ItemEnchantments(this.enchantments);
        }
    }
}