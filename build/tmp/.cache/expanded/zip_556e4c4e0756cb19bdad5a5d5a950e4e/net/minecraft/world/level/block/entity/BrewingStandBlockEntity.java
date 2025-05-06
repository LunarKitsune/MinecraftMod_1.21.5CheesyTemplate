package net.minecraft.world.level.block.entity;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BrewingStandBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    private static final int INGREDIENT_SLOT = 3;
    private static final int FUEL_SLOT = 4;
    private static final int[] SLOTS_FOR_UP = new int[]{3};
    private static final int[] SLOTS_FOR_DOWN = new int[]{0, 1, 2, 3};
    private static final int[] SLOTS_FOR_SIDES = new int[]{0, 1, 2, 4};
    public static final int FUEL_USES = 20;
    public static final int DATA_BREW_TIME = 0;
    public static final int DATA_FUEL_USES = 1;
    public static final int NUM_DATA_VALUES = 2;
    private static final short DEFAULT_BREW_TIME = 0;
    private static final byte DEFAULT_FUEL = 0;
    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    int brewTime;
    private boolean[] lastPotionCount;
    private Item ingredient;
    int fuel;
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int p_59038_) {
            return switch (p_59038_) {
                case 0 -> BrewingStandBlockEntity.this.brewTime;
                case 1 -> BrewingStandBlockEntity.this.fuel;
                default -> 0;
            };
        }

        @Override
        public void set(int p_59040_, int p_59041_) {
            switch (p_59040_) {
                case 0:
                    BrewingStandBlockEntity.this.brewTime = p_59041_;
                    break;
                case 1:
                    BrewingStandBlockEntity.this.fuel = p_59041_;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public BrewingStandBlockEntity(BlockPos pPos, BlockState pState) {
        super(BlockEntityType.BREWING_STAND, pPos, pState);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.brewing");
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> p_332629_) {
        this.items = p_332629_;
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, BrewingStandBlockEntity pBlockEntity) {
        ItemStack itemstack = pBlockEntity.items.get(4);
        if (pBlockEntity.fuel <= 0 && itemstack.is(ItemTags.BREWING_FUEL)) {
            pBlockEntity.fuel = 20;
            itemstack.shrink(1);
            setChanged(pLevel, pPos, pState);
        }

        boolean flag = isBrewable(pLevel.potionBrewing(), pBlockEntity.items);
        boolean flag1 = pBlockEntity.brewTime > 0;
        ItemStack itemstack1 = pBlockEntity.items.get(3);
        if (flag1) {
            pBlockEntity.brewTime--;
            boolean flag2 = pBlockEntity.brewTime == 0;
            if (flag2 && flag) {
                doBrew(pLevel, pPos, pBlockEntity.items);
            } else if (!flag || !itemstack1.is(pBlockEntity.ingredient)) {
                pBlockEntity.brewTime = 0;
            }

            setChanged(pLevel, pPos, pState);
        } else if (flag && pBlockEntity.fuel > 0) {
            pBlockEntity.fuel--;
            pBlockEntity.brewTime = 400;
            pBlockEntity.ingredient = itemstack1.getItem();
            setChanged(pLevel, pPos, pState);
        }

        boolean[] aboolean = pBlockEntity.getPotionBits();
        if (!Arrays.equals(aboolean, pBlockEntity.lastPotionCount)) {
            pBlockEntity.lastPotionCount = aboolean;
            BlockState blockstate = pState;
            if (!(pState.getBlock() instanceof BrewingStandBlock)) {
                return;
            }

            for (int i = 0; i < BrewingStandBlock.HAS_BOTTLE.length; i++) {
                blockstate = blockstate.setValue(BrewingStandBlock.HAS_BOTTLE[i], aboolean[i]);
            }

            pLevel.setBlock(pPos, blockstate, 2);
        }
    }

    private boolean[] getPotionBits() {
        boolean[] aboolean = new boolean[3];

        for (int i = 0; i < 3; i++) {
            if (!this.items.get(i).isEmpty()) {
                aboolean[i] = true;
            }
        }

        return aboolean;
    }

    private static boolean isBrewable(PotionBrewing pPotionBrewing, NonNullList<ItemStack> pItems) {
        ItemStack itemstack = pItems.get(3);
        if (itemstack.isEmpty()) {
            return false;
        } else if (!pPotionBrewing.isIngredient(itemstack)) {
            return false;
        } else {
            for (int i = 0; i < 3; i++) {
                ItemStack itemstack1 = pItems.get(i);
                if (!itemstack1.isEmpty() && pPotionBrewing.hasMix(itemstack1, itemstack)) {
                    return true;
                }
            }

            return false;
        }
    }

    private static void doBrew(Level pLevel, BlockPos pPos, NonNullList<ItemStack> pItems) {
        ItemStack itemstack = pItems.get(3);
        PotionBrewing potionbrewing = pLevel.potionBrewing();

        for (int i = 0; i < 3; i++) {
            pItems.set(i, potionbrewing.mix(itemstack, pItems.get(i)));
        }
        net.minecraftforge.event.ForgeEventFactory.onPotionBrewed(pItems);

        ItemStack itemstack1 = itemstack.getCraftingRemainder();
        if (!itemstack1.isEmpty()) {
            itemstack.shrink(1);
            if (itemstack.isEmpty()) {
                itemstack = itemstack1;
            } else {
                Containers.dropItemStack(pLevel, pPos.getX(), pPos.getY(), pPos.getZ(), itemstack1);
            }
        } else {
            itemstack.shrink(1);
        }

        pItems.set(3, itemstack);
        pLevel.levelEvent(1035, pPos, 0);
    }

    @Override
    protected void loadAdditional(CompoundTag p_335279_, HolderLookup.Provider p_330361_) {
        super.loadAdditional(p_335279_, p_330361_);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(p_335279_, this.items, p_330361_);
        this.brewTime = p_335279_.getShortOr("BrewTime", (short)0);
        if (this.brewTime > 0) {
            this.ingredient = this.items.get(3).getItem();
        }

        this.fuel = p_335279_.getByteOr("Fuel", (byte)0);
    }

    @Override
    protected void saveAdditional(CompoundTag p_187484_, HolderLookup.Provider p_336147_) {
        super.saveAdditional(p_187484_, p_336147_);
        p_187484_.putShort("BrewTime", (short)this.brewTime);
        ContainerHelper.saveAllItems(p_187484_, this.items, p_336147_);
        p_187484_.putByte("Fuel", (byte)this.fuel);
    }

    @Override
    public boolean canPlaceItem(int pIndex, ItemStack pStack) {
        if (pIndex == 3) {
            PotionBrewing potionbrewing = this.level != null ? this.level.potionBrewing() : PotionBrewing.EMPTY;
            return potionbrewing.isIngredient(pStack);
        } else if (pIndex != 4) {
            PotionBrewing potionbrewing = this.level != null ? this.level.potionBrewing() : PotionBrewing.EMPTY;
            return this.getItem(pIndex).isEmpty() && potionbrewing.isValidInput(pStack);
        } else {
            return pIndex == 4
                ? pStack.is(ItemTags.BREWING_FUEL)
                : (
                        pStack.is(Items.POTION)
                            || pStack.is(Items.SPLASH_POTION)
                            || pStack.is(Items.LINGERING_POTION)
                            || pStack.is(Items.GLASS_BOTTLE)
                    )
                    && this.getItem(pIndex).isEmpty();
        }
    }

    @Override
    public int[] getSlotsForFace(Direction pSide) {
        if (pSide == Direction.UP) {
            return SLOTS_FOR_UP;
        } else {
            return pSide == Direction.DOWN ? SLOTS_FOR_DOWN : SLOTS_FOR_SIDES;
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) {
        return this.canPlaceItem(pIndex, pItemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) {
        return pIndex == 3 ? pStack.is(Items.GLASS_BOTTLE) : true;
    }

    @Override
    protected AbstractContainerMenu createMenu(int pId, Inventory pPlayer) {
        return new BrewingStandMenu(pId, pPlayer, this, this.dataAccess);
    }

    net.minecraftforge.common.util.LazyOptional<? extends net.minecraftforge.items.IItemHandler>[] handlers =
        net.minecraftforge.items.wrapper.SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing) {
        if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && facing != null && !this.remove) {
            return switch (facing) {
                case UP -> handlers[0].cast();
                case DOWN -> handlers[1].cast();
                default -> handlers[2].cast();
            };
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (int x = 0; x < handlers.length; x++) {
            handlers[x].invalidate();
        }
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        this.handlers = net.minecraftforge.items.wrapper.SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);
    }
}
