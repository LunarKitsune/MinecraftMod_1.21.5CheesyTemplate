package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BaseContainerBlockEntity extends BlockEntity implements Container, MenuProvider, Nameable {
    private LockCode lockKey = LockCode.NO_LOCK;
    @Nullable
    private Component name;

    protected BaseContainerBlockEntity(BlockEntityType<?> p_155076_, BlockPos p_155077_, BlockState p_155078_) {
        super(p_155076_, p_155077_, p_155078_);
    }

    @Override
    protected void loadAdditional(CompoundTag p_335335_, HolderLookup.Provider p_329555_) {
        super.loadAdditional(p_335335_, p_329555_);
        this.lockKey = LockCode.fromTag(p_335335_, p_329555_);
        this.name = parseCustomNameSafe(p_335335_.get("CustomName"), p_329555_);
    }

    @Override
    protected void saveAdditional(CompoundTag p_187461_, HolderLookup.Provider p_335192_) {
        super.saveAdditional(p_187461_, p_335192_);
        this.lockKey.addToTag(p_187461_, p_335192_);
        if (this.name != null) {
            p_187461_.put("CustomName", ComponentSerialization.CODEC.encodeStart(p_335192_.createSerializationContext(NbtOps.INSTANCE), this.name).getOrThrow());
        }
    }

    @Override
    public Component getName() {
        return this.name != null ? this.name : this.getDefaultName();
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return this.name;
    }

    protected abstract Component getDefaultName();

    public boolean canOpen(Player pPlayer) {
        return canUnlock(pPlayer, this.lockKey, this.getDisplayName());
    }

    public static boolean canUnlock(Player pPlayer, LockCode pCode, Component pDisplayName) {
        if (!pPlayer.isSpectator() && !pCode.unlocksWith(pPlayer.getMainHandItem())) {
            pPlayer.displayClientMessage(Component.translatable("container.isLocked", pDisplayName), true);
            pPlayer.playNotifySound(SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
            return false;
        } else {
            return true;
        }
    }

    protected abstract NonNullList<ItemStack> getItems();

    protected abstract void setItems(NonNullList<ItemStack> pItems);

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.getItems()) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int p_334660_) {
        return this.getItems().get(p_334660_);
    }

    @Override
    public ItemStack removeItem(int p_333934_, int p_332088_) {
        ItemStack itemstack = ContainerHelper.removeItem(this.getItems(), p_333934_, p_332088_);
        if (!itemstack.isEmpty()) {
            this.setChanged();
        }

        return itemstack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int p_329940_) {
        return ContainerHelper.takeItem(this.getItems(), p_329940_);
    }

    @Override
    public void setItem(int p_331067_, ItemStack p_333112_) {
        this.getItems().set(p_331067_, p_333112_);
        p_333112_.limitSize(this.getMaxStackSize(p_333112_));
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player p_330935_) {
        return Container.stillValidBlockEntity(this, p_330935_);
    }

    @Override
    public void clearContent() {
        this.getItems().clear();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int p_58641_, Inventory p_58642_, Player p_58643_) {
        return this.canOpen(p_58643_) ? this.createMenu(p_58641_, p_58642_) : null;
    }

    protected abstract AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory);

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_392615_) {
        super.applyImplicitComponents(p_392615_);
        this.name = p_392615_.get(DataComponents.CUSTOM_NAME);
        this.lockKey = p_392615_.getOrDefault(DataComponents.LOCK, LockCode.NO_LOCK);
        p_392615_.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.getItems());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder p_336292_) {
        super.collectImplicitComponents(p_336292_);
        p_336292_.set(DataComponents.CUSTOM_NAME, this.name);
        if (!this.lockKey.equals(LockCode.NO_LOCK)) {
            p_336292_.set(DataComponents.LOCK, this.lockKey);
        }

        p_336292_.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag p_329140_) {
        p_329140_.remove("CustomName");
        p_329140_.remove("lock");
        p_329140_.remove("Items");
    }

    private net.minecraftforge.common.util.LazyOptional<?> itemHandler = net.minecraftforge.common.util.LazyOptional.of(() -> createUnSidedHandler());
    protected net.minecraftforge.items.IItemHandler createUnSidedHandler() {
        return new net.minecraftforge.items.wrapper.InvWrapper(this);
    }

    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && !this.remove)
            return itemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemHandler = net.minecraftforge.common.util.LazyOptional.of(() -> createUnSidedHandler());
    }
}
