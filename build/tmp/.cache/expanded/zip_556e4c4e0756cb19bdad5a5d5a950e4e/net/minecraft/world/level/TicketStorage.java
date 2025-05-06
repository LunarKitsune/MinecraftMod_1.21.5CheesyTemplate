package net.minecraft.world.level;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.slf4j.Logger;

public class TicketStorage extends SavedData {
    private static final int INITIAL_TICKET_LIST_CAPACITY = 4;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Codec<Pair<ChunkPos, Ticket>> TICKET_ENTRY = Codec.mapPair(ChunkPos.CODEC.fieldOf("chunk_pos"), Ticket.CODEC).codec();
    public static final Codec<TicketStorage> CODEC = RecordCodecBuilder.create(
        p_395442_ -> p_395442_.group(TICKET_ENTRY.listOf().optionalFieldOf("tickets", List.of()).forGetter(TicketStorage::packTickets))
            .apply(p_395442_, TicketStorage::fromPacked)
    );
    public static final SavedDataType<TicketStorage> TYPE = new SavedDataType<>(
        "chunks", TicketStorage::new, CODEC, DataFixTypes.SAVED_DATA_FORCED_CHUNKS
    );
    private final Long2ObjectOpenHashMap<List<Ticket>> tickets;
    private final Long2ObjectOpenHashMap<List<Ticket>> deactivatedTickets;
    private LongSet chunksWithForcedTickets = new LongOpenHashSet();
    @Nullable
    private TicketStorage.ChunkUpdated loadingChunkUpdatedListener;
    @Nullable
    private TicketStorage.ChunkUpdated simulationChunkUpdatedListener;

    private TicketStorage(Long2ObjectOpenHashMap<List<Ticket>> pTickets, Long2ObjectOpenHashMap<List<Ticket>> pDeactivatedTickets) {
        this.tickets = pTickets;
        this.deactivatedTickets = pDeactivatedTickets;
        this.updateForcedChunks();
    }

    public TicketStorage() {
        this(new Long2ObjectOpenHashMap<>(4), new Long2ObjectOpenHashMap<>());
    }

    private static TicketStorage fromPacked(List<Pair<ChunkPos, Ticket>> pPacked) {
        Long2ObjectOpenHashMap<List<Ticket>> long2objectopenhashmap = new Long2ObjectOpenHashMap<>();

        for (Pair<ChunkPos, Ticket> pair : pPacked) {
            ChunkPos chunkpos = pair.getFirst();
            List<Ticket> list = long2objectopenhashmap.computeIfAbsent(chunkpos.toLong(), p_396965_ -> new ObjectArrayList<>(4));
            list.add(pair.getSecond());
        }

        return new TicketStorage(new Long2ObjectOpenHashMap<>(4), long2objectopenhashmap);
    }

    private List<Pair<ChunkPos, Ticket>> packTickets() {
        List<Pair<ChunkPos, Ticket>> list = new ArrayList<>();
        this.forEachTicket((p_397558_, p_396676_) -> {
            if (p_396676_.getType().persist()) {
                list.add(new Pair<>(p_397558_, p_396676_));
            }
        });
        return list;
    }

    private void forEachTicket(BiConsumer<ChunkPos, Ticket> pAction) {
        forEachTicket(pAction, this.tickets);
        forEachTicket(pAction, this.deactivatedTickets);
    }

    private static void forEachTicket(BiConsumer<ChunkPos, Ticket> pAction, Long2ObjectOpenHashMap<List<Ticket>> pTickets) {
        for (Entry<List<Ticket>> entry : Long2ObjectMaps.fastIterable(pTickets)) {
            ChunkPos chunkpos = new ChunkPos(entry.getLongKey());

            for (Ticket ticket : entry.getValue()) {
                pAction.accept(chunkpos, ticket);
            }
        }
    }

    public void activateAllDeactivatedTickets() {
        for (Entry<List<Ticket>> entry : Long2ObjectMaps.fastIterable(this.deactivatedTickets)) {
            for (Ticket ticket : entry.getValue()) {
                this.addTicket(entry.getLongKey(), ticket);
            }
        }

        this.deactivatedTickets.clear();
    }

    public void setLoadingChunkUpdatedListener(@Nullable TicketStorage.ChunkUpdated pLoadingChunkUpdatedListener) {
        this.loadingChunkUpdatedListener = pLoadingChunkUpdatedListener;
    }

    public void setSimulationChunkUpdatedListener(@Nullable TicketStorage.ChunkUpdated pSimulationChunkUpdatedListener) {
        this.simulationChunkUpdatedListener = pSimulationChunkUpdatedListener;
    }

    public boolean hasTickets() {
        return !this.tickets.isEmpty();
    }

    public List<Ticket> getTickets(long pChunkPos) {
        return this.tickets.getOrDefault(pChunkPos, List.of());
    }

