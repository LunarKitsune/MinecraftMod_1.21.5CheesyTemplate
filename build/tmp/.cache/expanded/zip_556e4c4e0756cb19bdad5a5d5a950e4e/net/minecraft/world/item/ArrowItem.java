package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

public class ArrowItem extends Item implements ProjectileItem {
    public ArrowItem(Item.Properties p_40512_) {
        super(p_40512_);
    }

    public AbstractArrow createArrow(Level pLevel, ItemStack pAmmo, LivingEntity pShooter, @Nullable ItemStack pWeapon) {
        return new Arrow(pLevel, pShooter, pAmmo.copyWithCount(1), pWeapon);
    }

    @Override
    public Projectile asProjectile(Level p_330586_, Position p_330823_, ItemStack p_335593_, Direction p_335240_) {
        Arrow arrow = new Arrow(p_330586_, p_330823_.x(), p_330823_.y(), p_330823_.z(), p_335593_.copyWithCount(1), null);
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        return arrow;
    }

    public boolean isInfinite(ItemStack stack, ItemStack bow, net.minecraft.world.entity.LivingEntity owner) {
        return false;
        // Infinity doesnt go on arrows anymore..
        //int enchant = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.INFINITY_ARROWS, bow);
        //return enchant <= 0 ? false : this.getClass() == ArrowItem.class;
    }
}
