package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Function;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.SingleComponentItemPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public abstract class EnchantmentsPredicate implements SingleComponentItemPredicate<ItemEnchantments> {
    private final List<EnchantmentPredicate> enchantments;

    protected EnchantmentsPredicate(List<EnchantmentPredicate> pEnchantments) {
        this.enchantments = pEnchantments;
    }

    public static <T extends EnchantmentsPredicate> Codec<T> codec(Function<List<EnchantmentPredicate>, T> pFactory) {
        return EnchantmentPredicate.CODEC.listOf().xmap(pFactory, EnchantmentsPredicate::enchantments);
    }

    protected List<EnchantmentPredicate> enchantments() {
        return this.enchantments;
    }

    public boolean matches(ItemEnchantments p_395653_) {
        for (EnchantmentPredicate enchantmentpredicate : this.enchantments) {
            if (!enchantmentpredicate.containedIn(p_395653_)) {
                return false;
            }
        }

        return true;
    }

    public static EnchantmentsPredicate.Enchantments enchantments(List<EnchantmentPredicate> pEnchantments) {
        return new EnchantmentsPredicate.Enchantments(pEnchantments);
    }

    public static EnchantmentsPredicate.StoredEnchantments storedEnchantments(List<EnchantmentPredicate> pStoreEnchantments) {
        return new EnchantmentsPredicate.StoredEnchantments(pStoreEnchantments);
    }

    public static class Enchantments extends EnchantmentsPredicate {
        public static final Codec<EnchantmentsPredicate.Enchantments> CODEC = codec(EnchantmentsPredicate.Enchantments::new);

        protected Enchantments(List<EnchantmentPredicate> p_391341_) {
            super(p_391341_);
        }

        @Override
        public DataComponentType<ItemEnchantments> componentType() {
            return DataComponents.ENCHANTMENTS;
        }
    }

    public static class StoredEnchantments extends EnchantmentsPredicate {
        public static final Codec<EnchantmentsPredicate.StoredEnchantments> CODEC = codec(EnchantmentsPredicate.StoredEnchantments::new);

        protected StoredEnchantments(List<EnchantmentPredicate> p_393148_) {
            super(p_393148_);
        }

        @Override
        public DataComponentType<ItemEnchantments> componentType() {
            return DataComponents.STORED_ENCHANTMENTS;
        }
    }
}