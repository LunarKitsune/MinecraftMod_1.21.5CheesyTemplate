package net.minecraft.world.entity.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

public abstract class MinecartBehavior {
    protected final AbstractMinecart minecart;

    protected MinecartBehavior(AbstractMinecart pMinecart) {
        this.minecart = pMinecart;
    }

    public InterpolationHandler getInterpolation() {
        return null;
    }

    public void lerpMotion(double pX, double pY, double pZ) {
        this.setDeltaMovement(pX, pY, pZ);
    }

    public abstract void tick();

    public Level level() {
        return this.minecart.level();
    }

    public abstract void moveAlongTrack(ServerLevel pLevel);

    public abstract double stepAlongTrack(BlockPos pPos, RailShape pRailShape, double pSpeed);

    public abstract boolean pushAndPickupEntities();

    public Vec3 getDeltaMovement() {
        return this.minecart.getDeltaMovement();
    }

    public void setDeltaMovement(Vec3 pDeltaMovement) {
        this.minecart.setDeltaMovement(pDeltaMovement);
    }

    public void setDeltaMovement(double pX, double pY, double pZ) {
        this.minecart.setDeltaMovement(pX, pY, pZ);
    }

    public Vec3 position() {
        return this.minecart.position();
    }

    public double getX() {
        return this.minecart.getX();
    }

    public double getY() {
        return this.minecart.getY();
    }

    public double getZ() {
        return this.minecart.getZ();
    }

    public void setPos(Vec3 pPos) {
        this.minecart.setPos(pPos);
    }

    public void setPos(double pX, double pY, double pZ) {
        this.minecart.setPos(pX, pY, pZ);
    }

    public float getXRot() {
        return this.minecart.getXRot();
    }

    public void setXRot(float pXRot) {
        this.minecart.setXRot(pXRot);
    }

    public float getYRot() {
        return this.minecart.getYRot();
    }

    public void setYRot(float pYRot) {
        this.minecart.setYRot(pYRot);
    }

    public Direction getMotionDirection() {
        return this.minecart.getDirection();
    }

    public Vec3 getKnownMovement(Vec3 pMovement) {
        return pMovement;
    }

    public abstract double getMaxSpeed(ServerLevel pLevel);

    public abstract double getSlowdownFactor();
}