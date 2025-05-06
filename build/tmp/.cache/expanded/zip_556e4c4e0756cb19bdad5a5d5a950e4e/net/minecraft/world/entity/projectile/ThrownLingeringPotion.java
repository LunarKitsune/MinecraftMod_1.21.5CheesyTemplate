package net.minecraft.world.entity.projectile;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class ThrownLingeringPotion extends AbstractThrownPotion {
    public ThrownLingeringPotion(EntityType<? extends ThrownLingeringPotion> p_397034_, Level p_392568_) {
        super(p_397034_, p_392568_);
    }

    public ThrownLingeringPotion(Level pLevel, LivingEntity pOwner, ItemStack pItem) {
        super(EntityType.LINGERING_POTION, pLevel, pOwner, pItem);
    }

    public ThrownLingeringPotion(Level pLevel, double pX, double pY, double pZ, ItemStack pItem) {
        super(EntityType.LINGERING_POTION, pLevel, pX, pY, pZ, pItem);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.LINGERING_POTION;
    }

    @Override
    public void onHitAsPotion(ServerLevel p_394078_, ItemStack p_391393_, @Nullable Entity p_395222_) {
        AreaEffectCloud areaeffectcloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
        if (this.getOwner() instanceof LivingEntity livingentity) {
            areaeffectcloud.setOwner(livingentity);
        }

        areaeffectcloud.setRadius(3.0F);
        areaeffectcloud.setRadiusOnUse(-0.5F);
        areaeffectcloud.setDuration(600);
        areaeffectcloud.setWaitTime(10);
        areaeffectcloud.setRadiusPerTick(-areaeffectcloud.getRadius() / areaeffectcloud.getDuration());
        areaeffectcloud.applyComponentsFromItemStack(p_391393_);
        p_394078_.addFreshEntity(areaeffectcloud);
    }
}