package net.minecraft.world.entity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class EntityAttachments {
    private final Map<EntityAttachment, List<Vec3>> attachments;

    EntityAttachments(Map<EntityAttachment, List<Vec3>> pAttachments) {
        this.attachments = pAttachments;
    }

    public static EntityAttachments createDefault(float pWidth, float pHeight) {
        return builder().build(pWidth, pHeight);
    }

    public static EntityAttachments.Builder builder() {
        return new EntityAttachments.Builder();
    }

    public EntityAttachments scale(float pXScale, float pYScale, float pZScale) {
        return new EntityAttachments(Util.makeEnumMap(EntityAttachment.class, p_395775_ -> {
            List<Vec3> list = new ArrayList<>();

            for (Vec3 vec3 : this.attachments.get(p_395775_)) {
                list.add(vec3.multiply(pXScale, pYScale, pZScale));
            }

            return list;
        }));
    }

    @Nullable
    public Vec3 getNullable(EntityAttachment pAttachment, int pIndex, float pYRot) {
        List<Vec3> list = this.attachments.get(pAttachment);
        return pIndex >= 0 && pIndex < list.size() ? transformPoint(list.get(pIndex), pYRot) : null;
    }

    public Vec3 get(EntityAttachment pAttachment, int pIndex, float pYRot) {
        Vec3 vec3 = this.getNullable(pAttachment, pIndex, pYRot);
        if (vec3 == null) {
            throw new IllegalStateException("Had no attachment point of type: " + pAttachment + " for index: " + pIndex);
        } else {
            return vec3;
        }
    }

    public Vec3 getClamped(EntityAttachment pAttachment, int pIndex, float pYRot) {
        List<Vec3> list = this.attachments.get(pAttachment);
        if (list.isEmpty()) {
            throw new IllegalStateException("Had no attachment points of type: " + pAttachment);
        } else {
            Vec3 vec3 = list.get(Mth.clamp(pIndex, 0, list.size() - 1));
            return transformPoint(vec3, pYRot);
        }
    }

    private static Vec3 transformPoint(Vec3 pPoint, float pYRot) {
        return pPoint.yRot(-pYRot * (float) (Math.PI / 180.0));
    }

    public static class Builder {
        private final Map<EntityAttachment, List<Vec3>> attachments = new EnumMap<>(EntityAttachment.class);

        Builder() {
        }

        public EntityAttachments.Builder attach(EntityAttachment pAttachment, float pX, float pY, float pZ) {
            return this.attach(pAttachment, new Vec3(pX, pY, pZ));
        }

        public EntityAttachments.Builder attach(EntityAttachment pAttachment, Vec3 pPoas) {
            this.attachments.computeIfAbsent(pAttachment, p_333992_ -> new ArrayList<>(1)).add(pPoas);
            return this;
        }

        public EntityAttachments build(float pWidth, float pHeight) {
            Map<EntityAttachment, List<Vec3>> map = Util.makeEnumMap(EntityAttachment.class, p_390488_ -> {
                List<Vec3> list = this.attachments.get(p_390488_);
                return list == null ? p_390488_.createFallbackPoints(pWidth, pHeight) : List.copyOf(list);
            });
            return new EntityAttachments(map);
        }
    }
}