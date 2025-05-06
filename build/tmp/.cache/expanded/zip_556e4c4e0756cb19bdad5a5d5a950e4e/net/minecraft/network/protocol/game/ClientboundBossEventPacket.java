package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.BossEvent;

public class ClientboundBossEventPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundBossEventPacket> STREAM_CODEC = Packet.codec(
        ClientboundBossEventPacket::write, ClientboundBossEventPacket::new
    );
    private static final int FLAG_DARKEN = 1;
    private static final int FLAG_MUSIC = 2;
    private static final int FLAG_FOG = 4;
    private final UUID id;
    private final ClientboundBossEventPacket.Operation operation;
    static final ClientboundBossEventPacket.Operation REMOVE_OPERATION = new ClientboundBossEventPacket.Operation() {
        @Override
        public ClientboundBossEventPacket.OperationType getType() {
            return ClientboundBossEventPacket.OperationType.REMOVE;
        }

        @Override
        public void dispatch(UUID p_178660_, ClientboundBossEventPacket.Handler p_178661_) {
            p_178661_.remove(p_178660_);
        }

        @Override
        public void write(RegistryFriendlyByteBuf p_328351_) {
        }
    };

    private ClientboundBossEventPacket(UUID pId, ClientboundBossEventPacket.Operation pOperation) {
        this.id = pId;
        this.operation = pOperation;
    }

    private ClientboundBossEventPacket(RegistryFriendlyByteBuf pBuffer) {
        this.id = pBuffer.readUUID();
        ClientboundBossEventPacket.OperationType clientboundbosseventpacket$operationtype = pBuffer.readEnum(ClientboundBossEventPacket.OperationType.class);
        this.operation = clientboundbosseventpacket$operationtype.reader.decode(pBuffer);
    }

    public static ClientboundBossEventPacket createAddPacket(BossEvent pEvent) {
        return new ClientboundBossEventPacket(pEvent.getId(), new ClientboundBossEventPacket.AddOperation(pEvent));
    }

    public static ClientboundBossEventPacket createRemovePacket(UUID pId) {
        return new ClientboundBossEventPacket(pId, REMOVE_OPERATION);
    }

    public static ClientboundBossEventPacket createUpdateProgressPacket(BossEvent pEvent) {
        return new ClientboundBossEventPacket(pEvent.getId(), new ClientboundBossEventPacket.UpdateProgressOperation(pEvent.getProgress()));
    }

    public static ClientboundBossEventPacket createUpdateNamePacket(BossEvent pEvent) {
        return new ClientboundBossEventPacket(pEvent.getId(), new ClientboundBossEventPacket.UpdateNameOperation(pEvent.getName()));
    }

    public static ClientboundBossEventPacket createUpdateStylePacket(BossEvent pEvent) {
        return new ClientboundBossEventPacket(
            pEvent.getId(), new ClientboundBossEventPacket.UpdateStyleOperation(pEvent.getColor(), pEvent.getOverlay())
        );
    }

    public static ClientboundBossEventPacket createUpdatePropertiesPacket(BossEvent pEvent) {
        return new ClientboundBossEventPacket(
            pEvent.getId(), new ClientboundBossEventPacket.UpdatePropertiesOperation(pEvent.shouldDarkenScreen(), pEvent.shouldPlayBossMusic(), pEvent.shouldCreateWorldFog())
        );
    }

    private void write(RegistryFriendlyByteBuf pBuffer) {
        pBuffer.writeUUID(this.id);
        pBuffer.writeEnum(this.operation.getType());
        this.operation.write(pBuffer);
    }

    static int encodeProperties(boolean pDarkenScreen, boolean pPlayMusic, boolean pCreateWorldFog) {
        int i = 0;
        if (pDarkenScreen) {
            i |= 1;
        }

        if (pPlayMusic) {
            i |= 2;
        }

        if (pCreateWorldFog) {
            i |= 4;
        }

        return i;
    }

    @Override
    public PacketType<ClientboundBossEventPacket> type() {
        return GamePacketTypes.CLIENTBOUND_BOSS_EVENT;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleBossUpdate(this);
    }

    public void dispatch(ClientboundBossEventPacket.Handler pHandler) {
        this.operation.dispatch(this.id, pHandler);
    }

    static class AddOperation implements ClientboundBossEventPacket.Operation {
        private final Component name;
        private final float progress;
        private final BossEvent.BossBarColor color;
        private final BossEvent.BossBarOverlay overlay;
        private final boolean darkenScreen;
        private final boolean playMusic;
        private final boolean createWorldFog;

        AddOperation(BossEvent pEvent) {
            this.name = pEvent.getName();
            this.progress = pEvent.getProgress();
            this.color = pEvent.getColor();
            this.overlay = pEvent.getOverlay();
            this.darkenScreen = pEvent.shouldDarkenScreen();
            this.playMusic = pEvent.shouldPlayBossMusic();
            this.createWorldFog = pEvent.shouldCreateWorldFog();
        }

        private AddOperation(RegistryFriendlyByteBuf pBuffer) {
            this.name = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(pBuffer);
            this.progress = pBuffer.readFloat();
            this.color = pBuffer.readEnum(BossEvent.BossBarColor.class);
            this.overlay = pBuffer.readEnum(BossEvent.BossBarOverlay.class);
            int i = pBuffer.readUnsignedByte();
            this.darkenScreen = (i & 1) > 0;
            this.playMusic = (i & 2) > 0;
            this.createWorldFog = (i & 4) > 0;
        }

        @Override
        public ClientboundBossEventPacket.OperationType getType() {
            return ClientboundBossEventPacket.OperationType.ADD;
        }

        @Override
        public void dispatch(UUID p_178677_, ClientboundBossEventPacket.Handler p_178678_) {
            p_178678_.add(p_178677_, this.name, this.progress, this.color, this.overlay, this.darkenScreen, this.playMusic, this.createWorldFog);
        }

        @Override
        public void write(RegistryFriendlyByteBuf p_330694_) {
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(p_330694_, this.name);
            p_330694_.writeFloat(this.progress);
            p_330694_.writeEnum(this.color);
            p_330694_.writeEnum(this.overlay);
            p_330694_.writeByte(ClientboundBossEventPacket.encodeProperties(this.darkenScreen, this.playMusic, this.createWorldFog));
        }
    }

    public interface Handler {
        default void add(
            UUID pId,
            Component pName,
            float pProgress,
            BossEvent.BossBarColor pColor,
            BossEvent.BossBarOverlay pOverlay,
            boolean pDarkenScreen,
            boolean pPlayMusic,
            boolean pCreateWorldFog
        ) {
        }

        default void remove(UUID pId) {
        }

        default void updateProgress(UUID pId, float pProgress) {
        }

        default void updateName(UUID pId, Component pName) {
        }

        default void updateStyle(UUID pId, BossEvent.BossBarColor pColor, BossEvent.BossBarOverlay pOverlay) {
        }

        default void updateProperties(UUID pId, boolean pDarkenScreen, boolean pPlayMusic, boolean pCreateWorldFog) {
        }
    }

    interface Operation {
        ClientboundBossEventPacket.OperationType getType();

        void dispatch(UUID pId, ClientboundBossEventPacket.Handler pHandler);

        void write(RegistryFriendlyByteBuf pBuffer);
    }

    static enum OperationType {
        ADD(ClientboundBossEventPacket.AddOperation::new),
        REMOVE(p_329619_ -> ClientboundBossEventPacket.REMOVE_OPERATION),
        UPDATE_PROGRESS(ClientboundBossEventPacket.UpdateProgressOperation::new),
        UPDATE_NAME(ClientboundBossEventPacket.UpdateNameOperation::new),
        UPDATE_STYLE(ClientboundBossEventPacket.UpdateStyleOperation::new),
        UPDATE_PROPERTIES(ClientboundBossEventPacket.UpdatePropertiesOperation::new);

        final StreamDecoder<RegistryFriendlyByteBuf, ClientboundBossEventPacket.Operation> reader;

        private OperationType(final StreamDecoder<RegistryFriendlyByteBuf, ClientboundBossEventPacket.Operation> pReader) {
            this.reader = pReader;
        }
    }

    record UpdateNameOperation(Component name) implements ClientboundBossEventPacket.Operation {
        private UpdateNameOperation(RegistryFriendlyByteBuf pBuffer) {
            this(ComponentSerialization.TRUSTED_STREAM_CODEC.decode(pBuffer));
        }

        @Override
        public ClientboundBossEventPacket.OperationType getType() {
            return ClientboundBossEventPacket.OperationType.UPDATE_NAME;
        }

        @Override
        public void dispatch(UUID p_178730_, ClientboundBossEventPacket.Handler p_178731_) {
            p_178731_.updateName(p_178730_, this.name);
        }

        @Override
        public void write(RegistryFriendlyByteBuf p_332126_) {
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(p_332126_, this.name);
        }
    }

    record UpdateProgressOperation(float progress) implements ClientboundBossEventPacket.Operation {
        private UpdateProgressOperation(RegistryFriendlyByteBuf pBuffer) {
            this(pBuffer.readFloat());
        }

        @Override
        public ClientboundBossEventPacket.OperationType getType() {
            return ClientboundBossEventPacket.OperationType.UPDATE_PROGRESS;
        }

        @Override
        public void dispatch(UUID p_178741_, ClientboundBossEventPacket.Handler p_178742_) {
            p_178742_.updateProgress(p_178741_, this.progress);
        }

        @Override
        public void write(RegistryFriendlyByteBuf p_331612_) {
            p_331612_.writeFloat(this.progress);
        }
    }

    static class UpdatePropertiesOperation implements ClientboundBossEventPacket.Operation {
        private final boolean darkenScreen;
        private final boolean playMusic;
        private final boolean createWorldFog;

        UpdatePropertiesOperation(boolean pDarkenScreen, boolean pPlayMusic, boolean pCreateWorldFog) {
            this.darkenScreen = pDarkenScreen;
            this.playMusic = pPlayMusic;
            this.createWorldFog = pCreateWorldFog;
        }

        private UpdatePropertiesOperation(RegistryFriendlyByteBuf pBuffer) {
            int i = pBuffer.readUnsignedByte();
            this.darkenScreen = (i & 1) > 0;
            this.playMusic = (i & 2) > 0;
            this.createWorldFog = (i & 4) > 0;
        }

        @Override
        public ClientboundBossEventPacket.OperationType getType() {
            return ClientboundBossEventPacket.OperationType.UPDATE_PROPERTIES;
        }

        @Override
        public void dispatch(UUID p_178756_, ClientboundBossEventPacket.Handler p_178757_) {
            p_178757_.updateProperties(p_178756_, this.darkenScreen, this.playMusic, this.createWorldFog);
        }

        @Override
        public void write(RegistryFriendlyByteBuf p_327814_) {
            p_327814_.writeByte(ClientboundBossEventPacket.encodeProperties(this.darkenScreen, this.playMusic, this.createWorldFog));
        }
    }

    static class UpdateStyleOperation implements ClientboundBossEventPacket.Operation {
        private final BossEvent.BossBarColor color;
        private final BossEvent.BossBarOverlay overlay;

        UpdateStyleOperation(BossEvent.BossBarColor pColor, BossEvent.BossBarOverlay pOverlay) {
            this.color = pColor;
            this.overlay = pOverlay;
        }

        private UpdateStyleOperation(RegistryFriendlyByteBuf pBuffer) {
            this.color = pBuffer.readEnum(BossEvent.BossBarColor.class);
            this.overlay = pBuffer.readEnum(BossEvent.BossBarOverlay.class);
        }

        @Override
        public ClientboundBossEventPacket.OperationType getType() {
            return ClientboundBossEventPacket.OperationType.UPDATE_STYLE;
        }

        @Override
        public void dispatch(UUID p_178769_, ClientboundBossEventPacket.Handler p_178770_) {
            p_178770_.updateStyle(p_178769_, this.color, this.overlay);
        }

        @Override
        public void write(RegistryFriendlyByteBuf p_332920_) {
            p_332920_.writeEnum(this.color);
            p_332920_.writeEnum(this.overlay);
        }
    }
}