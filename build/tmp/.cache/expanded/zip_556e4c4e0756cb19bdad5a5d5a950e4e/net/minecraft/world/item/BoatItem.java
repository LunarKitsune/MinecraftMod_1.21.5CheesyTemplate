package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BoatItem extends Item {
    private final EntityType<? extends AbstractBoat> entityType;

    public BoatItem(EntityType<? extends AbstractBoat> pEntityType, Item.Properties pProperties) {
        super(pProperties);
        this.entityType = pEntityType;
    }

    @Override
    public InteractionResult use(Level p_40622_, Player p_40623_, InteractionHand p_40624_) {
        ItemStack itemstack = p_40623_.getItemInHand(p_40624_);
        HitResult hitresult = getPlayerPOVHitResult(p_40622_, p_40623_, ClipContext.Fluid.ANY);
        if (hitresult.getType() == HitResult.Type.MISS) {
            return InteractionResult.PASS;
        } else {
            Vec3 vec3 = p_40623_.getViewVector(1.0F);
            double d0 = 5.0;
            List<Entity> list = p_40622_.getEntities(p_40623_, p_40623_.getBoundingBox().expandTowards(vec3.scale(5.0)).inflate(1.0), EntitySelector.CAN_BE_PICKED);
            if (!list.isEmpty()) {
                Vec3 vec31 = p_40623_.getEyePosition();

                for (Entity entity : list) {
                    AABB aabb = entity.getBoundingBox().inflate(entity.getPickRadius());
                    if (aabb.contains(vec31)) {
                        return InteractionResult.PASS;
                    }
                }
            }

            if (hitresult.getType() == HitResult.Type.BLOCK) {
                AbstractBoat abstractboat = this.getBoat(p_40622_, hitresult, itemstack, p_40623_);
                if (abstractboat == null) {
                    return InteractionResult.FAIL;
                } else {
                    abstractboat.setYRot(p_40623_.getYRot());
                    if (!p_40622_.noCollision(abstractboat, abstractboat.getBoundingBox())) {
                        return InteractionResult.FAIL;
                    } else {
                        if (!p_40622_.isClientSide) {
                            p_40622_.addFreshEntity(abstractboat);
                            p_40622_.gameEvent(p_40623_, GameEvent.ENTITY_PLACE, hitresult.getLocation());
                            itemstack.consume(1, p_40623_);
                        }

                        p_40623_.awardStat(Stats.ITEM_USED.get(this));
                        return InteractionResult.SUCCESS;
                    }
                }
            } else {
                return InteractionResult.PASS;
            }
        }
    }

    @Nullable
    private AbstractBoat getBoat(Level pLevel, HitResult pHitResult, ItemStack pStack, Player pPlayer) {
        AbstractBoat abstractboat = this.entityType.create(pLevel, EntitySpawnReason.SPAWN_ITEM_USE);
        if (abstractboat != null) {
            Vec3 vec3 = pHitResult.getLocation();
            abstractboat.setInitialPos(vec3.x, vec3.y, vec3.z);
            if (pLevel instanceof ServerLevel serverlevel) {
                EntityType.<AbstractBoat>createDefaultStackConfig(serverlevel, pStack, pPlayer).accept(abstractboat);
            }
        }

        return abstractboat;
    }
}