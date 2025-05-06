package net.minecraft.client.renderer.block.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.math.Quadrant;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record BlockElementFace(@Nullable Direction cullForDirection, int tintIndex, String texture, @Nullable BlockElementFace.UVs uvs, Quadrant rotation, @Nullable net.minecraftforge.client.model.ForgeFaceData data) {
    public static final int NO_TINT = -1;

    public BlockElementFace(@Nullable Direction cullForDirection, int tintIndex, String texture, @Nullable BlockElementFace.UVs uvs, Quadrant rotation) {
        this(cullForDirection, tintIndex, texture, uvs, rotation, null);
    }

    @Override
    public net.minecraftforge.client.model.ForgeFaceData data() {
        return this.data == null ? net.minecraftforge.client.model.ForgeFaceData.DEFAULT : this.data;
    }

    public static float getU(BlockElementFace.UVs pUvs, Quadrant pRotation, int pVertexIndex) {
        return pUvs.getVertexU(pRotation.rotateVertexIndex(pVertexIndex)) / 16.0F;
    }

    public static float getV(BlockElementFace.UVs pUvs, Quadrant pRotation, int pVertexIndex) {
        return pUvs.getVertexV(pRotation.rotateVertexIndex(pVertexIndex)) / 16.0F;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Deserializer implements JsonDeserializer<BlockElementFace> {
        private static final int DEFAULT_TINT_INDEX = -1;
        private static final int DEFAULT_ROTATION = 0;

        public BlockElementFace deserialize(JsonElement pJson, Type pType, JsonDeserializationContext pContext) throws JsonParseException {
            JsonObject jsonobject = pJson.getAsJsonObject();
            Direction direction = getCullFacing(jsonobject);
            int i = getTintIndex(jsonobject);
            String s = getTexture(jsonobject);
            BlockElementFace.UVs blockelementface$uvs = getUVs(jsonobject);
            Quadrant quadrant = getRotation(jsonobject);
            return new BlockElementFace(direction, i, s, blockelementface$uvs, quadrant, net.minecraftforge.client.model.ForgeFaceData.read(jsonobject.get("forge_data"), null));
        }

        private static int getTintIndex(JsonObject pJson) {
            return GsonHelper.getAsInt(pJson, "tintindex", -1);
        }

        private static String getTexture(JsonObject pJson) {
            return GsonHelper.getAsString(pJson, "texture");
        }

        @Nullable
        private static Direction getCullFacing(JsonObject pJson) {
            String s = GsonHelper.getAsString(pJson, "cullface", "");
            return Direction.byName(s);
        }

        private static Quadrant getRotation(JsonObject pJson) {
            int i = GsonHelper.getAsInt(pJson, "rotation", 0);
            return Quadrant.parseJson(i);
        }

        @Nullable
        private static BlockElementFace.UVs getUVs(JsonObject pJson) {
            if (!pJson.has("uv")) {
                return null;
            } else {
                JsonArray jsonarray = GsonHelper.getAsJsonArray(pJson, "uv");
                if (jsonarray.size() != 4) {
                    throw new JsonParseException("Expected 4 uv values, found: " + jsonarray.size());
                } else {
                    float f = GsonHelper.convertToFloat(jsonarray.get(0), "minU");
                    float f1 = GsonHelper.convertToFloat(jsonarray.get(1), "minV");
                    float f2 = GsonHelper.convertToFloat(jsonarray.get(2), "maxU");
                    float f3 = GsonHelper.convertToFloat(jsonarray.get(3), "maxV");
                    return new BlockElementFace.UVs(f, f1, f2, f3);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record UVs(float minU, float minV, float maxU, float maxV) {
        public float getVertexU(int pVertexIndex) {
            return pVertexIndex != 0 && pVertexIndex != 1 ? this.maxU : this.minU;
        }

        public float getVertexV(int pVertexIndex) {
            return pVertexIndex != 0 && pVertexIndex != 3 ? this.maxV : this.minV;
        }
    }
}
