package net.minecraft.network;

import io.netty.handler.codec.EncoderException;
import net.minecraft.network.codec.IdDispatchCodec;

public class SkipPacketEncoderException extends EncoderException implements SkipPacketException, IdDispatchCodec.DontDecorateException {
    public SkipPacketEncoderException(String pMessage) {
        super(pMessage);
    }

    public SkipPacketEncoderException(Throwable pCause) {
        super(pCause);
    }
}