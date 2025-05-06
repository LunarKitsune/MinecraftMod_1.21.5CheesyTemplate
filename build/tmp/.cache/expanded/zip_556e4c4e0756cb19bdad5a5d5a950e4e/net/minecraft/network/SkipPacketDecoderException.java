package net.minecraft.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.IdDispatchCodec;

public class SkipPacketDecoderException extends DecoderException implements SkipPacketException, IdDispatchCodec.DontDecorateException {
    public SkipPacketDecoderException(String pMessage) {
        super(pMessage);
    }

    public SkipPacketDecoderException(Throwable pCause) {
        super(pCause);
    }
}