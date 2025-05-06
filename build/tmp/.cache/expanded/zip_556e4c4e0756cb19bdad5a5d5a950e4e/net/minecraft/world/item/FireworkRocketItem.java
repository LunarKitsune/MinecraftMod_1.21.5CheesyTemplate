package net.minecraft.world.item;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FireworkRocketItem extends Item implements ProjectileItem {
    public static final byte[] CRAFTABLE_DURATIONS = new byte[]{1, 2, 3};
    public static final double ROCKET_PLACEMENT_OFFSET = 0.15;

    public FireworkRocketItem(Item.Properties p_41209_) {
        super(p_41209_);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        if (level instanceof ServerLevel serverlevel) {
            ItemStack itemstack = pContext.getItemInHand();
            Vec3 vec3 = pContext.getClickLocation();
            Direction direction = pContext.getClickedFace();
            Projectile.spawnProjectile(
                new FireworkRocketEntity(
                    level,
                    pContext.getPlayer(),
                    vec3.x + direction.getStepX() * 0.15,
                    vec3.y + direction.getStepY() * 0.15,
                    vec3.z + direction.getStepZ() * 0.15,
                    itemstack
                ),
                serverlevel,
                itemstack
            );
            itemstack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level p_41218_, Player p_41219_, InteractionHand p_41220_) {
        if (p_41219_.isFallFlying()) {
            ItemStack itemstack = p_41219_.getItemInHand(p_41220_);
            if (p_41218_ instanceof ServerLevel serverlevel) {
                Projectile.spawnProjectile(new FireworkRocketEntity(p_41218_, itemstack, p_41219_), serverlevel, itemstack);
                itemstack.consume(1, p_41219_);
                p_41219_.awardStat(Stats.ITEM_USED.get(this));
            }

            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public Projectile asProjectile(Level p_335383_, Position p_331917_, ItemStack p_329236_, Direction p_336387_) {
        return new FireworkRocketEntity(p_335383_, p_329236_.copyWithCount(1), p_331917_.x(), p_331917_.y(), p_331917_.z(), true);
    }

    @Override
    public ProjectileItem.DispenseConfig createDispenseConfig() {
        return ProjectileItem.DispenseConfig.builder().positionFunction(FireworkRocketItem::getEntityJustOutsideOfBlockPos).uncertainty(1.0F).power(0.5F).overrideDispenseEvent(1004).build();
    }

    private static Vec3 getEntityJustOutsideOfBlockPos(BlockSource pBlockSource, Direction pDirection) {
        return pBlockSource.center()
            .add(pDirection.getStepX() * 0.5000099999997474, pDirection.getStepY() * 0.5000099999997474, pDirection.getStepZ() * 0.5000099999997474);
    }
}