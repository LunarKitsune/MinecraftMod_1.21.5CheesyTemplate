package net.minecraft.network.protocol.common;

import net.minecraft.network.protocol.cookie.ServerCookiePacketListener;

public interface ServerCommonPacketListener extends ServerCookiePacketListener {
    void handleKeepAlive(ServerboundKeepAlivePacket pPacket);

    void handlePong(ServerboundPongPacket pPacket);

    void handleCustomPayload(ServerboundCustomPayloadPacket pPacket);

    void handleResourcePackResponse(ServerboundResourcePackPacket pPacket);

    void handleClientInformation(ServerboundClientInformationPacket pPacket);
}