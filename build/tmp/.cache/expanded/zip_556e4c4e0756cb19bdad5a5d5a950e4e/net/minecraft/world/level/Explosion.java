package net.minecraft.world.level;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

public interface Explosion {
    static DamageSource getDefaultDamageSource(Level pLevel, @Nullable Entity pSource) {
        return pLevel.damageSources().explosion(pSource, getIndirectSourceEntity(pSource));
    }

    @Nullable
    static LivingEntity getIndirectSourceEntity(@Nullable Entity pSource) {
        return switch (pSource) {
            case PrimedTnt primedtnt -> primedtnt.getOwner();
            case LivingEntity livingentity -> livingentity;
            case Projectile projectile when projectile.getOwner() instanceof LivingEntity livingentity1 -> livingentity1;
            case null, default -> null;
        };
    }

    ServerLevel level();

    Explosion.BlockInteraction getBlockInteraction();

    @Nullable
    LivingEntity getIndirectSourceEntity();

    @Nullable
    Entity getDirectSourceEntity();

    float radius();

    Vec3 center();

    boolean canTriggerBlocks();

    boolean shouldAffectBlocklikeEntities();

    public static enum BlockInteraction {
        KEEP(false),
        DESTROY(true),
        DESTROY_WITH_DECAY(true),
        TRIGGER_BLOCK(false);

        private final boolean shouldAffectBlocklikeEntities;

        private BlockInteraction(final boolean pShouldAffectBlocklikeEntities) {
            this.shouldAffectBlocklikeEntities = pShouldAffectBlocklikeEntities;
        }

        public boolean shouldAffectBlocklikeEntities() {
            return this.shouldAffectBlocklikeEntities;
        }
    }
}