    private List<Ticket> getOrCreateTickets(long pChunkPos) {
        return this.tickets.computeIfAbsent(pChunkPos, p_395686_ -> new ObjectArrayList<>(4));
    }

    public void addTicketWithRadius(TicketType pTicketType, ChunkPos pChunkPos, int pRadius) {
        Ticket ticket = new Ticket(pTicketType, ChunkLevel.byStatus(FullChunkStatus.FULL) - pRadius);
        this.addTicket(pChunkPos.toLong(), ticket);
    }

    public void addTicket(Ticket pTicket, ChunkPos pChunkPos) {
        this.addTicket(pChunkPos.toLong(), pTicket);
    }

    public boolean addTicket(long pChunkPos, Ticket pTicket) {
        List<Ticket> list = this.getOrCreateTickets(pChunkPos);

        for (Ticket ticket : list) {
            if (isTicketSameTypeAndLevel(pTicket, ticket)) {
                ticket.resetTicksLeft();
                this.setDirty();
                return false;
            }
        }

        int i = getTicketLevelAt(list, true);
        int j = getTicketLevelAt(list, false);
        list.add(pTicket);
        if (pTicket.getType().doesSimulate() && pTicket.getTicketLevel() < i && this.simulationChunkUpdatedListener != null) {
            this.simulationChunkUpdatedListener.update(pChunkPos, pTicket.getTicketLevel(), true);
        }

        if (pTicket.getType().doesLoad() && pTicket.getTicketLevel() < j && this.loadingChunkUpdatedListener != null) {
            this.loadingChunkUpdatedListener.update(pChunkPos, pTicket.getTicketLevel(), true);
        }

        if (pTicket.getType().equals(TicketType.FORCED)) {
            this.chunksWithForcedTickets.add(pChunkPos);
        }

        this.setDirty();
        return true;
    }

    private static boolean isTicketSameTypeAndLevel(Ticket pFirst, Ticket pSecond) {
        return pSecond.getType() == pFirst.getType() && pSecond.getTicketLevel() == pFirst.getTicketLevel();
    }

    public int getTicketLevelAt(long pChunkPos, boolean pRequireSimulation) {
        return getTicketLevelAt(this.getTickets(pChunkPos), pRequireSimulation);
    }

    private static int getTicketLevelAt(List<Ticket> pTickets, boolean pRequireSimulation) {
        Ticket ticket = getLowestTicket(pTickets, pRequireSimulation);
        return ticket == null ? ChunkLevel.MAX_LEVEL + 1 : ticket.getTicketLevel();
    }

    @Nullable
    private static Ticket getLowestTicket(@Nullable List<Ticket> pTickets, boolean pRequireSimulation) {
        if (pTickets == null) {
            return null;
        } else {
            Ticket ticket = null;

            for (Ticket ticket1 : pTickets) {
                if (ticket == null || ticket1.getTicketLevel() < ticket.getTicketLevel()) {
                    if (pRequireSimulation && ticket1.getType().doesSimulate()) {
                        ticket = ticket1;
                    } else if (!pRequireSimulation && ticket1.getType().doesLoad()) {
                        ticket = ticket1;
                    }
                }
            }

            return ticket;
        }
    }

    public void removeTicketWithRadius(TicketType pTicketType, ChunkPos pChunkPos, int pRadius) {
        Ticket ticket = new Ticket(pTicketType, ChunkLevel.byStatus(FullChunkStatus.FULL) - pRadius);
        this.removeTicket(pChunkPos.toLong(), ticket);
    }

    public void removeTicket(Ticket pTicket, ChunkPos pChunkPos) {
        this.removeTicket(pChunkPos.toLong(), pTicket);
    }

    public boolean removeTicket(long pChunkPos, Ticket pTicket) {
        List<Ticket> list = this.tickets.get(pChunkPos);
        if (list == null) {
            return false;
        } else {
            boolean flag = false;
            Iterator<Ticket> iterator = list.iterator();

            while (iterator.hasNext()) {
                Ticket ticket = iterator.next();
                if (isTicketSameTypeAndLevel(pTicket, ticket)) {
                    iterator.remove();
                    flag = true;
                    break;
                }
            }

            if (!flag) {
                return false;
            } else {
                if (list.isEmpty()) {
                    this.tickets.remove(pChunkPos);
                }

                if (pTicket.getType().doesSimulate() && this.simulationChunkUpdatedListener != null) {
                    this.simulationChunkUpdatedListener.update(pChunkPos, getTicketLevelAt(list, true), false);
                }

                if (pTicket.getType().doesLoad() && this.loadingChunkUpdatedListener != null) {
                    this.loadingChunkUpdatedListener.update(pChunkPos, getTicketLevelAt(list, false), false);
                }

                if (pTicket.getType().equals(TicketType.FORCED)) {
                    this.updateForcedChunks();
                }

                this.setDirty();
                return true;
            }
        }
    }

