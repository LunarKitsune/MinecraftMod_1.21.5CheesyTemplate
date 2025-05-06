package net.minecraft.world.level.portal;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;

public record TeleportTransition(
    ServerLevel newLevel,
    Vec3 position,
    Vec3 deltaMovement,
    float yRot,
    float xRot,
    boolean missingRespawnBlock,
    boolean asPassenger,
    Set<Relative> relatives,
    TeleportTransition.PostTeleportTransition postTeleportTransition
) {
    public static final TeleportTransition.PostTeleportTransition DO_NOTHING = p_360923_ -> {};
    public static final TeleportTransition.PostTeleportTransition PLAY_PORTAL_SOUND = TeleportTransition::playPortalSound;
    public static final TeleportTransition.PostTeleportTransition PLACE_PORTAL_TICKET = TeleportTransition::placePortalTicket;

    public TeleportTransition(
        ServerLevel pNewLevel, Vec3 pPosition, Vec3 pDeltaMovement, float pYRot, float pXRot, TeleportTransition.PostTeleportTransition pPostTeleportTransition
    ) {
        this(pNewLevel, pPosition, pDeltaMovement, pYRot, pXRot, Set.of(), pPostTeleportTransition);
    }

    public TeleportTransition(
        ServerLevel pNewLevel,
        Vec3 pPosition,
        Vec3 pDeltaMovement,
        float pYRot,
        float pXRot,
        Set<Relative> pRelatives,
        TeleportTransition.PostTeleportTransition pPostTeleportTransition
    ) {
        this(pNewLevel, pPosition, pDeltaMovement, pYRot, pXRot, false, false, pRelatives, pPostTeleportTransition);
    }

    public TeleportTransition(ServerLevel pLevel, Entity pEntity, TeleportTransition.PostTeleportTransition pPostTeleportTransition) {
        this(pLevel, findAdjustedSharedSpawnPos(pLevel, pEntity), Vec3.ZERO, 0.0F, 0.0F, false, false, Set.of(), pPostTeleportTransition);
    }

    private static void playPortalSound(Entity pEntity) {
        if (pEntity instanceof ServerPlayer serverplayer) {
            serverplayer.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
        }
    }

    private static void placePortalTicket(Entity pEntity) {
        pEntity.placePortalTicket(BlockPos.containing(pEntity.position()));
    }

    public static TeleportTransition missingRespawnBlock(ServerLevel pLevel, Entity pEntity, TeleportTransition.PostTeleportTransition pPostTeleportTransition) {
        return new TeleportTransition(pLevel, findAdjustedSharedSpawnPos(pLevel, pEntity), Vec3.ZERO, 0.0F, 0.0F, true, false, Set.of(), pPostTeleportTransition);
    }

    private static Vec3 findAdjustedSharedSpawnPos(ServerLevel pLevel, Entity pEntity) {
        return pEntity.adjustSpawnLocation(pLevel, pLevel.getSharedSpawnPos()).getBottomCenter();
    }

    public TeleportTransition withRotation(float pYRot, float pXRot) {
        return new TeleportTransition(
            this.newLevel(), this.position(), this.deltaMovement(), pYRot, pXRot, this.missingRespawnBlock(), this.asPassenger(), this.relatives(), this.postTeleportTransition()
        );
    }

    public TeleportTransition withPosition(Vec3 pPosition) {
        return new TeleportTransition(
            this.newLevel(),
            pPosition,
            this.deltaMovement(),
            this.yRot(),
            this.xRot(),
            this.missingRespawnBlock(),
            this.asPassenger(),
            this.relatives(),
            this.postTeleportTransition()
        );
    }

    public TeleportTransition transitionAsPassenger() {
        return new TeleportTransition(
            this.newLevel(),
            this.position(),
            this.deltaMovement(),
            this.yRot(),
            this.xRot(),
            this.missingRespawnBlock(),
            true,
            this.relatives(),
            this.postTeleportTransition()
        );
    }

    @FunctionalInterface
    public interface PostTeleportTransition {
        void onTransition(Entity pEntity);

        default TeleportTransition.PostTeleportTransition then(TeleportTransition.PostTeleportTransition pPostTeleportTransition) {
            return p_362346_ -> {
                this.onTransition(p_362346_);
                pPostTeleportTransition.onTransition(p_362346_);
            };
        }
    }
}