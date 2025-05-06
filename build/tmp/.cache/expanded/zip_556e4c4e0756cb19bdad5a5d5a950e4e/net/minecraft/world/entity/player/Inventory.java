package net.minecraft.world.entity.player;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class Inventory implements Container, Nameable {
    public static final int POP_TIME_DURATION = 5;
    public static final int INVENTORY_SIZE = 36;
    public static final int SELECTION_SIZE = 9;
    public static final int SLOT_OFFHAND = 40;
    public static final int NOT_FOUND_INDEX = -1;
    public static final Int2ObjectMap<EquipmentSlot> EQUIPMENT_SLOT_MAPPING = new Int2ObjectArrayMap<>(
        Map.of(
            EquipmentSlot.FEET.getIndex(36),
            EquipmentSlot.FEET,
            EquipmentSlot.LEGS.getIndex(36),
            EquipmentSlot.LEGS,
            EquipmentSlot.CHEST.getIndex(36),
            EquipmentSlot.CHEST,
            EquipmentSlot.HEAD.getIndex(36),
            EquipmentSlot.HEAD,
            40,
            EquipmentSlot.OFFHAND
        )
    );
    private final NonNullList<ItemStack> items = NonNullList.withSize(36, ItemStack.EMPTY);
    private int selected;
    public final Player player;
    private final EntityEquipment equipment;
    private int timesChanged;

    public Inventory(Player pPlayer, EntityEquipment pEquipment) {
        this.player = pPlayer;
        this.equipment = pEquipment;
    }

    public int getSelectedSlot() {
        return this.selected;
    }

    public void setSelectedSlot(int pSlot) {
        if (!isHotbarSlot(pSlot)) {
            throw new IllegalArgumentException("Invalid selected slot");
        } else {
            this.selected = pSlot;
        }
    }

    public ItemStack getSelectedItem() {
        return this.items.get(this.selected);
    }

    public ItemStack setSelectedItem(ItemStack pStack) {
        return this.items.set(this.selected, pStack);
    }

    public static int getSelectionSize() {
        return 9;
    }

    public NonNullList<ItemStack> getNonEquipmentItems() {
        return this.items;
    }

    public EntityEquipment getEquipment() {
        return this.equipment;
    }

    private boolean hasRemainingSpaceForItem(ItemStack pDestination, ItemStack pOrigin) {
        return !pDestination.isEmpty() && ItemStack.isSameItemSameComponents(pDestination, pOrigin) && pDestination.isStackable() && pDestination.getCount() < this.getMaxStackSize(pDestination);
    }

    public int getFreeSlot() {
        for (int i = 0; i < this.items.size(); i++) {
            if (this.items.get(i).isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    public void addAndPickItem(ItemStack pStack) {
        this.setSelectedSlot(this.getSuitableHotbarSlot());
        if (!this.items.get(this.selected).isEmpty()) {
            int i = this.getFreeSlot();
            if (i != -1) {
                this.items.set(i, this.items.get(this.selected));
            }
        }

        this.items.set(this.selected, pStack);
    }

    public void pickSlot(int pIndex) {
        this.setSelectedSlot(this.getSuitableHotbarSlot());
        ItemStack itemstack = this.items.get(this.selected);
        this.items.set(this.selected, this.items.get(pIndex));
        this.items.set(pIndex, itemstack);
    }

    public static boolean isHotbarSlot(int pIndex) {
        return pIndex >= 0 && pIndex < 9;
    }

    public int findSlotMatchingItem(ItemStack pStack) {
        for (int i = 0; i < this.items.size(); i++) {
            if (!this.items.get(i).isEmpty() && ItemStack.isSameItemSameComponents(pStack, this.items.get(i))) {
                return i;
            }
        }

        return -1;
    }

    public static boolean isUsableForCrafting(ItemStack pStack) {
        return !pStack.isDamaged() && !pStack.isEnchanted() && !pStack.has(DataComponents.CUSTOM_NAME);
    }

    public int findSlotMatchingCraftingIngredient(Holder<Item> pItem, ItemStack pStack) {
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack itemstack = this.items.get(i);
            if (!itemstack.isEmpty()
                && itemstack.is(pItem)
                && isUsableForCrafting(itemstack)
                && (pStack.isEmpty() || ItemStack.isSameItemSameComponents(pStack, itemstack))) {
                return i;
            }
        }

        return -1;
    }

    public int getSuitableHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            int j = (this.selected + i) % 9;
            if (this.items.get(j).isEmpty()) {
                return j;
            }
        }

        for (int k = 0; k < 9; k++) {
            int l = (this.selected + k) % 9;
            if (!this.items.get(l).isNotReplaceableByPickAction(this.player, l)) {
                return l;
            }
        }

        return this.selected;
    }

    public int clearOrCountMatchingItems(Predicate<ItemStack> pStackPredicate, int pMaxCount, Container pInventory) {
        int i = 0;
        boolean flag = pMaxCount == 0;
        i += ContainerHelper.clearOrCountMatchingItems(this, pStackPredicate, pMaxCount - i, flag);
        i += ContainerHelper.clearOrCountMatchingItems(pInventory, pStackPredicate, pMaxCount - i, flag);
        ItemStack itemstack = this.player.containerMenu.getCarried();
        i += ContainerHelper.clearOrCountMatchingItems(itemstack, pStackPredicate, pMaxCount - i, flag);
        if (itemstack.isEmpty()) {
            this.player.containerMenu.setCarried(ItemStack.EMPTY);
        }

        return i;
    }

    private int addResource(ItemStack pStack) {
        int i = this.getSlotWithRemainingSpace(pStack);
        if (i == -1) {
            i = this.getFreeSlot();
        }

        return i == -1 ? pStack.getCount() : this.addResource(i, pStack);
    }

    private int addResource(int pSlot, ItemStack pStack) {
        int i = pStack.getCount();
        ItemStack itemstack = this.getItem(pSlot);
        if (itemstack.isEmpty()) {
            itemstack = pStack.copyWithCount(0);
            this.setItem(pSlot, itemstack);
        }

        int j = this.getMaxStackSize(itemstack) - itemstack.getCount();
        int k = Math.min(i, j);
        if (k == 0) {
            return i;
        } else {
            i -= k;
            itemstack.grow(k);
            itemstack.setPopTime(5);
            return i;
        }
    }

    public int getSlotWithRemainingSpace(ItemStack pStack) {
        if (this.hasRemainingSpaceForItem(this.getItem(this.selected), pStack)) {
            return this.selected;
        } else if (this.hasRemainingSpaceForItem(this.getItem(40), pStack)) {
            return 40;
        } else {
            for (int i = 0; i < this.items.size(); i++) {
                if (this.hasRemainingSpaceForItem(this.items.get(i), pStack)) {
                    return i;
                }
            }

            return -1;
        }
    }

    public void tick() {
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack itemstack = this.getItem(i);
            if (!itemstack.isEmpty()) {
                itemstack.inventoryTick(this.player.level(), this.player, i == this.selected ? EquipmentSlot.MAINHAND : null, i);
            }
        }
    }

    public boolean add(ItemStack pStack) {
        return this.add(-1, pStack);
    }

    public boolean add(int pSlot, ItemStack pStack) {
        if (pStack.isEmpty()) {
            return false;
        } else {
            try {
                if (pStack.isDamaged()) {
                    if (pSlot == -1) {
                        pSlot = this.getFreeSlot();
                    }

                    if (pSlot >= 0) {
                        this.items.set(pSlot, pStack.copyAndClear());
                        this.items.get(pSlot).setPopTime(5);
                        return true;
                    } else if (this.player.hasInfiniteMaterials()) {
                        pStack.setCount(0);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    int i;
                    do {
                        i = pStack.getCount();
                        if (pSlot == -1) {
                            pStack.setCount(this.addResource(pStack));
                        } else {
                            pStack.setCount(this.addResource(pSlot, pStack));
                        }
                    } while (!pStack.isEmpty() && pStack.getCount() < i);

                    if (pStack.getCount() == i && this.player.hasInfiniteMaterials()) {
                        pStack.setCount(0);
                        return true;
                    } else {
                        return pStack.getCount() < i;
                    }
                }
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Adding item to inventory");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Item being added");
                crashreportcategory.setDetail("Registry Name", () -> String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(pStack.getItem())));
                crashreportcategory.setDetail("Item Class", () -> pStack.getItem().getClass().getName());
                crashreportcategory.setDetail("Item ID", Item.getId(pStack.getItem()));
                crashreportcategory.setDetail("Item data", pStack.getDamageValue());
                crashreportcategory.setDetail("Item name", () -> pStack.getHoverName().getString());
                throw new ReportedException(crashreport);
            }
        }
    }

    public void placeItemBackInInventory(ItemStack pStack) {
        this.placeItemBackInInventory(pStack, true);
    }

    public void placeItemBackInInventory(ItemStack pStack, boolean pSendPacket) {
        while (!pStack.isEmpty()) {
            int i = this.getSlotWithRemainingSpace(pStack);
            if (i == -1) {
                i = this.getFreeSlot();
            }

            if (i == -1) {
                this.player.drop(pStack, false);
                break;
            }

            int j = pStack.getMaxStackSize() - this.getItem(i).getCount();
            if (this.add(i, pStack.split(j)) && pSendPacket && this.player instanceof ServerPlayer serverplayer) {
                serverplayer.connection.send(this.createInventoryUpdatePacket(i));
            }
        }
    }

    public ClientboundSetPlayerInventoryPacket createInventoryUpdatePacket(int pSlot) {
        return new ClientboundSetPlayerInventoryPacket(pSlot, this.getItem(pSlot).copy());
    }

    @Override
    public ItemStack removeItem(int pIndex, int pCount) {
        if (pIndex < this.items.size()) {
            return ContainerHelper.removeItem(this.items, pIndex, pCount);
        } else {
            EquipmentSlot equipmentslot = EQUIPMENT_SLOT_MAPPING.get(pIndex);
            if (equipmentslot != null) {
                ItemStack itemstack = this.equipment.get(equipmentslot);
                if (!itemstack.isEmpty()) {
                    return itemstack.split(pCount);
                }
            }

            return ItemStack.EMPTY;
        }
    }

    public void removeItem(ItemStack pStack) {
        for (int i = 0; i < this.items.size(); i++) {
            if (this.items.get(i) == pStack) {
                this.items.set(i, ItemStack.EMPTY);
                return;
            }
        }

        for (EquipmentSlot equipmentslot : EQUIPMENT_SLOT_MAPPING.values()) {
            ItemStack itemstack = this.equipment.get(equipmentslot);
            if (itemstack == pStack) {
                this.equipment.set(equipmentslot, ItemStack.EMPTY);
                return;
            }
        }
    }

    @Override
    public ItemStack removeItemNoUpdate(int pIndex) {
        if (pIndex < this.items.size()) {
            ItemStack itemstack = this.items.get(pIndex);
            this.items.set(pIndex, ItemStack.EMPTY);
            return itemstack;
        } else {
            EquipmentSlot equipmentslot = EQUIPMENT_SLOT_MAPPING.get(pIndex);
            return equipmentslot != null ? this.equipment.set(equipmentslot, ItemStack.EMPTY) : ItemStack.EMPTY;
        }
    }

    @Override
    public void setItem(int pIndex, ItemStack pStack) {
        if (pIndex < this.items.size()) {
            this.items.set(pIndex, pStack);
        }

        EquipmentSlot equipmentslot = EQUIPMENT_SLOT_MAPPING.get(pIndex);
        if (equipmentslot != null) {
            this.equipment.set(equipmentslot, pStack);
        }
    }

    public ListTag save(ListTag pListTag) {
        for (int i = 0; i < this.items.size(); i++) {
            if (!this.items.get(i).isEmpty()) {
                CompoundTag compoundtag = new CompoundTag();
                compoundtag.putByte("Slot", (byte)i);
                pListTag.add(this.items.get(i).save(this.player.registryAccess(), compoundtag));
            }
        }

        return pListTag;
    }

    public void load(ListTag pListTag) {
        this.items.clear();

        for (int i = 0; i < pListTag.size(); i++) {
            CompoundTag compoundtag = pListTag.getCompoundOrEmpty(i);
            int j = compoundtag.getByteOr("Slot", (byte)0) & 255;
            ItemStack itemstack = ItemStack.parse(this.player.registryAccess(), compoundtag).orElse(ItemStack.EMPTY);
            if (j < this.items.size()) {
                this.setItem(j, itemstack);
            }
        }
    }

    @Override
    public int getContainerSize() {
        return this.items.size() + EQUIPMENT_SLOT_MAPPING.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        for (EquipmentSlot equipmentslot : EQUIPMENT_SLOT_MAPPING.values()) {
            if (!this.equipment.get(equipmentslot).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int pIndex) {
        if (pIndex < this.items.size()) {
            return this.items.get(pIndex);
        } else {
            EquipmentSlot equipmentslot = EQUIPMENT_SLOT_MAPPING.get(pIndex);
            return equipmentslot != null ? this.equipment.get(equipmentslot) : ItemStack.EMPTY;
        }
    }

    @Override
    public Component getName() {
        return Component.translatable("container.inventory");
    }

    public void dropAll() {
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack itemstack = this.items.get(i);
            if (!itemstack.isEmpty()) {
                this.player.drop(itemstack, true, false);
                this.items.set(i, ItemStack.EMPTY);
            }
        }

        this.equipment.dropAll(this.player);
    }

    @Override
    public void setChanged() {
        this.timesChanged++;
    }

    public int getTimesChanged() {
        return this.timesChanged;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return true;
    }

    public boolean contains(ItemStack pStack) {
        for (ItemStack itemstack : this) {
            if (!itemstack.isEmpty() && ItemStack.isSameItemSameComponents(itemstack, pStack)) {
                return true;
            }
        }

        return false;
    }

    public boolean contains(TagKey<Item> pTag) {
        for (ItemStack itemstack : this) {
            if (!itemstack.isEmpty() && itemstack.is(pTag)) {
                return true;
            }
        }

        return false;
    }

    public boolean contains(Predicate<ItemStack> pPredicate) {
        for (ItemStack itemstack : this) {
            if (pPredicate.test(itemstack)) {
                return true;
            }
        }

        return false;
    }

    public void replaceWith(Inventory pPlayerInventory) {
        for (int i = 0; i < this.getContainerSize(); i++) {
            this.setItem(i, pPlayerInventory.getItem(i));
        }

        this.setSelectedSlot(pPlayerInventory.getSelectedSlot());
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.equipment.clear();
    }

    public void fillStackedContents(StackedItemContents pContents) {
        for (ItemStack itemstack : this.items) {
            pContents.accountSimpleStack(itemstack);
        }
    }

    public ItemStack removeFromSelected(boolean pRemoveStack) {
        ItemStack itemstack = this.getSelectedItem();
        return itemstack.isEmpty() ? ItemStack.EMPTY : this.removeItem(this.selected, pRemoveStack ? itemstack.getCount() : 1);
    }
}