    private void updateForcedChunks() {
        this.chunksWithForcedTickets = this.getAllChunksWithTicketThat(p_394883_ -> p_394883_.getType().equals(TicketType.FORCED));
    }

    public String getTicketDebugString(long pChunkPos, boolean pRequireSimulation) {
        List<Ticket> list = this.getTickets(pChunkPos);
        Ticket ticket = getLowestTicket(list, pRequireSimulation);
        return ticket == null ? "no_ticket" : ticket.toString();
    }

    public void purgeStaleTickets() {
        this.removeTicketIf(p_395227_ -> {
            p_395227_.decreaseTicksLeft();
            return p_395227_.isTimedOut();
        }, null);
        this.setDirty();
    }

    public void deactivateTicketsOnClosing() {
        this.removeTicketIf(p_392990_ -> p_392990_.getType() != TicketType.UNKNOWN, this.deactivatedTickets);
    }

    public void removeTicketIf(Predicate<Ticket> pPredicate, @Nullable Long2ObjectOpenHashMap<List<Ticket>> pTickets) {
        ObjectIterator<Entry<List<Ticket>>> objectiterator = this.tickets.long2ObjectEntrySet().fastIterator();
        boolean flag = false;

        while (objectiterator.hasNext()) {
            Entry<List<Ticket>> entry = objectiterator.next();
            Iterator<Ticket> iterator = entry.getValue().iterator();
            boolean flag1 = false;
            boolean flag2 = false;

            while (iterator.hasNext()) {
                Ticket ticket = iterator.next();
                if (pPredicate.test(ticket)) {
                    if (pTickets != null) {
                        List<Ticket> list = pTickets.computeIfAbsent(entry.getLongKey(), p_394290_ -> new ObjectArrayList<>(entry.getValue().size()));
                        list.add(ticket);
                    }

                    iterator.remove();
                    if (ticket.getType().doesLoad()) {
                        flag2 = true;
                    }

                    if (ticket.getType().doesSimulate()) {
                        flag1 = true;
                    }

                    if (ticket.getType().equals(TicketType.FORCED)) {
                        flag = true;
                    }
                }
            }

            if (flag2 || flag1) {
                if (flag2 && this.loadingChunkUpdatedListener != null) {
                    this.loadingChunkUpdatedListener.update(entry.getLongKey(), getTicketLevelAt(entry.getValue(), false), false);
                }

                if (flag1 && this.simulationChunkUpdatedListener != null) {
                    this.simulationChunkUpdatedListener.update(entry.getLongKey(), getTicketLevelAt(entry.getValue(), true), false);
                }

                this.setDirty();
                if (entry.getValue().isEmpty()) {
                    objectiterator.remove();
                }
            }
        }

        if (flag) {
            this.updateForcedChunks();
        }
    }

    public void replaceTicketLevelOfType(int pLevel, TicketType pType) {
        List<Pair<Ticket, Long>> list = new ArrayList<>();

        for (Entry<List<Ticket>> entry : this.tickets.long2ObjectEntrySet()) {
            for (Ticket ticket : entry.getValue()) {
                if (ticket.getType() == pType) {
                    list.add(Pair.of(ticket, entry.getLongKey()));
                }
            }
        }

        for (Pair<Ticket, Long> pair : list) {
            Long olong = pair.getSecond();
            Ticket ticket1 = pair.getFirst();
            this.removeTicket(olong, ticket1);
            TicketType tickettype = ticket1.getType();
            this.addTicket(olong, new Ticket(tickettype, pLevel));
        }
    }

    public boolean updateChunkForced(ChunkPos pChunkPos, boolean pAdd) {
        Ticket ticket = new Ticket(TicketType.FORCED, ChunkMap.FORCED_TICKET_LEVEL);
        return pAdd ? this.addTicket(pChunkPos.toLong(), ticket) : this.removeTicket(pChunkPos.toLong(), ticket);
    }

    public LongSet getForceLoadedChunks() {
        return this.chunksWithForcedTickets;
    }

    private LongSet getAllChunksWithTicketThat(Predicate<Ticket> pPredicate) {
        LongOpenHashSet longopenhashset = new LongOpenHashSet();

        for (Entry<List<Ticket>> entry : Long2ObjectMaps.fastIterable(this.tickets)) {
            for (Ticket ticket : entry.getValue()) {
                if (pPredicate.test(ticket)) {
                    longopenhashset.add(entry.getLongKey());
                    break;
                }
            }
        }

        return longopenhashset;
    }

    @FunctionalInterface
    public interface ChunkUpdated {
        void update(long pChunkPos, int pTicketLevel, boolean pIsDecreasing);
    }
}