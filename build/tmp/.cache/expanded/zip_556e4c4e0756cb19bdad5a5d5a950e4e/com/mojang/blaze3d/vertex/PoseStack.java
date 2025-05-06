package com.mojang.blaze3d.vertex;

import com.mojang.math.MatrixUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class PoseStack implements net.minecraftforge.client.extensions.IForgePoseStack {
    private final List<PoseStack.Pose> poses = new ArrayList<>(16);
    private int lastIndex;

    public PoseStack() {
        this.poses.add(new PoseStack.Pose());
    }

    public void translate(double pX, double pY, double pZ) {
        this.translate((float)pX, (float)pY, (float)pZ);
    }

    public void translate(float pX, float pY, float pZ) {
        this.last().translate(pX, pY, pZ);
    }

    public void translate(Vec3 pVector) {
        this.translate(pVector.x, pVector.y, pVector.z);
    }

    public void scale(float pX, float pY, float pZ) {
        this.last().scale(pX, pY, pZ);
    }

    public void mulPose(Quaternionfc pPose) {
        this.last().rotate(pPose);
    }

    public void rotateAround(Quaternionfc pQuaternion, float pX, float pY, float pZ) {
        this.last().rotateAround(pQuaternion, pX, pY, pZ);
    }

    public void pushPose() {
        PoseStack.Pose posestack$pose = this.last();
        this.lastIndex++;
        if (this.lastIndex >= this.poses.size()) {
            this.poses.add(posestack$pose.copy());
        } else {
            this.poses.get(this.lastIndex).set(posestack$pose);
        }
    }

    public void popPose() {
        if (this.lastIndex == 0) {
            throw new NoSuchElementException();
        } else {
            this.lastIndex--;
        }
    }

    public PoseStack.Pose last() {
        return this.poses.get(this.lastIndex);
    }

    public boolean isEmpty() {
        return this.lastIndex == 0;
    }

    public void setIdentity() {
        this.last().setIdentity();
    }

    public void mulPose(Matrix4fc pPose) {
        this.last().mulPose(pPose);
    }

    @OnlyIn(Dist.CLIENT)
    public static final class Pose {
        private final Matrix4f pose = new Matrix4f();
        private final Matrix3f normal = new Matrix3f();
        private boolean trustedNormals = true;

        private void computeNormalMatrix() {
            this.normal.set(this.pose).invert().transpose();
            this.trustedNormals = false;
        }

        void set(PoseStack.Pose pPose) {
            this.pose.set(pPose.pose);
            this.normal.set(pPose.normal);
            this.trustedNormals = pPose.trustedNormals;
        }

        public Matrix4f pose() {
            return this.pose;
        }

        public Matrix3f normal() {
            return this.normal;
        }

        public Vector3f transformNormal(Vector3fc pPos, Vector3f pDestination) {
            return this.transformNormal(pPos.x(), pPos.y(), pPos.z(), pDestination);
        }

        public Vector3f transformNormal(float pX, float pY, float pZ, Vector3f pDestination) {
            Vector3f vector3f = this.normal.transform(pX, pY, pZ, pDestination);
            return this.trustedNormals ? vector3f : vector3f.normalize();
        }

        public Matrix4f translate(float pX, float pY, float pZ) {
            return this.pose.translate(pX, pY, pZ);
        }

        public void scale(float pX, float pY, float pZ) {
            this.pose.scale(pX, pY, pZ);
            if (Math.abs(pX) == Math.abs(pY) && Math.abs(pY) == Math.abs(pZ)) {
                if (pX < 0.0F || pY < 0.0F || pZ < 0.0F) {
                    this.normal.scale(Math.signum(pX), Math.signum(pY), Math.signum(pZ));
                }
            } else {
                this.normal.scale(1.0F / pX, 1.0F / pY, 1.0F / pZ);
                this.trustedNormals = false;
            }
        }

        public void rotate(Quaternionfc pPose) {
            this.pose.rotate(pPose);
            this.normal.rotate(pPose);
        }

        public void rotateAround(Quaternionfc pPose, float pX, float pY, float pZ) {
            this.pose.rotateAround(pPose, pX, pY, pZ);
            this.normal.rotate(pPose);
        }

        public void setIdentity() {
            this.pose.identity();
            this.normal.identity();
            this.trustedNormals = true;
        }

        public void mulPose(Matrix4fc pPose) {
            this.pose.mul(pPose);
            if (!MatrixUtil.isPureTranslation(pPose)) {
                if (MatrixUtil.isOrthonormal(pPose)) {
                    this.normal.mul(new Matrix3f(pPose));
                } else {
                    this.computeNormalMatrix();
                }
            }
        }

        public PoseStack.Pose copy() {
            PoseStack.Pose posestack$pose = new PoseStack.Pose();
            posestack$pose.set(this);
            return posestack$pose;
        }
    }
}
