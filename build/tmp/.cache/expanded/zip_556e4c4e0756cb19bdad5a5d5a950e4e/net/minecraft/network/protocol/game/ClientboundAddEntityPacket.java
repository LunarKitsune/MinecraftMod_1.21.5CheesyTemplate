package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

public class ClientboundAddEntityPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundAddEntityPacket> STREAM_CODEC = Packet.codec(
        ClientboundAddEntityPacket::write, ClientboundAddEntityPacket::new
    );
    private static final double MAGICAL_QUANTIZATION = 8000.0;
    private static final double LIMIT = 3.9;
    private final int id;
    private final UUID uuid;
    private final EntityType<?> type;
    private final double x;
    private final double y;
    private final double z;
    private final int xa;
    private final int ya;
    private final int za;
    private final byte xRot;
    private final byte yRot;
    private final byte yHeadRot;
    private final int data;

    public ClientboundAddEntityPacket(Entity pEntity, ServerEntity pServerEntity) {
        this(pEntity, pServerEntity, 0);
    }

    public ClientboundAddEntityPacket(Entity pEntity, ServerEntity pServerEntity, int pData) {
        this(
            pEntity.getId(),
            pEntity.getUUID(),
            pServerEntity.getPositionBase().x(),
            pServerEntity.getPositionBase().y(),
            pServerEntity.getPositionBase().z(),
            pServerEntity.getLastSentXRot(),
            pServerEntity.getLastSentYRot(),
            pEntity.getType(),
            pData,
            pServerEntity.getLastSentMovement(),
            pServerEntity.getLastSentYHeadRot()
        );
    }

    public ClientboundAddEntityPacket(Entity pEntity, int pData, BlockPos pPos) {
        this(
            pEntity.getId(),
            pEntity.getUUID(),
            pPos.getX(),
            pPos.getY(),
            pPos.getZ(),
            pEntity.getXRot(),
            pEntity.getYRot(),
            pEntity.getType(),
            pData,
            pEntity.getDeltaMovement(),
            pEntity.getYHeadRot()
        );
    }

    public ClientboundAddEntityPacket(
        int pId,
        UUID pUuid,
        double pX,
        double pY,
        double pZ,
        float pXRot,
        float pYRot,
        EntityType<?> pType,
        int pData,
        Vec3 pDeltaMovement,
        double pYHeadRot
    ) {
        this.id = pId;
        this.uuid = pUuid;
        this.x = pX;
        this.y = pY;
        this.z = pZ;
        this.xRot = Mth.packDegrees(pXRot);
        this.yRot = Mth.packDegrees(pYRot);
        this.yHeadRot = Mth.packDegrees((float)pYHeadRot);
        this.type = pType;
        this.data = pData;
        this.xa = (int)(Mth.clamp(pDeltaMovement.x, -3.9, 3.9) * 8000.0);
        this.ya = (int)(Mth.clamp(pDeltaMovement.y, -3.9, 3.9) * 8000.0);
        this.za = (int)(Mth.clamp(pDeltaMovement.z, -3.9, 3.9) * 8000.0);
    }

    private ClientboundAddEntityPacket(RegistryFriendlyByteBuf pBuffer) {
        this.id = pBuffer.readVarInt();
        this.uuid = pBuffer.readUUID();
        this.type = ByteBufCodecs.registry(Registries.ENTITY_TYPE).decode(pBuffer);
        this.x = pBuffer.readDouble();
        this.y = pBuffer.readDouble();
        this.z = pBuffer.readDouble();
        this.xRot = pBuffer.readByte();
        this.yRot = pBuffer.readByte();
        this.yHeadRot = pBuffer.readByte();
        this.data = pBuffer.readVarInt();
        this.xa = pBuffer.readShort();
        this.ya = pBuffer.readShort();
        this.za = pBuffer.readShort();
    }

    private void write(RegistryFriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.id);
        pBuffer.writeUUID(this.uuid);
        ByteBufCodecs.registry(Registries.ENTITY_TYPE).encode(pBuffer, this.type);
        pBuffer.writeDouble(this.x);
        pBuffer.writeDouble(this.y);
        pBuffer.writeDouble(this.z);
        pBuffer.writeByte(this.xRot);
        pBuffer.writeByte(this.yRot);
        pBuffer.writeByte(this.yHeadRot);
        pBuffer.writeVarInt(this.data);
        pBuffer.writeShort(this.xa);
        pBuffer.writeShort(this.ya);
        pBuffer.writeShort(this.za);
    }

    @Override
    public PacketType<ClientboundAddEntityPacket> type() {
        return GamePacketTypes.CLIENTBOUND_ADD_ENTITY;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleAddEntity(this);
    }

    public int getId() {
        return this.id;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public EntityType<?> getType() {
        return this.type;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public double getXa() {
        return this.xa / 8000.0;
    }

    public double getYa() {
        return this.ya / 8000.0;
    }

    public double getZa() {
        return this.za / 8000.0;
    }

    public float getXRot() {
        return Mth.unpackDegrees(this.xRot);
    }

    public float getYRot() {
        return Mth.unpackDegrees(this.yRot);
    }

    public float getYHeadRot() {
        return Mth.unpackDegrees(this.yHeadRot);
    }

    public int getData() {
        return this.data;
    }
}