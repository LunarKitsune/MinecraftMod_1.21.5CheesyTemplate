package net.minecraft.client.model.geom;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record PartPose(
    float x, float y, float z, float xRot, float yRot, float zRot, float xScale, float yScale, float zScale
) {
    public static final PartPose ZERO = offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

    public static PartPose offset(float pX, float pY, float pZ) {
        return offsetAndRotation(pX, pY, pZ, 0.0F, 0.0F, 0.0F);
    }

    public static PartPose rotation(float pXRot, float pYRot, float pZRot) {
        return offsetAndRotation(0.0F, 0.0F, 0.0F, pXRot, pYRot, pZRot);
    }

    public static PartPose offsetAndRotation(float pX, float pY, float pZ, float pXRot, float pYRot, float pZRot) {
        return new PartPose(pX, pY, pZ, pXRot, pYRot, pZRot, 1.0F, 1.0F, 1.0F);
    }

    public PartPose translated(float pX, float pY, float pZ) {
        return new PartPose(
            this.x + pX,
            this.y + pY,
            this.z + pZ,
            this.xRot,
            this.yRot,
            this.zRot,
            this.xScale,
            this.yScale,
            this.zScale
        );
    }

    public PartPose withScale(float pScale) {
        return new PartPose(this.x, this.y, this.z, this.xRot, this.yRot, this.zRot, pScale, pScale, pScale);
    }

    public PartPose scaled(float pScale) {
        return pScale == 1.0F ? this : this.scaled(pScale, pScale, pScale);
    }

    public PartPose scaled(float pX, float pY, float pZ) {
        return new PartPose(
            this.x * pX,
            this.y * pY,
            this.z * pZ,
            this.xRot,
            this.yRot,
            this.zRot,
            this.xScale * pX,
            this.yScale * pY,
            this.zScale * pZ
        );
    }
}