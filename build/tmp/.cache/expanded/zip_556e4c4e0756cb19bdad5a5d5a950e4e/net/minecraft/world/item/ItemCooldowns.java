package net.minecraft.world.item;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.component.UseCooldown;

public class ItemCooldowns {
    private final Map<ResourceLocation, ItemCooldowns.CooldownInstance> cooldowns = Maps.newHashMap();
    private int tickCount;

    public boolean isOnCooldown(ItemStack pStack) {
        return this.getCooldownPercent(pStack, 0.0F) > 0.0F;
    }

    public float getCooldownPercent(ItemStack pStack, float pPartialTick) {
        ResourceLocation resourcelocation = this.getCooldownGroup(pStack);
        ItemCooldowns.CooldownInstance itemcooldowns$cooldowninstance = this.cooldowns.get(resourcelocation);
        if (itemcooldowns$cooldowninstance != null) {
            float f = itemcooldowns$cooldowninstance.endTime - itemcooldowns$cooldowninstance.startTime;
            float f1 = itemcooldowns$cooldowninstance.endTime - (this.tickCount + pPartialTick);
            return Mth.clamp(f1 / f, 0.0F, 1.0F);
        } else {
            return 0.0F;
        }
    }

    public void tick() {
        this.tickCount++;
        if (!this.cooldowns.isEmpty()) {
            Iterator<Entry<ResourceLocation, ItemCooldowns.CooldownInstance>> iterator = this.cooldowns.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry<ResourceLocation, ItemCooldowns.CooldownInstance> entry = iterator.next();
                if (entry.getValue().endTime <= this.tickCount) {
                    iterator.remove();
                    this.onCooldownEnded(entry.getKey());
                }
            }
        }
    }

    public ResourceLocation getCooldownGroup(ItemStack pStack) {
        UseCooldown usecooldown = pStack.get(DataComponents.USE_COOLDOWN);
        ResourceLocation resourcelocation = BuiltInRegistries.ITEM.getKey(pStack.getItem());
        return usecooldown == null ? resourcelocation : usecooldown.cooldownGroup().orElse(resourcelocation);
    }

    public void addCooldown(ItemStack pStack, int pCooldown) {
        this.addCooldown(this.getCooldownGroup(pStack), pCooldown);
    }

    public void addCooldown(ResourceLocation pGroup, int pCooldown) {
        this.cooldowns.put(pGroup, new ItemCooldowns.CooldownInstance(this.tickCount, this.tickCount + pCooldown));
        this.onCooldownStarted(pGroup, pCooldown);
    }

    public void removeCooldown(ResourceLocation pGroup) {
        this.cooldowns.remove(pGroup);
        this.onCooldownEnded(pGroup);
    }

    protected void onCooldownStarted(ResourceLocation pGroup, int pCooldown) {
    }

    protected void onCooldownEnded(ResourceLocation pGroup) {
    }

    record CooldownInstance(int startTime, int endTime) {
    }
}