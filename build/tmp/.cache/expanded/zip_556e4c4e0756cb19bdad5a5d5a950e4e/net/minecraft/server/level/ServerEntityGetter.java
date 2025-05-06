package net.minecraft.server.level;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;

public interface ServerEntityGetter extends EntityGetter {
    ServerLevel getLevel();

    @Nullable
    default Player getNearestPlayer(TargetingConditions pTargetingConditions, LivingEntity pSource) {
        return this.getNearestEntity(this.players(), pTargetingConditions, pSource, pSource.getX(), pSource.getY(), pSource.getZ());
    }

    @Nullable
    default Player getNearestPlayer(TargetingConditions pTargetingConditions, LivingEntity pSource, double pX, double pY, double pZ) {
        return this.getNearestEntity(this.players(), pTargetingConditions, pSource, pX, pY, pZ);
    }

    @Nullable
    default Player getNearestPlayer(TargetingConditions pTargetingConditions, double pX, double pY, double pZ) {
        return this.getNearestEntity(this.players(), pTargetingConditions, null, pX, pY, pZ);
    }

    @Nullable
    default <T extends LivingEntity> T getNearestEntity(
        Class<? extends T> pEntityClass,
        TargetingConditions pTargetingConditions,
        @Nullable LivingEntity pSource,
        double pX,
        double pY,
        double pZ,
        AABB pArea
    ) {
        return this.getNearestEntity(this.getEntitiesOfClass(pEntityClass, pArea, p_369748_ -> true), pTargetingConditions, pSource, pX, pY, pZ);
    }

    @Nullable
    default <T extends LivingEntity> T getNearestEntity(
        List<? extends T> pEntities, TargetingConditions pTargetingConditions, @Nullable LivingEntity pSource, double pX, double pY, double pZ
    ) {
        double d0 = -1.0;
        T t = null;

        for (T t1 : pEntities) {
            if (pTargetingConditions.test(this.getLevel(), pSource, t1)) {
                double d1 = t1.distanceToSqr(pX, pY, pZ);
                if (d0 == -1.0 || d1 < d0) {
                    d0 = d1;
                    t = t1;
                }
            }
        }

        return t;
    }

    default List<Player> getNearbyPlayers(TargetingConditions pTargetingConditions, LivingEntity pSource, AABB pArea) {
        List<Player> list = new ArrayList<>();

        for (Player player : this.players()) {
            if (pArea.contains(player.getX(), player.getY(), player.getZ()) && pTargetingConditions.test(this.getLevel(), pSource, player)) {
                list.add(player);
            }
        }

        return list;
    }

    default <T extends LivingEntity> List<T> getNearbyEntities(Class<T> pEntityClass, TargetingConditions pTargetingConditions, LivingEntity pSource, AABB pArea) {
        List<T> list = this.getEntitiesOfClass(pEntityClass, pArea, p_368152_ -> true);
        List<T> list1 = new ArrayList<>();

        for (T t : list) {
            if (pTargetingConditions.test(this.getLevel(), pSource, t)) {
                list1.add(t);
            }
        }

        return list1;
    }
}