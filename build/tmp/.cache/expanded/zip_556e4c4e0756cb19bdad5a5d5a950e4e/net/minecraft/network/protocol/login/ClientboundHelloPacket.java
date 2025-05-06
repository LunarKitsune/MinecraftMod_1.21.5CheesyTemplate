package net.minecraft.network.protocol.login;

import java.security.PublicKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;

public class ClientboundHelloPacket implements Packet<ClientLoginPacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundHelloPacket> STREAM_CODEC = Packet.codec(
        ClientboundHelloPacket::write, ClientboundHelloPacket::new
    );
    private final String serverId;
    private final byte[] publicKey;
    private final byte[] challenge;
    private final boolean shouldAuthenticate;

    public ClientboundHelloPacket(String pServerId, byte[] pPublicKey, byte[] pChallenge, boolean pShouldAuthenticate) {
        this.serverId = pServerId;
        this.publicKey = pPublicKey;
        this.challenge = pChallenge;
        this.shouldAuthenticate = pShouldAuthenticate;
    }

    private ClientboundHelloPacket(FriendlyByteBuf pBuffer) {
        this.serverId = pBuffer.readUtf(20);
        this.publicKey = pBuffer.readByteArray();
        this.challenge = pBuffer.readByteArray();
        this.shouldAuthenticate = pBuffer.readBoolean();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeUtf(this.serverId);
        pBuffer.writeByteArray(this.publicKey);
        pBuffer.writeByteArray(this.challenge);
        pBuffer.writeBoolean(this.shouldAuthenticate);
    }

    @Override
    public PacketType<ClientboundHelloPacket> type() {
        return LoginPacketTypes.CLIENTBOUND_HELLO;
    }

    public void handle(ClientLoginPacketListener pHandler) {
        pHandler.handleHello(this);
    }

    public String getServerId() {
        return this.serverId;
    }

    public PublicKey getPublicKey() throws CryptException {
        return Crypt.byteToPublicKey(this.publicKey);
    }

    public byte[] getChallenge() {
        return this.challenge;
    }

    public boolean shouldAuthenticate() {
        return this.shouldAuthenticate;
    }
}