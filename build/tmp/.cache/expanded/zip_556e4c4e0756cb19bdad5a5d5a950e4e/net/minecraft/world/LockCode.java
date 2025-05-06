package net.minecraft.world;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;

public record LockCode(ItemPredicate predicate) {
    public static final LockCode NO_LOCK = new LockCode(ItemPredicate.Builder.item().build());
    public static final Codec<LockCode> CODEC = ItemPredicate.CODEC.xmap(LockCode::new, LockCode::predicate);
    public static final String TAG_LOCK = "lock";

    public boolean unlocksWith(ItemStack pStack) {
        return this.predicate.test(pStack);
    }

    public void addToTag(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        if (this != NO_LOCK) {
            pTag.store("lock", CODEC, pRegistries.createSerializationContext(NbtOps.INSTANCE), this);
        }
    }

    public static LockCode fromTag(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        return pTag.read("lock", CODEC, pRegistries.createSerializationContext(NbtOps.INSTANCE)).orElse(NO_LOCK);
    }
}