package net.minecraft.world.entity;

import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

public record PositionMoveRotation(Vec3 position, Vec3 deltaMovement, float yRot, float xRot) {
    public static final StreamCodec<FriendlyByteBuf, PositionMoveRotation> STREAM_CODEC = StreamCodec.composite(
        Vec3.STREAM_CODEC,
        PositionMoveRotation::position,
        Vec3.STREAM_CODEC,
        PositionMoveRotation::deltaMovement,
        ByteBufCodecs.FLOAT,
        PositionMoveRotation::yRot,
        ByteBufCodecs.FLOAT,
        PositionMoveRotation::xRot,
        PositionMoveRotation::new
    );

    public static PositionMoveRotation of(Entity pEntity) {
        return pEntity.isInterpolating()
            ? new PositionMoveRotation(
                pEntity.getInterpolation().position(), pEntity.getKnownMovement(), pEntity.getInterpolation().yRot(), pEntity.getInterpolation().xRot()
            )
            : new PositionMoveRotation(pEntity.position(), pEntity.getKnownMovement(), pEntity.getYRot(), pEntity.getXRot());
    }

    public static PositionMoveRotation of(TeleportTransition pTeleportTransition) {
        return new PositionMoveRotation(pTeleportTransition.position(), pTeleportTransition.deltaMovement(), pTeleportTransition.yRot(), pTeleportTransition.xRot());
    }

    public static PositionMoveRotation calculateAbsolute(PositionMoveRotation pCurrent, PositionMoveRotation pAfter, Set<Relative> pRelatives) {
        double d0 = pRelatives.contains(Relative.X) ? pCurrent.position.x : 0.0;
        double d1 = pRelatives.contains(Relative.Y) ? pCurrent.position.y : 0.0;
        double d2 = pRelatives.contains(Relative.Z) ? pCurrent.position.z : 0.0;
        float f = pRelatives.contains(Relative.Y_ROT) ? pCurrent.yRot : 0.0F;
        float f1 = pRelatives.contains(Relative.X_ROT) ? pCurrent.xRot : 0.0F;
        Vec3 vec3 = new Vec3(d0 + pAfter.position.x, d1 + pAfter.position.y, d2 + pAfter.position.z);
        float f2 = f + pAfter.yRot;
        float f3 = Mth.clamp(f1 + pAfter.xRot, -90.0F, 90.0F);
        Vec3 vec31 = pCurrent.deltaMovement;
        if (pRelatives.contains(Relative.ROTATE_DELTA)) {
            float f4 = pCurrent.yRot - f2;
            float f5 = pCurrent.xRot - f3;
            vec31 = vec31.xRot((float)Math.toRadians(f5));
            vec31 = vec31.yRot((float)Math.toRadians(f4));
        }

        Vec3 vec32 = new Vec3(
            calculateDelta(vec31.x, pAfter.deltaMovement.x, pRelatives, Relative.DELTA_X),
            calculateDelta(vec31.y, pAfter.deltaMovement.y, pRelatives, Relative.DELTA_Y),
            calculateDelta(vec31.z, pAfter.deltaMovement.z, pRelatives, Relative.DELTA_Z)
        );
        return new PositionMoveRotation(vec3, vec32, f2, f3);
    }

    private static double calculateDelta(double pPosition, double pDeltaMovement, Set<Relative> pRelatives, Relative pDeltaRelative) {
        return pRelatives.contains(pDeltaRelative) ? pPosition + pDeltaMovement : pDeltaMovement;
    }
}