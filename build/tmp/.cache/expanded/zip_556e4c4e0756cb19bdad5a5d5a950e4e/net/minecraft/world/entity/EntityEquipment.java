package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import net.minecraft.world.item.ItemStack;

public class EntityEquipment {
    public static final Codec<EntityEquipment> CODEC = Codec.unboundedMap(EquipmentSlot.CODEC, ItemStack.CODEC).xmap(p_395484_ -> {
        EnumMap<EquipmentSlot, ItemStack> enummap = new EnumMap<>(EquipmentSlot.class);
        enummap.putAll((Map<? extends EquipmentSlot, ? extends ItemStack>)p_395484_);
        return new EntityEquipment(enummap);
    }, p_392815_ -> {
        Map<EquipmentSlot, ItemStack> map = new EnumMap<>(p_392815_.items);
        map.values().removeIf(ItemStack::isEmpty);
        return map;
    });
    private final EnumMap<EquipmentSlot, ItemStack> items;

    private EntityEquipment(EnumMap<EquipmentSlot, ItemStack> pItems) {
        this.items = pItems;
    }

    public EntityEquipment() {
        this(new EnumMap<>(EquipmentSlot.class));
    }

    public ItemStack set(EquipmentSlot pSlot, ItemStack pStack) {
        pStack.getItem().verifyComponentsAfterLoad(pStack);
        return Objects.requireNonNullElse(this.items.put(pSlot, pStack), ItemStack.EMPTY);
    }

    public ItemStack get(EquipmentSlot pSlot) {
        return this.items.getOrDefault(pSlot, ItemStack.EMPTY);
    }

    public boolean isEmpty() {
        for (ItemStack itemstack : this.items.values()) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public void tick(Entity pEntity) {
        for (Entry<EquipmentSlot, ItemStack> entry : this.items.entrySet()) {
            ItemStack itemstack = entry.getValue();
            if (!itemstack.isEmpty()) {
                itemstack.inventoryTick(pEntity.level(), pEntity, entry.getKey(), -1);
            }
        }
    }

    public void setAll(EntityEquipment pEquipment) {
        this.items.clear();
        this.items.putAll(pEquipment.items);
    }

    public void dropAll(LivingEntity pEntity) {
        for (ItemStack itemstack : this.items.values()) {
            pEntity.drop(itemstack, true, false);
        }

        this.clear();
    }

    public void clear() {
        this.items.replaceAll((p_393205_, p_394162_) -> ItemStack.EMPTY);
    }

    public int size() {
        return this.items.size();
    }
}
