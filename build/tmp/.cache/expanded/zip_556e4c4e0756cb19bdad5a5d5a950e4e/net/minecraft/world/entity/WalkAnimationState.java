package net.minecraft.world.entity;

import net.minecraft.util.Mth;

public class WalkAnimationState {
    private float speedOld;
    private float speed;
    private float position;
    private float positionScale = 1.0F;

    public void setSpeed(float pSpeed) {
        this.speed = pSpeed;
    }

    public void update(float pMovementSpeed, float pMultiplier, float pPositionScale) {
        this.speedOld = this.speed;
        this.speed = this.speed + (pMovementSpeed - this.speed) * pMultiplier;
        this.position = this.position + this.speed;
        this.positionScale = pPositionScale;
    }

    public void stop() {
        this.speedOld = 0.0F;
        this.speed = 0.0F;
        this.position = 0.0F;
    }

    public float speed() {
        return this.speed;
    }

    public float speed(float pPartialTick) {
        return Math.min(Mth.lerp(pPartialTick, this.speedOld, this.speed), 1.0F);
    }

    public float position() {
        return this.position * this.positionScale;
    }

    public float position(float pPartialTick) {
        return (this.position - this.speed * (1.0F - pPartialTick)) * this.positionScale;
    }

    public boolean isMoving() {
        return this.speed > 1.0E-5F;
    }
}