package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.phys.Vec3;

public abstract class ServerboundMovePlayerPacket implements Packet<ServerGamePacketListener> {
    private static final int FLAG_ON_GROUND = 1;
    private static final int FLAG_HORIZONTAL_COLLISION = 2;
    protected final double x;
    protected final double y;
    protected final double z;
    protected final float yRot;
    protected final float xRot;
    protected final boolean onGround;
    protected final boolean horizontalCollision;
    protected final boolean hasPos;
    protected final boolean hasRot;

    static int packFlags(boolean pOnGround, boolean pHorizontalCollision) {
        int i = 0;
        if (pOnGround) {
            i |= 1;
        }

        if (pHorizontalCollision) {
            i |= 2;
        }

        return i;
    }

    static boolean unpackOnGround(int pFlags) {
        return (pFlags & 1) != 0;
    }

    static boolean unpackHorizontalCollision(int pFlags) {
        return (pFlags & 2) != 0;
    }

    protected ServerboundMovePlayerPacket(
        double pX,
        double pY,
        double pZ,
        float pYRot,
        float pXRot,
        boolean pOnGround,
        boolean pHorizontalCollision,
        boolean pHasPos,
        boolean pHasRot
    ) {
        this.x = pX;
        this.y = pY;
        this.z = pZ;
        this.yRot = pYRot;
        this.xRot = pXRot;
        this.onGround = pOnGround;
        this.horizontalCollision = pHorizontalCollision;
        this.hasPos = pHasPos;
        this.hasRot = pHasRot;
    }

