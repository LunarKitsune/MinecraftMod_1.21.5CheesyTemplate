package net.minecraft.network.protocol.login;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.cookie.ServerCookiePacketListener;

/**
 * PacketListener for the server side of the LOGIN protocol.
 */
public interface ServerLoginPacketListener extends ServerCookiePacketListener {
    @Override
    default ConnectionProtocol protocol() {
        return ConnectionProtocol.LOGIN;
    }

    void handleHello(ServerboundHelloPacket pPacket);

    void handleKey(ServerboundKeyPacket pPacket);

    void handleCustomQueryPacket(ServerboundCustomQueryAnswerPacket pPacket);

    void handleLoginAcknowledgement(ServerboundLoginAcknowledgedPacket pPacket);
}