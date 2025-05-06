package net.minecraft.network.protocol.game;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundLevelParticlesPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundLevelParticlesPacket> STREAM_CODEC = Packet.codec(
        ClientboundLevelParticlesPacket::write, ClientboundLevelParticlesPacket::new
    );
    private final double x;
    private final double y;
    private final double z;
    private final float xDist;
    private final float yDist;
    private final float zDist;
    private final float maxSpeed;
    private final int count;
    private final boolean overrideLimiter;
    private final boolean alwaysShow;
    private final ParticleOptions particle;

    public <T extends ParticleOptions> ClientboundLevelParticlesPacket(
        T pParticle,
        boolean pOverrideLimiter,
        boolean pAlwaysShow,
        double pX,
        double pY,
        double pZ,
        float pXDist,
        float pYDist,
        float pZDist,
        float pMaxSpeed,
        int pCount
    ) {
        this.particle = pParticle;
        this.overrideLimiter = pOverrideLimiter;
        this.alwaysShow = pAlwaysShow;
        this.x = pX;
        this.y = pY;
        this.z = pZ;
        this.xDist = pXDist;
        this.yDist = pYDist;
        this.zDist = pZDist;
        this.maxSpeed = pMaxSpeed;
        this.count = pCount;
    }

    private ClientboundLevelParticlesPacket(RegistryFriendlyByteBuf pBuffer) {
        this.overrideLimiter = pBuffer.readBoolean();
        this.alwaysShow = pBuffer.readBoolean();
        this.x = pBuffer.readDouble();
        this.y = pBuffer.readDouble();
        this.z = pBuffer.readDouble();
        this.xDist = pBuffer.readFloat();
        this.yDist = pBuffer.readFloat();
        this.zDist = pBuffer.readFloat();
        this.maxSpeed = pBuffer.readFloat();
        this.count = pBuffer.readInt();
        this.particle = ParticleTypes.STREAM_CODEC.decode(pBuffer);
    }

    private void write(RegistryFriendlyByteBuf pBuffer) {
        pBuffer.writeBoolean(this.overrideLimiter);
        pBuffer.writeBoolean(this.alwaysShow);
        pBuffer.writeDouble(this.x);
        pBuffer.writeDouble(this.y);
        pBuffer.writeDouble(this.z);
        pBuffer.writeFloat(this.xDist);
        pBuffer.writeFloat(this.yDist);
        pBuffer.writeFloat(this.zDist);
        pBuffer.writeFloat(this.maxSpeed);
        pBuffer.writeInt(this.count);
        ParticleTypes.STREAM_CODEC.encode(pBuffer, this.particle);
    }

    @Override
    public PacketType<ClientboundLevelParticlesPacket> type() {
        return GamePacketTypes.CLIENTBOUND_LEVEL_PARTICLES;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleParticleEvent(this);
    }

    public boolean isOverrideLimiter() {
        return this.overrideLimiter;
    }

    public boolean alwaysShow() {
        return this.alwaysShow;
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

    public float getXDist() {
        return this.xDist;
    }

    public float getYDist() {
        return this.yDist;
    }

    public float getZDist() {
        return this.zDist;
    }

    public float getMaxSpeed() {
        return this.maxSpeed;
    }

    public int getCount() {
        return this.count;
    }

    public ParticleOptions getParticle() {
        return this.particle;
    }
}