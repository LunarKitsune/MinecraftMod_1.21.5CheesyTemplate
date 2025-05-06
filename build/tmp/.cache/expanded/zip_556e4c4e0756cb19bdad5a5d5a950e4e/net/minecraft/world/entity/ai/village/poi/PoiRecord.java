package net.minecraft.world.entity.ai.village.poi;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.util.VisibleForDebug;

public class PoiRecord {
    private final BlockPos pos;
    private final Holder<PoiType> poiType;
    private int freeTickets;
    private final Runnable setDirty;

    PoiRecord(BlockPos pPos, Holder<PoiType> pPoiType, int pFreeTickets, Runnable pSetDirty) {
        this.pos = pPos.immutable();
        this.poiType = pPoiType;
        this.freeTickets = pFreeTickets;
        this.setDirty = pSetDirty;
    }

    public PoiRecord(BlockPos pPod, Holder<PoiType> pPoiType, Runnable pSetDirty) {
        this(pPod, pPoiType, pPoiType.value().maxTickets(), pSetDirty);
    }

    public PoiRecord.Packed pack() {
        return new PoiRecord.Packed(this.pos, this.poiType, this.freeTickets);
    }

    @Deprecated
    @VisibleForDebug
    public int getFreeTickets() {
        return this.freeTickets;
    }

    protected boolean acquireTicket() {
        if (this.freeTickets <= 0) {
            return false;
        } else {
            this.freeTickets--;
            this.setDirty.run();
            return true;
        }
    }

    protected boolean releaseTicket() {
        if (this.freeTickets >= this.poiType.value().maxTickets()) {
            return false;
        } else {
            this.freeTickets++;
            this.setDirty.run();
            return true;
        }
    }

    public boolean hasSpace() {
        return this.freeTickets > 0;
    }

    public boolean isOccupied() {
        return this.freeTickets != this.poiType.value().maxTickets();
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public Holder<PoiType> getPoiType() {
        return this.poiType;
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return pOther != null && this.getClass() == pOther.getClass() ? Objects.equals(this.pos, ((PoiRecord)pOther).pos) : false;
        }
    }

    @Override
    public int hashCode() {
        return this.pos.hashCode();
    }

    public record Packed(BlockPos pos, Holder<PoiType> poiType, int freeTickets) {
        public static final Codec<PoiRecord.Packed> CODEC = RecordCodecBuilder.create(
            p_362341_ -> p_362341_.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(PoiRecord.Packed::pos),
                    RegistryFixedCodec.create(Registries.POINT_OF_INTEREST_TYPE).fieldOf("type").forGetter(PoiRecord.Packed::poiType),
                    Codec.INT.fieldOf("free_tickets").orElse(0).forGetter(PoiRecord.Packed::freeTickets)
                )
                .apply(p_362341_, PoiRecord.Packed::new)
        );

        public PoiRecord unpack(Runnable pSetDirty) {
            return new PoiRecord(this.pos, this.poiType, this.freeTickets, pSetDirty);
        }
    }
}