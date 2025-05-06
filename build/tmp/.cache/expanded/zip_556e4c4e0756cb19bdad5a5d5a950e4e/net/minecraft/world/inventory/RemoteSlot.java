package net.minecraft.world.inventory;

import javax.annotation.Nullable;
import net.minecraft.network.HashedPatchMap;
import net.minecraft.network.HashedStack;
import net.minecraft.world.item.ItemStack;

public interface RemoteSlot {
    RemoteSlot PLACEHOLDER = new RemoteSlot() {
        @Override
        public void receive(HashedStack p_393094_) {
        }

        @Override
        public void force(ItemStack p_394507_) {
        }

        @Override
        public boolean matches(ItemStack p_391424_) {
            return true;
        }
    };

    void force(ItemStack pStack);

    void receive(HashedStack pStack);

    boolean matches(ItemStack pStack);

    public static class Synchronized implements RemoteSlot {
        private final HashedPatchMap.HashGenerator hasher;
        @Nullable
        private ItemStack remoteStack = null;
        @Nullable
        private HashedStack remoteHash = null;

        public Synchronized(HashedPatchMap.HashGenerator pHasher) {
            this.hasher = pHasher;
        }

        @Override
        public void force(ItemStack p_392006_) {
            this.remoteStack = p_392006_.copy();
            this.remoteHash = null;
        }

        @Override
        public void receive(HashedStack p_392600_) {
            this.remoteStack = null;
            this.remoteHash = p_392600_;
        }

        @Override
        public boolean matches(ItemStack p_392251_) {
            if (this.remoteStack != null) {
                return ItemStack.matches(this.remoteStack, p_392251_);
            } else if (this.remoteHash != null && this.remoteHash.matches(p_392251_, this.hasher)) {
                this.remoteStack = p_392251_.copy();
                return true;
            } else {
                return false;
            }
        }

        public void copyFrom(RemoteSlot.Synchronized pOther) {
            this.remoteStack = pOther.remoteStack;
            this.remoteHash = pOther.remoteHash;
        }
    }
}