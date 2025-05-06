package net.minecraft.world.entity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;

public interface Leashable {
    String LEASH_TAG = "leash";
    double LEASH_TOO_FAR_DIST = 10.0;
    double LEASH_ELASTIC_DIST = 6.0;

    @Nullable
    Leashable.LeashData getLeashData();

    void setLeashData(@Nullable Leashable.LeashData pLeashData);

    default boolean isLeashed() {
        return this.getLeashData() != null && this.getLeashData().leashHolder != null;
    }

    default boolean mayBeLeashed() {
        return this.getLeashData() != null;
    }

    default boolean canHaveALeashAttachedToIt() {
        return this.canBeLeashed() && !this.isLeashed();
    }

    default boolean canBeLeashed() {
        return true;
    }

    default void setDelayedLeashHolderId(int pDelayedLeashHolderId) {
        this.setLeashData(new Leashable.LeashData(pDelayedLeashHolderId));
        dropLeash((Entity & Leashable)this, false, false);
    }

    default void readLeashData(CompoundTag pTag) {
        Leashable.LeashData leashable$leashdata = pTag.read("leash", Leashable.LeashData.CODEC).orElse(null);
        if (this.getLeashData() != null && leashable$leashdata == null) {
            this.removeLeash();
        }

        this.setLeashData(leashable$leashdata);
    }

    default void writeLeashData(CompoundTag pTag, @Nullable Leashable.LeashData pLeashData) {
        pTag.storeNullable("leash", Leashable.LeashData.CODEC, pLeashData);
    }

    private static <E extends Entity & Leashable> void restoreLeashFromSave(E pEntity, Leashable.LeashData pLeashData) {
        if (pLeashData.delayedLeashInfo != null && pEntity.level() instanceof ServerLevel serverlevel) {
            Optional<UUID> optional1 = pLeashData.delayedLeashInfo.left();
            Optional<BlockPos> optional = pLeashData.delayedLeashInfo.right();
            if (optional1.isPresent()) {
                Entity entity = serverlevel.getEntity(optional1.get());
                if (entity != null) {
                    setLeashedTo(pEntity, entity, true);
                    return;
                }
            } else if (optional.isPresent()) {
                setLeashedTo(pEntity, LeashFenceKnotEntity.getOrCreateKnot(serverlevel, optional.get()), true);
                return;
            }

            if (pEntity.tickCount > 100) {
                pEntity.spawnAtLocation(serverlevel, Items.LEAD);
                pEntity.setLeashData(null);
            }
        }
    }

    default void dropLeash() {
        dropLeash((Entity & Leashable)this, true, true);
    }

    default void removeLeash() {
        dropLeash((Entity & Leashable)this, true, false);
    }

    default void onLeashRemoved() {
    }

    private static <E extends Entity & Leashable> void dropLeash(E pEntity, boolean pBroadcastPacket, boolean pDropItem) {
        Leashable.LeashData leashable$leashdata = pEntity.getLeashData();
        if (leashable$leashdata != null && leashable$leashdata.leashHolder != null) {
            pEntity.setLeashData(null);
            pEntity.onLeashRemoved();
            if (pEntity.level() instanceof ServerLevel serverlevel) {
                if (pDropItem) {
                    pEntity.spawnAtLocation(serverlevel, Items.LEAD);
                }

                if (pBroadcastPacket) {
                    serverlevel.getChunkSource().broadcast(pEntity, new ClientboundSetEntityLinkPacket(pEntity, null));
                }
            }
        }
    }

    static <E extends Entity & Leashable> void tickLeash(ServerLevel pLevel, E pEntity) {
        Leashable.LeashData leashable$leashdata = pEntity.getLeashData();
        if (leashable$leashdata != null && leashable$leashdata.delayedLeashInfo != null) {
            restoreLeashFromSave(pEntity, leashable$leashdata);
        }

        if (leashable$leashdata != null && leashable$leashdata.leashHolder != null) {
            if (!pEntity.isAlive() || !leashable$leashdata.leashHolder.isAlive()) {
                if (pLevel.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                    pEntity.dropLeash();
                } else {
                    pEntity.removeLeash();
                }
            }

            Entity entity = pEntity.getLeashHolder();
            if (entity != null && entity.level() == pEntity.level()) {
                float f = pEntity.distanceTo(entity);
                if (!pEntity.handleLeashAtDistance(entity, f)) {
                    return;
                }

                if (f > 10.0) {
                    pEntity.leashTooFarBehaviour();
                } else if (f > 6.0) {
                    pEntity.elasticRangeLeashBehaviour(entity, f);
                    pEntity.checkSlowFallDistance();
                } else {
                    pEntity.closeRangeLeashBehaviour(entity);
                }
            }
        }
    }