    @Override
    public abstract PacketType<? extends ServerboundMovePlayerPacket> type();

    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleMovePlayer(this);
    }

    public double getX(double pDefaultValue) {
        return this.hasPos ? this.x : pDefaultValue;
    }

    public double getY(double pDefaultValue) {
        return this.hasPos ? this.y : pDefaultValue;
    }

    public double getZ(double pDefaultValue) {
        return this.hasPos ? this.z : pDefaultValue;
    }

    public float getYRot(float pDefaultValue) {
        return this.hasRot ? this.yRot : pDefaultValue;
    }

    public float getXRot(float pDefaultValue) {
        return this.hasRot ? this.xRot : pDefaultValue;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public boolean horizontalCollision() {
        return this.horizontalCollision;
    }

    public boolean hasPosition() {
        return this.hasPos;
    }

    public boolean hasRotation() {
        return this.hasRot;
    }

    public static class Pos extends ServerboundMovePlayerPacket {
        public static final StreamCodec<FriendlyByteBuf, ServerboundMovePlayerPacket.Pos> STREAM_CODEC = Packet.codec(
            ServerboundMovePlayerPacket.Pos::write, ServerboundMovePlayerPacket.Pos::read
        );

        public Pos(Vec3 pPos, boolean pOnGround, boolean pHorizonralCollision) {
            super(pPos.x, pPos.y, pPos.z, 0.0F, 0.0F, pOnGround, pHorizonralCollision, true, false);
        }

        public Pos(double pX, double pY, double pZ, boolean pOnGround, boolean pHorizontalCollision) {
            super(pX, pY, pZ, 0.0F, 0.0F, pOnGround, pHorizontalCollision, true, false);
        }

        private static ServerboundMovePlayerPacket.Pos read(FriendlyByteBuf pBuffer) {
            double d0 = pBuffer.readDouble();
            double d1 = pBuffer.readDouble();
            double d2 = pBuffer.readDouble();
            short short1 = pBuffer.readUnsignedByte();
            boolean flag = ServerboundMovePlayerPacket.unpackOnGround(short1);
            boolean flag1 = ServerboundMovePlayerPacket.unpackHorizontalCollision(short1);
            return new ServerboundMovePlayerPacket.Pos(d0, d1, d2, flag, flag1);
        }

        private void write(FriendlyByteBuf pBuffer) {
            pBuffer.writeDouble(this.x);
            pBuffer.writeDouble(this.y);
            pBuffer.writeDouble(this.z);
            pBuffer.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
        }

        @Override
        public PacketType<ServerboundMovePlayerPacket.Pos> type() {
            return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_POS;
        }
    }

    public static class PosRot extends ServerboundMovePlayerPacket {
        public static final StreamCodec<FriendlyByteBuf, ServerboundMovePlayerPacket.PosRot> STREAM_CODEC = Packet.codec(
            ServerboundMovePlayerPacket.PosRot::write, ServerboundMovePlayerPacket.PosRot::read
        );

        public PosRot(Vec3 pPos, float pYRot, float pXRot, boolean pOnGround, boolean pHorizontalCollision) {
            super(pPos.x, pPos.y, pPos.z, pYRot, pXRot, pOnGround, pHorizontalCollision, true, true);
        }

        public PosRot(double pX, double pY, double pZ, float pYRot, float pXRot, boolean pOnGround, boolean pHorizontalCollision) {
            super(pX, pY, pZ, pYRot, pXRot, pOnGround, pHorizontalCollision, true, true);
        }

        private static ServerboundMovePlayerPacket.PosRot read(FriendlyByteBuf pBuffer) {
            double d0 = pBuffer.readDouble();
            double d1 = pBuffer.readDouble();
            double d2 = pBuffer.readDouble();
            float f = pBuffer.readFloat();
            float f1 = pBuffer.readFloat();
            short short1 = pBuffer.readUnsignedByte();
            boolean flag = ServerboundMovePlayerPacket.unpackOnGround(short1);
            boolean flag1 = ServerboundMovePlayerPacket.unpackHorizontalCollision(short1);
            return new ServerboundMovePlayerPacket.PosRot(d0, d1, d2, f, f1, flag, flag1);
        }

        private void write(FriendlyByteBuf pBuffer) {
            pBuffer.writeDouble(this.x);
            pBuffer.writeDouble(this.y);
            pBuffer.writeDouble(this.z);
            pBuffer.writeFloat(this.yRot);
            pBuffer.writeFloat(this.xRot);
            pBuffer.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
        }

        @Override
        public PacketType<ServerboundMovePlayerPacket.PosRot> type() {
            return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_POS_ROT;
        }
    }

    public static class Rot extends ServerboundMovePlayerPacket {
        public static final StreamCodec<FriendlyByteBuf, ServerboundMovePlayerPacket.Rot> STREAM_CODEC = Packet.codec(
            ServerboundMovePlayerPacket.Rot::write, ServerboundMovePlayerPacket.Rot::read
        );

        public Rot(float pYRot, float pXRot, boolean pOnGround, boolean pHorizontalCollision) {
            super(0.0, 0.0, 0.0, pYRot, pXRot, pOnGround, pHorizontalCollision, false, true);
        }

        private static ServerboundMovePlayerPacket.Rot read(FriendlyByteBuf pBuffer) {
            float f = pBuffer.readFloat();
            float f1 = pBuffer.readFloat();
            short short1 = pBuffer.readUnsignedByte();
            boolean flag = ServerboundMovePlayerPacket.unpackOnGround(short1);
            boolean flag1 = ServerboundMovePlayerPacket.unpackHorizontalCollision(short1);
            return new ServerboundMovePlayerPacket.Rot(f, f1, flag, flag1);
        }

        private void write(FriendlyByteBuf pBuffer) {
            pBuffer.writeFloat(this.yRot);
            pBuffer.writeFloat(this.xRot);
            pBuffer.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
        }

        @Override
        public PacketType<ServerboundMovePlayerPacket.Rot> type() {
            return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_ROT;
        }
    }

    public static class StatusOnly extends ServerboundMovePlayerPacket {
        public static final StreamCodec<FriendlyByteBuf, ServerboundMovePlayerPacket.StatusOnly> STREAM_CODEC = Packet.codec(
            ServerboundMovePlayerPacket.StatusOnly::write, ServerboundMovePlayerPacket.StatusOnly::read
        );

        public StatusOnly(boolean pOnGround, boolean pHorizontalCollision) {
            super(0.0, 0.0, 0.0, 0.0F, 0.0F, pOnGround, pHorizontalCollision, false, false);
        }

        private static ServerboundMovePlayerPacket.StatusOnly read(FriendlyByteBuf pBuffer) {
            short short1 = pBuffer.readUnsignedByte();
            boolean flag = ServerboundMovePlayerPacket.unpackOnGround(short1);
            boolean flag1 = ServerboundMovePlayerPacket.unpackHorizontalCollision(short1);
            return new ServerboundMovePlayerPacket.StatusOnly(flag, flag1);
        }

        private void write(FriendlyByteBuf pBuffer) {
            pBuffer.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
        }

        @Override
        public PacketType<ServerboundMovePlayerPacket.StatusOnly> type() {
            return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_STATUS_ONLY;
        }
    }
}