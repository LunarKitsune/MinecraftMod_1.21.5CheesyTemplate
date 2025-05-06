package net.minecraft.network.protocol;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.network.ClientboundPacketListener;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.ServerboundPacketListener;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;

public class ProtocolInfoBuilder<T extends PacketListener, B extends ByteBuf, C> {
    final ConnectionProtocol protocol;
    final PacketFlow flow;
    private final List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> codecs = new ArrayList<>();
    @Nullable
    private BundlerInfo bundlerInfo;

    public ProtocolInfoBuilder(ConnectionProtocol pProtocol, PacketFlow pFlow) {
        this.protocol = pProtocol;
        this.flow = pFlow;
    }

    public <P extends Packet<? super T>> ProtocolInfoBuilder<T, B, C> addPacket(PacketType<P> pType, StreamCodec<? super B, P> pSerializer) {
        this.codecs.add(new ProtocolInfoBuilder.CodecEntry<>(pType, pSerializer, null));
        return this;
    }

    public <P extends Packet<? super T>> ProtocolInfoBuilder<T, B, C> addPacket(
        PacketType<P> pType, StreamCodec<? super B, P> pSerializer, CodecModifier<B, P, C> pModifier
    ) {
        this.codecs.add(new ProtocolInfoBuilder.CodecEntry<>(pType, pSerializer, pModifier));
        return this;
    }

    public <P extends BundlePacket<? super T>, D extends BundleDelimiterPacket<? super T>> ProtocolInfoBuilder<T, B, C> withBundlePacket(
        PacketType<P> pType, Function<Iterable<Packet<? super T>>, P> pBundler, D pPacket
    ) {
        StreamCodec<ByteBuf, D> streamcodec = StreamCodec.unit(pPacket);
        PacketType<D> packettype = (PacketType)pPacket.type();
        this.codecs.add(new ProtocolInfoBuilder.CodecEntry<>(packettype, streamcodec, null));
        this.bundlerInfo = BundlerInfo.createForPacket(pType, pBundler, pPacket);
        return this;
    }

    StreamCodec<ByteBuf, Packet<? super T>> buildPacketCodec(Function<ByteBuf, B> pBufferFactory, List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> pEntries, C pContext) {
        ProtocolCodecBuilder<ByteBuf, T> protocolcodecbuilder = new ProtocolCodecBuilder<>(this.flow);

        for (ProtocolInfoBuilder.CodecEntry<T, ?, B, C> codecentry : pEntries) {
            codecentry.addToBuilder(protocolcodecbuilder, pBufferFactory, pContext);
        }

        return protocolcodecbuilder.build();
    }

    private static ProtocolInfo.Details buildDetails(
        final ConnectionProtocol pProtocol, final PacketFlow pFlow, final List<? extends ProtocolInfoBuilder.CodecEntry<?, ?, ?, ?>> pEntries
    ) {
        return new ProtocolInfo.Details() {
            @Override
            public ConnectionProtocol id() {
                return pProtocol;
            }

            @Override
            public PacketFlow flow() {
                return pFlow;
            }

            @Override
            public void listPackets(ProtocolInfo.Details.PacketVisitor p_397253_) {
                for (int i = 0; i < pEntries.size(); i++) {
                    ProtocolInfoBuilder.CodecEntry<?, ?, ?, ?> codecentry = (ProtocolInfoBuilder.CodecEntry<?, ?, ?, ?>)pEntries.get(i);
                    p_397253_.accept(codecentry.type, i);
                }
            }
        };
    }

    public SimpleUnboundProtocol<T, B> buildUnbound(final C pContext) {
        final List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> list = List.copyOf(this.codecs);
        final BundlerInfo bundlerinfo = this.bundlerInfo;
        final ProtocolInfo.Details protocolinfo$details = buildDetails(this.protocol, this.flow, list);
        return new SimpleUnboundProtocol<T, B>() {
            @Override
            public ProtocolInfo<T> bind(Function<ByteBuf, B> p_391671_) {
                return new ProtocolInfoBuilder.Implementation<>(
                    ProtocolInfoBuilder.this.protocol,
                    ProtocolInfoBuilder.this.flow,
                    ProtocolInfoBuilder.this.buildPacketCodec(p_391671_, list, pContext),
                    bundlerinfo
                );
            }

            @Override
            public ProtocolInfo.Details details() {
                return protocolinfo$details;
            }
        };
    }

