package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.util.ExtraCodecs;

public record DropChances(Map<EquipmentSlot, Float> byEquipment) {
    public static final float DEFAULT_EQUIPMENT_DROP_CHANCE = 0.085F;
    public static final float PRESERVE_ITEM_DROP_CHANCE_THRESHOLD = 1.0F;
    public static final int PRESERVE_ITEM_DROP_CHANCE = 2;
    public static final DropChances DEFAULT = new DropChances(Util.makeEnumMap(EquipmentSlot.class, p_392495_ -> 0.085F));
    public static final Codec<DropChances> CODEC = Codec.unboundedMap(EquipmentSlot.CODEC, ExtraCodecs.NON_NEGATIVE_FLOAT)
        .xmap(DropChances::toEnumMap, DropChances::filterDefaultValues)
        .xmap(DropChances::new, DropChances::byEquipment);

    private static Map<EquipmentSlot, Float> filterDefaultValues(Map<EquipmentSlot, Float> pChances) {
        Map<EquipmentSlot, Float> map = new HashMap<>(pChances);
        map.values().removeIf(p_391217_ -> p_391217_ == 0.085F);
        return map;
    }

    private static Map<EquipmentSlot, Float> toEnumMap(Map<EquipmentSlot, Float> pChances) {
        return Util.makeEnumMap(EquipmentSlot.class, p_391496_ -> pChances.getOrDefault(p_391496_, 0.085F));
    }

    public DropChances withGuaranteedDrop(EquipmentSlot pSlot) {
        return this.withEquipmentChance(pSlot, 2.0F);
    }

    public DropChances withEquipmentChance(EquipmentSlot pSlot, float pChance) {
        if (pChance < 0.0F) {
            throw new IllegalArgumentException("Tried to set invalid equipment chance " + pChance + " for " + pSlot);
        } else {
            return this.byEquipment(pSlot) == pChance
                ? this
                : new DropChances(Util.makeEnumMap(EquipmentSlot.class, p_392887_ -> p_392887_ == pSlot ? pChance : this.byEquipment(p_392887_)));
        }
    }

    public float byEquipment(EquipmentSlot pSlot) {
        return this.byEquipment.getOrDefault(pSlot, 0.085F);
    }

    public boolean isPreserved(EquipmentSlot pSlot) {
        return this.byEquipment(pSlot) > 1.0F;
    }
}