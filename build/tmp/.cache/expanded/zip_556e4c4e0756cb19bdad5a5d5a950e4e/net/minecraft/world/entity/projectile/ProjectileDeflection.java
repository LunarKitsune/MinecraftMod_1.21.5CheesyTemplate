package net.minecraft.world.entity.projectile;

import javax.annotation.Nullable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@FunctionalInterface
public interface ProjectileDeflection {
    ProjectileDeflection NONE = (p_335766_, p_335741_, p_334113_) -> {};
    ProjectileDeflection REVERSE = (p_390742_, p_390743_, p_390744_) -> {
        float f = 170.0F + p_390744_.nextFloat() * 20.0F;
        p_390742_.setDeltaMovement(p_390742_.getDeltaMovement().scale(-0.5));
        p_390742_.setYRot(p_390742_.getYRot() + f);
        p_390742_.yRotO += f;
        p_390742_.hasImpulse = true;
    };
    ProjectileDeflection AIM_DEFLECT = (p_375177_, p_375178_, p_375179_) -> {
        if (p_375178_ != null) {
            Vec3 vec3 = p_375178_.getLookAngle().normalize();
            p_375177_.setDeltaMovement(vec3);
            p_375177_.hasImpulse = true;
        }
    };
    ProjectileDeflection MOMENTUM_DEFLECT = (p_375174_, p_375175_, p_375176_) -> {
        if (p_375175_ != null) {
            Vec3 vec3 = p_375175_.getDeltaMovement().normalize();
            p_375174_.setDeltaMovement(vec3);
            p_375174_.hasImpulse = true;
        }
    };

    void deflect(Projectile pProjectile, @Nullable Entity pEntity, RandomSource pRandom);
}