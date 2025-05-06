package net.minecraft.world.entity;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class InterpolationHandler {
    public static final int DEFAULT_INTERPOLATION_STEPS = 3;
    private final Entity entity;
    private int interpolationSteps;
    private final InterpolationHandler.InterpolationData interpolationData = new InterpolationHandler.InterpolationData(0, Vec3.ZERO, 0.0F, 0.0F);
    @Nullable
    private Vec3 previousTickPosition;
    @Nullable
    private Vec2 previousTickRot;
    @Nullable
    private final Consumer<InterpolationHandler> onInterpolationStart;

    public InterpolationHandler(Entity pEntity) {
        this(pEntity, 3, null);
    }

    public InterpolationHandler(Entity pEntity, int pInterpolationSteps) {
        this(pEntity, pInterpolationSteps, null);
    }

    public InterpolationHandler(Entity pEntity, @Nullable Consumer<InterpolationHandler> pOnInterpolationStart) {
        this(pEntity, 3, pOnInterpolationStart);
    }

    public InterpolationHandler(Entity pEntity, int pInterpolationSteps, @Nullable Consumer<InterpolationHandler> pOnInterpolationStart) {
        this.interpolationSteps = pInterpolationSteps;
        this.entity = pEntity;
        this.onInterpolationStart = pOnInterpolationStart;
    }

    public Vec3 position() {
        return this.interpolationData.steps > 0 ? this.interpolationData.position : this.entity.position();
    }

    public float yRot() {
        return this.interpolationData.steps > 0 ? this.interpolationData.yRot : this.entity.getYRot();
    }

    public float xRot() {
        return this.interpolationData.steps > 0 ? this.interpolationData.xRot : this.entity.getXRot();
    }

    public void interpolateTo(Vec3 pPos, float pYRot, float pXRot) {
        if (this.interpolationSteps == 0) {
            this.entity.snapTo(pPos, pYRot, pXRot);
            this.cancel();
        } else {
            this.interpolationData.steps = this.interpolationSteps;
            this.interpolationData.position = pPos;
            this.interpolationData.yRot = pYRot;
            this.interpolationData.xRot = pXRot;
            this.previousTickPosition = this.entity.position();
            this.previousTickRot = new Vec2(this.entity.getXRot(), this.entity.getYRot());
            if (this.onInterpolationStart != null) {
                this.onInterpolationStart.accept(this);
            }
        }
    }

    public boolean hasActiveInterpolation() {
        return this.interpolationData.steps > 0;
    }

    public void setInterpolationLength(int pInterpolationLength) {
        this.interpolationSteps = pInterpolationLength;
    }

    public void interpolate() {
        if (!this.hasActiveInterpolation()) {
            this.cancel();
        } else {
            double d0 = 1.0 / this.interpolationData.steps;
            if (this.previousTickPosition != null) {
                Vec3 vec3 = this.entity.position().subtract(this.previousTickPosition);
                if (this.entity.level().noCollision(this.entity, this.entity.makeBoundingBox().move(this.interpolationData.position.add(vec3)))) {
                    this.interpolationData.addDelta(vec3);
                }
            }

            if (this.previousTickRot != null) {
                float f3 = this.entity.getYRot() - this.previousTickRot.y;
                float f = this.entity.getXRot() - this.previousTickRot.x;
                this.interpolationData.addRotation(f3, f);
            }

            double d3 = Mth.lerp(d0, this.entity.getX(), this.interpolationData.position.x);
            double d1 = Mth.lerp(d0, this.entity.getY(), this.interpolationData.position.y);
            double d2 = Mth.lerp(d0, this.entity.getZ(), this.interpolationData.position.z);
            Vec3 vec31 = new Vec3(d3, d1, d2);
            float f1 = (float)Mth.rotLerp(d0, this.entity.getYRot(), this.interpolationData.yRot);
            float f2 = (float)Mth.lerp(d0, this.entity.getXRot(), this.interpolationData.xRot);
            this.entity.setPos(vec31);
            this.entity.setRot(f1, f2);
            this.interpolationData.decrease();
            this.previousTickPosition = vec31;
            this.previousTickRot = new Vec2(this.entity.getXRot(), this.entity.getYRot());
        }
    }

    public void cancel() {
        this.interpolationData.steps = 0;
        this.previousTickPosition = null;
        this.previousTickRot = null;
    }

    static class InterpolationData {
        protected int steps;
        Vec3 position;
        float yRot;
        float xRot;

        InterpolationData(int pSteps, Vec3 pPosition, float pYRot, float pXRot) {
            this.steps = pSteps;
            this.position = pPosition;
            this.yRot = pYRot;
            this.xRot = pXRot;
        }

        public void decrease() {
            this.steps--;
        }

        public void addDelta(Vec3 pDelta) {
            this.position = this.position.add(pDelta);
        }

        public void addRotation(float pYRot, float pXRot) {
            this.yRot += pYRot;
            this.xRot += pXRot;
        }
    }
}