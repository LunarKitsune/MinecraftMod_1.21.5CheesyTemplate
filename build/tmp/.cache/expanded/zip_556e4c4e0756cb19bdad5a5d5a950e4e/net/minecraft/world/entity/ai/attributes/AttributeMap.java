package net.minecraft.world.entity.ai.attributes;

import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;

public class AttributeMap {
    private final Map<Holder<Attribute>, AttributeInstance> attributes = new Object2ObjectOpenHashMap<>();
    private final Set<AttributeInstance> attributesToSync = new ObjectOpenHashSet<>();
    private final Set<AttributeInstance> attributesToUpdate = new ObjectOpenHashSet<>();
    private final AttributeSupplier supplier;

    public AttributeMap(AttributeSupplier pSupplier) {
        this.supplier = pSupplier;
    }

    private void onAttributeModified(AttributeInstance pInstance) {
        this.attributesToUpdate.add(pInstance);
        if (pInstance.getAttribute().value().isClientSyncable()) {
            this.attributesToSync.add(pInstance);
        }
    }

    public Set<AttributeInstance> getAttributesToSync() {
        return this.attributesToSync;
    }

    public Set<AttributeInstance> getAttributesToUpdate() {
        return this.attributesToUpdate;
    }

    public Collection<AttributeInstance> getSyncableAttributes() {
        return this.attributes.values().stream().filter(p_326797_ -> p_326797_.getAttribute().value().isClientSyncable()).collect(Collectors.toList());
    }

    @Nullable
    public AttributeInstance getInstance(Holder<Attribute> pAttribute) {
        return this.attributes.computeIfAbsent(pAttribute, p_326793_ -> this.supplier.createInstance(this::onAttributeModified, (Holder<Attribute>)p_326793_));
    }

    public boolean hasAttribute(Holder<Attribute> pAttribute) {
        return this.attributes.get(pAttribute) != null || this.supplier.hasAttribute(pAttribute);
    }

    public boolean hasModifier(Holder<Attribute> pAttribute, ResourceLocation pId) {
        AttributeInstance attributeinstance = this.attributes.get(pAttribute);
        return attributeinstance != null ? attributeinstance.getModifier(pId) != null : this.supplier.hasModifier(pAttribute, pId);
    }

    public double getValue(Holder<Attribute> pAttribute) {
        AttributeInstance attributeinstance = this.attributes.get(pAttribute);
        return attributeinstance != null ? attributeinstance.getValue() : this.supplier.getValue(pAttribute);
    }

    public double getBaseValue(Holder<Attribute> pAttribute) {
        AttributeInstance attributeinstance = this.attributes.get(pAttribute);
        return attributeinstance != null ? attributeinstance.getBaseValue() : this.supplier.getBaseValue(pAttribute);
    }

    public double getModifierValue(Holder<Attribute> pAttribute, ResourceLocation pId) {
        AttributeInstance attributeinstance = this.attributes.get(pAttribute);
        return attributeinstance != null ? attributeinstance.getModifier(pId).amount() : this.supplier.getModifierValue(pAttribute, pId);
    }

    public void addTransientAttributeModifiers(Multimap<Holder<Attribute>, AttributeModifier> pModifiers) {
        pModifiers.forEach((p_341286_, p_341287_) -> {
            AttributeInstance attributeinstance = this.getInstance((Holder<Attribute>)p_341286_);
            if (attributeinstance != null) {
                attributeinstance.removeModifier(p_341287_.id());
                attributeinstance.addTransientModifier(p_341287_);
            }
        });
    }

    public void removeAttributeModifiers(Multimap<Holder<Attribute>, AttributeModifier> pModifiers) {
        pModifiers.asMap().forEach((p_341283_, p_341284_) -> {
            AttributeInstance attributeinstance = this.attributes.get(p_341283_);
            if (attributeinstance != null) {
                p_341284_.forEach(p_341289_ -> attributeinstance.removeModifier(p_341289_.id()));
            }
        });
    }

    public void assignAllValues(AttributeMap pMap) {
        pMap.attributes.values().forEach(p_326796_ -> {
            AttributeInstance attributeinstance = this.getInstance(p_326796_.getAttribute());
            if (attributeinstance != null) {
                attributeinstance.replaceFrom(p_326796_);
            }
        });
    }

    public void assignBaseValues(AttributeMap pMap) {
        pMap.attributes.values().forEach(p_341285_ -> {
            AttributeInstance attributeinstance = this.getInstance(p_341285_.getAttribute());
            if (attributeinstance != null) {
                attributeinstance.setBaseValue(p_341285_.getBaseValue());
            }
        });
    }

    public void assignPermanentModifiers(AttributeMap pMap) {
        pMap.attributes.values().forEach(p_358913_ -> {
            AttributeInstance attributeinstance = this.getInstance(p_358913_.getAttribute());
            if (attributeinstance != null) {
                attributeinstance.addPermanentModifiers(p_358913_.getPermanentModifiers());
            }
        });
    }

    public boolean resetBaseValue(Holder<Attribute> pAttribute) {
        if (!this.supplier.hasAttribute(pAttribute)) {
            return false;
        } else {
            AttributeInstance attributeinstance = this.attributes.get(pAttribute);
            if (attributeinstance != null) {
                attributeinstance.setBaseValue(this.supplier.getBaseValue(pAttribute));
            }

            return true;
        }
    }

    public ListTag save() {
        ListTag listtag = new ListTag();

        for (AttributeInstance attributeinstance : this.attributes.values()) {
            listtag.add(attributeinstance.save());
        }

        return listtag;
    }

    public void load(ListTag pNbt) {
        for (int i = 0; i < pNbt.size(); i++) {
            CompoundTag compoundtag = pNbt.getCompoundOrEmpty(i);
            compoundtag.read("id", AttributeInstance.TYPE_CODEC).map(this::getInstance).ifPresent(p_390533_ -> p_390533_.load(compoundtag));
        }
    }
}