    default boolean handleLeashAtDistance(Entity pLeashHolder, float pDistance) {
        return true;
    }

    default void leashTooFarBehaviour() {
        this.dropLeash();
    }

    default void closeRangeLeashBehaviour(Entity pEntity) {
    }

    default void elasticRangeLeashBehaviour(Entity pLeashHolder, float pDistance) {
        legacyElasticRangeLeashBehaviour((Entity & Leashable)this, pLeashHolder, pDistance);
    }

    private static <E extends Entity & Leashable> void legacyElasticRangeLeashBehaviour(E pEntity, Entity pLeashHolder, float pDistance) {
        double d0 = (pLeashHolder.getX() - pEntity.getX()) / pDistance;
        double d1 = (pLeashHolder.getY() - pEntity.getY()) / pDistance;
        double d2 = (pLeashHolder.getZ() - pEntity.getZ()) / pDistance;
        pEntity.setDeltaMovement(pEntity.getDeltaMovement().add(Math.copySign(d0 * d0 * 0.4, d0), Math.copySign(d1 * d1 * 0.4, d1), Math.copySign(d2 * d2 * 0.4, d2)));
    }

    default void setLeashedTo(Entity pLeashHolder, boolean pBroadcastPacket) {
        setLeashedTo((Entity & Leashable)this, pLeashHolder, pBroadcastPacket);
    }

    private static <E extends Entity & Leashable> void setLeashedTo(E pEntity, Entity pLeashHolder, boolean pBroadcastPacket) {
        Leashable.LeashData leashable$leashdata = pEntity.getLeashData();
        if (leashable$leashdata == null) {
            leashable$leashdata = new Leashable.LeashData(pLeashHolder);
            pEntity.setLeashData(leashable$leashdata);
        } else {
            leashable$leashdata.setLeashHolder(pLeashHolder);
        }

        if (pBroadcastPacket && pEntity.level() instanceof ServerLevel serverlevel) {
            serverlevel.getChunkSource().broadcast(pEntity, new ClientboundSetEntityLinkPacket(pEntity, pLeashHolder));
        }

        if (pEntity.isPassenger()) {
            pEntity.stopRiding();
        }
    }

    @Nullable
    default Entity getLeashHolder() {
        return getLeashHolder((Entity & Leashable)this);
    }

    @Nullable
    private static <E extends Entity & Leashable> Entity getLeashHolder(E pEntity) {
        Leashable.LeashData leashable$leashdata = pEntity.getLeashData();
        if (leashable$leashdata == null) {
            return null;
        } else {
            if (leashable$leashdata.delayedLeashHolderId != 0 && pEntity.level().isClientSide) {
                Entity entity = pEntity.level().getEntity(leashable$leashdata.delayedLeashHolderId);
                if (entity instanceof Entity) {
                    leashable$leashdata.setLeashHolder(entity);
                }
            }

            return leashable$leashdata.leashHolder;
        }
    }

    public static final class LeashData {
        public static final Codec<Leashable.LeashData> CODEC = Codec.xor(UUIDUtil.CODEC.fieldOf("UUID").codec(), BlockPos.CODEC)
            .xmap(
                Leashable.LeashData::new,
                p_394128_ -> {
                    if (p_394128_.leashHolder instanceof LeashFenceKnotEntity leashfenceknotentity) {
                        return Either.right(leashfenceknotentity.getPos());
                    } else {
                        return p_394128_.leashHolder != null
                            ? Either.left(p_394128_.leashHolder.getUUID())
                            : Objects.requireNonNull(p_394128_.delayedLeashInfo, "Invalid LeashData had no attachment");
                    }
                }
            );
        int delayedLeashHolderId;
        @Nullable
        public Entity leashHolder;
        @Nullable
        public Either<UUID, BlockPos> delayedLeashInfo;

        private LeashData(Either<UUID, BlockPos> pDelayedLeashInfo) {
            this.delayedLeashInfo = pDelayedLeashInfo;
        }

        LeashData(Entity pLeashHolder) {
            this.leashHolder = pLeashHolder;
        }

        LeashData(int pDelayedLeashInfoId) {
            this.delayedLeashHolderId = pDelayedLeashInfoId;
        }

        public void setLeashHolder(Entity pLeashHolder) {
            this.leashHolder = pLeashHolder;
            this.delayedLeashInfo = null;
            this.delayedLeashHolderId = 0;
        }
    }
}