    public UnboundProtocol<T, B, C> buildUnbound() {
        final List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> list = List.copyOf(this.codecs);
        final BundlerInfo bundlerinfo = this.bundlerInfo;
        final ProtocolInfo.Details protocolinfo$details = buildDetails(this.protocol, this.flow, list);
        return new UnboundProtocol<T, B, C>() {
            @Override
            public ProtocolInfo<T> bind(Function<ByteBuf, B> p_391590_, C p_391890_) {
                return new ProtocolInfoBuilder.Implementation<>(
                    ProtocolInfoBuilder.this.protocol,
                    ProtocolInfoBuilder.this.flow,
                    ProtocolInfoBuilder.this.buildPacketCodec(p_391590_, list, p_391890_),
                    bundlerinfo
                );
            }

            @Override
            public ProtocolInfo.Details details() {
                return protocolinfo$details;
            }
        };
    }

    private static <L extends PacketListener, B extends ByteBuf> SimpleUnboundProtocol<L, B> protocol(
        ConnectionProtocol pProtocol, PacketFlow pFlow, Consumer<ProtocolInfoBuilder<L, B, Unit>> pPacketAdder
    ) {
        ProtocolInfoBuilder<L, B, Unit> protocolinfobuilder = new ProtocolInfoBuilder<>(pProtocol, pFlow);
        pPacketAdder.accept(protocolinfobuilder);
        return protocolinfobuilder.buildUnbound(Unit.INSTANCE);
    }

    public static <T extends ServerboundPacketListener, B extends ByteBuf> SimpleUnboundProtocol<T, B> serverboundProtocol(
        ConnectionProtocol pProtocol, Consumer<ProtocolInfoBuilder<T, B, Unit>> pPacketAdder
    ) {
        return protocol(pProtocol, PacketFlow.SERVERBOUND, pPacketAdder);
    }

    public static <T extends ClientboundPacketListener, B extends ByteBuf> SimpleUnboundProtocol<T, B> clientboundProtocol(
        ConnectionProtocol pProtocol, Consumer<ProtocolInfoBuilder<T, B, Unit>> pPacketAdder
    ) {
        return protocol(pProtocol, PacketFlow.CLIENTBOUND, pPacketAdder);
    }

    private static <L extends PacketListener, B extends ByteBuf, C> UnboundProtocol<L, B, C> contextProtocol(
        ConnectionProtocol pProtocol, PacketFlow pFlow, Consumer<ProtocolInfoBuilder<L, B, C>> pPacketAdder
    ) {
        ProtocolInfoBuilder<L, B, C> protocolinfobuilder = new ProtocolInfoBuilder<>(pProtocol, pFlow);
        pPacketAdder.accept(protocolinfobuilder);
        return protocolinfobuilder.buildUnbound();
    }

    public static <T extends ServerboundPacketListener, B extends ByteBuf, C> UnboundProtocol<T, B, C> contextServerboundProtocol(
        ConnectionProtocol pProtocol, Consumer<ProtocolInfoBuilder<T, B, C>> pPacketAdder
    ) {
        return contextProtocol(pProtocol, PacketFlow.SERVERBOUND, pPacketAdder);
    }

    public static <T extends ClientboundPacketListener, B extends ByteBuf, C> UnboundProtocol<T, B, C> contextClientboundProtocol(
        ConnectionProtocol pProtocol, Consumer<ProtocolInfoBuilder<T, B, C>> pPacketAdder
    ) {
        return contextProtocol(pProtocol, PacketFlow.CLIENTBOUND, pPacketAdder);
    }

    record CodecEntry<T extends PacketListener, P extends Packet<? super T>, B extends ByteBuf, C>(
        PacketType<P> type, StreamCodec<? super B, P> serializer, @Nullable CodecModifier<B, P, C> modifier
    ) {
        public void addToBuilder(ProtocolCodecBuilder<ByteBuf, T> pBuilder, Function<ByteBuf, B> pBufferFactory, C pContext) {
            StreamCodec<? super B, P> streamcodec;
            if (this.modifier != null) {
                streamcodec = this.modifier.apply(this.serializer, pContext);
            } else {
                streamcodec = this.serializer;
            }

            StreamCodec<ByteBuf, P> streamcodec1 = streamcodec.mapStream(pBufferFactory);
            pBuilder.add(this.type, streamcodec1);
        }
    }

    record Implementation<L extends PacketListener>(
        ConnectionProtocol id, PacketFlow flow, StreamCodec<ByteBuf, Packet<? super L>> codec, @Nullable BundlerInfo bundlerInfo
    ) implements ProtocolInfo<L> {
        @Override
        public ConnectionProtocol id() {
            return this.id;
        }

        @Override
        public PacketFlow flow() {
            return this.flow;
        }

        @Override
        public StreamCodec<ByteBuf, Packet<? super L>> codec() {
            return this.codec;
        }

        @Nullable
        @Override
        public BundlerInfo bundlerInfo() {
            return this.bundlerInfo;
        }
    }
}