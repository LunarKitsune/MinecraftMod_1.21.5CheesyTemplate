package net.minecraft.client.renderer.block.model;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.math.MatrixUtil;
import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class FaceBakery {
    public static final int VERTEX_INT_SIZE = 8;
    private static final float RESCALE_22_5 = 1.0F / (float)Math.cos((float) (Math.PI / 8)) - 1.0F;
    private static final float RESCALE_45 = 1.0F / (float)Math.cos((float) (Math.PI / 4)) - 1.0F;
    public static final int VERTEX_COUNT = 4;
    private static final int COLOR_INDEX = 3;
    public static final int UV_INDEX = 4;

    @VisibleForTesting
    static BlockElementFace.UVs defaultFaceUV(Vector3fc pPosFrom, Vector3fc pPosTo, Direction pFacing) {
        return switch (pFacing) {
            case DOWN -> new BlockElementFace.UVs(pPosFrom.x(), 16.0F - pPosTo.z(), pPosTo.x(), 16.0F - pPosFrom.z());
            case UP -> new BlockElementFace.UVs(pPosFrom.x(), pPosFrom.z(), pPosTo.x(), pPosTo.z());
            case NORTH -> new BlockElementFace.UVs(16.0F - pPosTo.x(), 16.0F - pPosTo.y(), 16.0F - pPosFrom.x(), 16.0F - pPosFrom.y());
            case SOUTH -> new BlockElementFace.UVs(pPosFrom.x(), 16.0F - pPosTo.y(), pPosTo.x(), 16.0F - pPosFrom.y());
            case WEST -> new BlockElementFace.UVs(pPosFrom.z(), 16.0F - pPosTo.y(), pPosTo.z(), 16.0F - pPosFrom.y());
            case EAST -> new BlockElementFace.UVs(16.0F - pPosTo.z(), 16.0F - pPosTo.y(), 16.0F - pPosFrom.z(), 16.0F - pPosFrom.y());
        };
    }

    public static BakedQuad bakeQuad(
        Vector3fc pPosFrom,
        Vector3fc pPosTo,
        BlockElementFace pFace,
        TextureAtlasSprite pSprite,
        Direction pFacing,
        ModelState pModelState,
        @Nullable BlockElementRotation pRotation,
        boolean pShade,
        int pLightEmission
    ) {
        BlockElementFace.UVs blockelementface$uvs = pFace.uvs();
        if (blockelementface$uvs == null) {
            blockelementface$uvs = defaultFaceUV(pPosFrom, pPosTo, pFacing);
        }

        blockelementface$uvs = shrinkUVs(pSprite, blockelementface$uvs);
        Matrix4fc matrix4fc = pModelState.inverseFaceTransformation(pFacing);
        int[] aint = makeVertices(
            blockelementface$uvs, pFace.rotation(), matrix4fc, pSprite, pFacing, setupShape(pPosFrom, pPosTo), pModelState.transformation(), pRotation
        );
        Direction direction = calculateFacing(aint);
        if (pRotation == null) {
            recalculateWinding(aint, direction);
        }

        var data = pFace.data();
        var quad = new BakedQuad(aint, pFace.tintIndex(), direction, pSprite, pShade, pLightEmission, data.ambientOcclusion());
        if (!net.minecraftforge.client.model.ForgeFaceData.DEFAULT.equals(data)) {
           quad = net.minecraftforge.client.model.QuadTransformers.applyingLightmap(data.blockLight(), data.skyLight())
               .andThen(net.minecraftforge.client.model.QuadTransformers.applyingColor(data.color()))
               .process(quad);
        }
        return quad;
    }

    private static BlockElementFace.UVs shrinkUVs(TextureAtlasSprite pSprite, BlockElementFace.UVs pUvs) {
        float f = pUvs.minU();
        float f1 = pUvs.minV();
        float f2 = pUvs.maxU();
        float f3 = pUvs.maxV();
        float f4 = pSprite.uvShrinkRatio();
        float f5 = (f + f + f2 + f2) / 4.0F;
        float f6 = (f1 + f1 + f3 + f3) / 4.0F;
        return new BlockElementFace.UVs(Mth.lerp(f4, f, f5), Mth.lerp(f4, f1, f6), Mth.lerp(f4, f2, f5), Mth.lerp(f4, f3, f6));
    }

    private static int[] makeVertices(
        BlockElementFace.UVs pUvs,
        Quadrant pRotation,
        Matrix4fc pInverseFaceTransform,
        TextureAtlasSprite pSprite,
        Direction pFacing,
        float[] pShape,
        Transformation pTransformation,
        @Nullable BlockElementRotation pPartRotation
    ) {
        FaceInfo faceinfo = FaceInfo.fromFacing(pFacing);
        int[] aint = new int[32];

        for (int i = 0; i < 4; i++) {
            bakeVertex(aint, i, faceinfo, pUvs, pRotation, pInverseFaceTransform, pShape, pSprite, pTransformation, pPartRotation);
        }

        return aint;
    }

    private static float[] setupShape(Vector3fc pPosFrom, Vector3fc pPosTo) {
        float[] afloat = new float[Direction.values().length];
        afloat[FaceInfo.Constants.MIN_X] = pPosFrom.x() / 16.0F;
        afloat[FaceInfo.Constants.MIN_Y] = pPosFrom.y() / 16.0F;
        afloat[FaceInfo.Constants.MIN_Z] = pPosFrom.z() / 16.0F;
        afloat[FaceInfo.Constants.MAX_X] = pPosTo.x() / 16.0F;
        afloat[FaceInfo.Constants.MAX_Y] = pPosTo.y() / 16.0F;
        afloat[FaceInfo.Constants.MAX_Z] = pPosTo.z() / 16.0F;
        return afloat;
    }

    private static void bakeVertex(
        int[] pVertexData,
        int pVertexIndex,
        FaceInfo pFaceInfo,
        BlockElementFace.UVs pUvs,
        Quadrant pRotation,
        Matrix4fc pInverseFaceTransform,
        float[] pShape,
        TextureAtlasSprite pSprite,
        Transformation pTransformation,
        @Nullable BlockElementRotation pPartRotation
    ) {
        FaceInfo.VertexInfo faceinfo$vertexinfo = pFaceInfo.getVertexInfo(pVertexIndex);
        Vector3f vector3f = new Vector3f(
            pShape[faceinfo$vertexinfo.xFace], pShape[faceinfo$vertexinfo.yFace], pShape[faceinfo$vertexinfo.zFace]
        );
        applyElementRotation(vector3f, pPartRotation);
        applyModelRotation(vector3f, pTransformation);
        float f = BlockElementFace.getU(pUvs, pRotation, pVertexIndex);
        float f1 = BlockElementFace.getV(pUvs, pRotation, pVertexIndex);
        float f2;
        float f3;
        if (MatrixUtil.isIdentity(pInverseFaceTransform)) {
            f3 = f;
            f2 = f1;
        } else {
            Vector3f vector3f1 = pInverseFaceTransform.transformPosition(new Vector3f(cornerToCenter(f), cornerToCenter(f1), 0.0F));
            f3 = centerToCorner(vector3f1.x);
            f2 = centerToCorner(vector3f1.y);
        }

        fillVertex(pVertexData, pVertexIndex, vector3f, pSprite, f3, f2);
    }

    private static float cornerToCenter(float pCoord) {
        return pCoord - 0.5F;
    }

    private static float centerToCorner(float pCoord) {
        return pCoord + 0.5F;
    }

    private static void fillVertex(int[] pVertexData, int pVertexIndex, Vector3f pPos, TextureAtlasSprite pSprite, float pU, float pV) {
        int i = pVertexIndex * 8;
        pVertexData[i] = Float.floatToRawIntBits(pPos.x());
        pVertexData[i + 1] = Float.floatToRawIntBits(pPos.y());
        pVertexData[i + 2] = Float.floatToRawIntBits(pPos.z());
        pVertexData[i + 3] = -1;
        pVertexData[i + 4] = Float.floatToRawIntBits(pSprite.getU(pU));
        pVertexData[i + 4 + 1] = Float.floatToRawIntBits(pSprite.getV(pV));
    }

    private static void applyElementRotation(Vector3f pVec, @Nullable BlockElementRotation pPartRotation) {
        if (pPartRotation != null) {
            Vector3f vector3f;
            Vector3f vector3f1;
            switch (pPartRotation.axis()) {
                case X:
                    vector3f = new Vector3f(1.0F, 0.0F, 0.0F);
                    vector3f1 = new Vector3f(0.0F, 1.0F, 1.0F);
                    break;
                case Y:
                    vector3f = new Vector3f(0.0F, 1.0F, 0.0F);
                    vector3f1 = new Vector3f(1.0F, 0.0F, 1.0F);
                    break;
                case Z:
                    vector3f = new Vector3f(0.0F, 0.0F, 1.0F);
                    vector3f1 = new Vector3f(1.0F, 1.0F, 0.0F);
                    break;
                default:
                    throw new IllegalArgumentException("There are only 3 axes");
            }

            Quaternionf quaternionf = new Quaternionf().rotationAxis(pPartRotation.angle() * (float) (Math.PI / 180.0), vector3f);
            if (pPartRotation.rescale()) {
                if (Math.abs(pPartRotation.angle()) == 22.5F) {
                    vector3f1.mul(RESCALE_22_5);
                } else {
                    vector3f1.mul(RESCALE_45);
                }

                vector3f1.add(1.0F, 1.0F, 1.0F);
            } else {
                vector3f1.set(1.0F, 1.0F, 1.0F);
            }

            rotateVertexBy(pVec, new Vector3f(pPartRotation.origin()), new Matrix4f().rotation(quaternionf), vector3f1);
        }
    }

    private static void applyModelRotation(Vector3f pPos, Transformation pTransform) {
        if (pTransform != Transformation.identity()) {
            rotateVertexBy(pPos, new Vector3f(0.5F, 0.5F, 0.5F), pTransform.getMatrix(), new Vector3f(1.0F, 1.0F, 1.0F));
        }
    }

    private static void rotateVertexBy(Vector3f pPos, Vector3fc pOrigin, Matrix4fc pTransform, Vector3fc pScale) {
        Vector4f vector4f = pTransform.transform(new Vector4f(pPos.x() - pOrigin.x(), pPos.y() - pOrigin.y(), pPos.z() - pOrigin.z(), 1.0F));
        vector4f.mul(new Vector4f(pScale, 1.0F));
        pPos.set(vector4f.x() + pOrigin.x(), vector4f.y() + pOrigin.y(), vector4f.z() + pOrigin.z());
    }

    private static Direction calculateFacing(int[] pFaceData) {
        Vector3f vector3f = vectorFromData(pFaceData, 0);
        Vector3f vector3f1 = vectorFromData(pFaceData, 8);
        Vector3f vector3f2 = vectorFromData(pFaceData, 16);
        Vector3f vector3f3 = new Vector3f(vector3f).sub(vector3f1);
        Vector3f vector3f4 = new Vector3f(vector3f2).sub(vector3f1);
        Vector3f vector3f5 = new Vector3f(vector3f4).cross(vector3f3).normalize();
        if (!vector3f5.isFinite()) {
            return Direction.UP;
        } else {
            Direction direction = null;
            float f = 0.0F;

            for (Direction direction1 : Direction.values()) {
                float f1 = vector3f5.dot(direction1.getUnitVec3f());
                if (f1 >= 0.0F && f1 > f) {
                    f = f1;
                    direction = direction1;
                }
            }

            return direction == null ? Direction.UP : direction;
        }
    }

    private static float xFromData(int[] pFaceData, int pIndex) {
        return Float.intBitsToFloat(pFaceData[pIndex]);
    }

    private static float yFromData(int[] pFaceData, int pIndex) {
        return Float.intBitsToFloat(pFaceData[pIndex + 1]);
    }

    private static float zFromData(int[] pFaceData, int pIndex) {
        return Float.intBitsToFloat(pFaceData[pIndex + 2]);
    }

    private static Vector3f vectorFromData(int[] pFaceData, int pIndex) {
        return new Vector3f(xFromData(pFaceData, pIndex), yFromData(pFaceData, pIndex), zFromData(pFaceData, pIndex));
    }

    private static void recalculateWinding(int[] pVertices, Direction pDirection) {
        int[] aint = new int[pVertices.length];
        System.arraycopy(pVertices, 0, aint, 0, pVertices.length);
        float[] afloat = new float[Direction.values().length];
        afloat[FaceInfo.Constants.MIN_X] = 999.0F;
        afloat[FaceInfo.Constants.MIN_Y] = 999.0F;
        afloat[FaceInfo.Constants.MIN_Z] = 999.0F;
        afloat[FaceInfo.Constants.MAX_X] = -999.0F;
        afloat[FaceInfo.Constants.MAX_Y] = -999.0F;
        afloat[FaceInfo.Constants.MAX_Z] = -999.0F;

        for (int i = 0; i < 4; i++) {
            int j = 8 * i;
            float f = xFromData(aint, j);
            float f1 = yFromData(aint, j);
            float f2 = zFromData(aint, j);
            if (f < afloat[FaceInfo.Constants.MIN_X]) {
                afloat[FaceInfo.Constants.MIN_X] = f;
            }

            if (f1 < afloat[FaceInfo.Constants.MIN_Y]) {
                afloat[FaceInfo.Constants.MIN_Y] = f1;
            }

            if (f2 < afloat[FaceInfo.Constants.MIN_Z]) {
                afloat[FaceInfo.Constants.MIN_Z] = f2;
            }

            if (f > afloat[FaceInfo.Constants.MAX_X]) {
                afloat[FaceInfo.Constants.MAX_X] = f;
            }

            if (f1 > afloat[FaceInfo.Constants.MAX_Y]) {
                afloat[FaceInfo.Constants.MAX_Y] = f1;
            }

            if (f2 > afloat[FaceInfo.Constants.MAX_Z]) {
                afloat[FaceInfo.Constants.MAX_Z] = f2;
            }
        }

        FaceInfo faceinfo = FaceInfo.fromFacing(pDirection);

        for (int i1 = 0; i1 < 4; i1++) {
            int j1 = 8 * i1;
            FaceInfo.VertexInfo faceinfo$vertexinfo = faceinfo.getVertexInfo(i1);
            float f8 = afloat[faceinfo$vertexinfo.xFace];
            float f3 = afloat[faceinfo$vertexinfo.yFace];
            float f4 = afloat[faceinfo$vertexinfo.zFace];
            pVertices[j1] = Float.floatToRawIntBits(f8);
            pVertices[j1 + 1] = Float.floatToRawIntBits(f3);
            pVertices[j1 + 2] = Float.floatToRawIntBits(f4);

            for (int k = 0; k < 4; k++) {
                int l = 8 * k;
                float f5 = xFromData(aint, l);
                float f6 = yFromData(aint, l);
                float f7 = zFromData(aint, l);
                if (Mth.equal(f8, f5) && Mth.equal(f3, f6) && Mth.equal(f4, f7)) {
                    pVertices[j1 + 4] = aint[l + 4];
                    pVertices[j1 + 4 + 1] = aint[l + 4 + 1];
                }
            }
        }
    }

    public static void extractPositions(int[] pFaceData, Consumer<Vector3f> pOutput) {
        for (int i = 0; i < 4; i++) {
            pOutput.accept(vectorFromData(pFaceData, 8 * i));
        }
    }
}
