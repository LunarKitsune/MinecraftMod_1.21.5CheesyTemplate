package net.minecraft.client.renderer.culling;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class Frustum {
    public static final int OFFSET_STEP = 4;
    private final FrustumIntersection intersection = new FrustumIntersection();
    private final Matrix4f matrix = new Matrix4f();
    private Vector4f viewVector;
    private double camX;
    private double camY;
    private double camZ;

    public Frustum(Matrix4f pFrustum, Matrix4f pProjection) {
        this.calculateFrustum(pFrustum, pProjection);
    }

    public Frustum(Frustum pOther) {
        this.intersection.set(pOther.matrix);
        this.matrix.set(pOther.matrix);
        this.camX = pOther.camX;
        this.camY = pOther.camY;
        this.camZ = pOther.camZ;
        this.viewVector = pOther.viewVector;
    }

    public Frustum offsetToFullyIncludeCameraCube(int pOffset) {
        double d0 = Math.floor(this.camX / pOffset) * pOffset;
        double d1 = Math.floor(this.camY / pOffset) * pOffset;
        double d2 = Math.floor(this.camZ / pOffset) * pOffset;
        double d3 = Math.ceil(this.camX / pOffset) * pOffset;
        double d4 = Math.ceil(this.camY / pOffset) * pOffset;

        for (double d5 = Math.ceil(this.camZ / pOffset) * pOffset;
            this.intersection
                    .intersectAab(
                        (float)(d0 - this.camX),
                        (float)(d1 - this.camY),
                        (float)(d2 - this.camZ),
                        (float)(d3 - this.camX),
                        (float)(d4 - this.camY),
                        (float)(d5 - this.camZ)
                    )
                != -2;
            this.camZ = this.camZ - this.viewVector.z() * 4.0F
        ) {
            this.camX = this.camX - this.viewVector.x() * 4.0F;
            this.camY = this.camY - this.viewVector.y() * 4.0F;
        }

        return this;
    }

    public void prepare(double pCamX, double pCamY, double pCamZ) {
        this.camX = pCamX;
        this.camY = pCamY;
        this.camZ = pCamZ;
    }

    private void calculateFrustum(Matrix4f pFrustum, Matrix4f pProjection) {
        pProjection.mul(pFrustum, this.matrix);
        this.intersection.set(this.matrix);
        this.viewVector = this.matrix.transformTranspose(new Vector4f(0.0F, 0.0F, 1.0F, 0.0F));
    }

    public boolean isVisible(AABB pAabb) {
        // Forge: exit early for infinite bounds, these would otherwise fail in the intersection test at certain camera angles (GH-9321)
        if (pAabb.equals(net.minecraftforge.common.extensions.IForgeBlockEntity.INFINITE_EXTENT_AABB)) return true;
        int i = this.cubeInFrustum(pAabb.minX, pAabb.minY, pAabb.minZ, pAabb.maxX, pAabb.maxY, pAabb.maxZ);
        return i == -2 || i == -1;
    }

    public int cubeInFrustum(BoundingBox pBoundingBox) {
        return this.cubeInFrustum(
            pBoundingBox.minX(),
            pBoundingBox.minY(),
            pBoundingBox.minZ(),
            pBoundingBox.maxX() + 1,
            pBoundingBox.maxY() + 1,
            pBoundingBox.maxZ() + 1
        );
    }

    private int cubeInFrustum(double pMinX, double pMinY, double pMinZ, double pMaxX, double pMaxY, double pMaxZ) {
        float f = (float)(pMinX - this.camX);
        float f1 = (float)(pMinY - this.camY);
        float f2 = (float)(pMinZ - this.camZ);
        float f3 = (float)(pMaxX - this.camX);
        float f4 = (float)(pMaxY - this.camY);
        float f5 = (float)(pMaxZ - this.camZ);
        return this.intersection.intersectAab(f, f1, f2, f3, f4, f5);
    }

    public Vector4f[] getFrustumPoints() {
        Vector4f[] avector4f = new Vector4f[]{
            new Vector4f(-1.0F, -1.0F, -1.0F, 1.0F),
            new Vector4f(1.0F, -1.0F, -1.0F, 1.0F),
            new Vector4f(1.0F, 1.0F, -1.0F, 1.0F),
            new Vector4f(-1.0F, 1.0F, -1.0F, 1.0F),
            new Vector4f(-1.0F, -1.0F, 1.0F, 1.0F),
            new Vector4f(1.0F, -1.0F, 1.0F, 1.0F),
            new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
            new Vector4f(-1.0F, 1.0F, 1.0F, 1.0F)
        };
        Matrix4f matrix4f = this.matrix.invert(new Matrix4f());

        for (int i = 0; i < 8; i++) {
            matrix4f.transform(avector4f[i]);
            avector4f[i].div(avector4f[i].w());
        }

        return avector4f;
    }

    public double getCamX() {
        return this.camX;
    }

    public double getCamY() {
        return this.camY;
    }

    public double getCamZ() {
        return this.camZ;
    }
}
