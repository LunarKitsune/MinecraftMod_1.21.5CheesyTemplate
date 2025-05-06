package net.minecraft.world.level.timers;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.primitives.UnsignedLong;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;

public class TimerQueue<T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CALLBACK_DATA_TAG = "Callback";
    private static final String TIMER_NAME_TAG = "Name";
    private static final String TIMER_TRIGGER_TIME_TAG = "TriggerTime";
    private final TimerCallbacks<T> callbacksRegistry;
    private final Queue<TimerQueue.Event<T>> queue = new PriorityQueue<>(createComparator());
    private UnsignedLong sequentialId = UnsignedLong.ZERO;
    private final Table<String, Long, TimerQueue.Event<T>> events = HashBasedTable.create();

    private static <T> Comparator<TimerQueue.Event<T>> createComparator() {
        return Comparator.<TimerQueue.Event<T>>comparingLong(p_82272_ -> p_82272_.triggerTime).thenComparing(p_82269_ -> p_82269_.sequentialId);
    }

    public TimerQueue(TimerCallbacks<T> pCallbacksRegistry, Stream<? extends Dynamic<?>> pScheduledEventsDynamic) {
        this(pCallbacksRegistry);
        this.queue.clear();
        this.events.clear();
        this.sequentialId = UnsignedLong.ZERO;
        pScheduledEventsDynamic.forEach(p_265027_ -> {
            Tag tag = p_265027_.convert(NbtOps.INSTANCE).getValue();
            if (tag instanceof CompoundTag compoundtag) {
                this.loadEvent(compoundtag);
            } else {
                LOGGER.warn("Invalid format of events: {}", tag);
            }
        });
    }

    public TimerQueue(TimerCallbacks<T> pCallbacksRegistry) {
        this.callbacksRegistry = pCallbacksRegistry;
    }

    public void tick(T pObj, long pGameTime) {
        while (true) {
            TimerQueue.Event<T> event = this.queue.peek();
            if (event == null || event.triggerTime > pGameTime) {
                return;
            }

            this.queue.remove();
            this.events.remove(event.id, pGameTime);
            event.callback.handle(pObj, this, pGameTime);
        }
    }

    public void schedule(String pId, long pTriggerTime, TimerCallback<T> pCallback) {
        if (!this.events.contains(pId, pTriggerTime)) {
            this.sequentialId = this.sequentialId.plus(UnsignedLong.ONE);
            TimerQueue.Event<T> event = new TimerQueue.Event<>(pTriggerTime, this.sequentialId, pId, pCallback);
            this.events.put(pId, pTriggerTime, event);
            this.queue.add(event);
        }
    }

    public int remove(String pEventId) {
        Collection<TimerQueue.Event<T>> collection = this.events.row(pEventId).values();
        collection.forEach(this.queue::remove);
        int i = collection.size();
        collection.clear();
        return i;
    }

    public Set<String> getEventsIds() {
        return Collections.unmodifiableSet(this.events.rowKeySet());
    }

    private void loadEvent(CompoundTag pTag) {
        TimerCallback<T> timercallback = pTag.read("Callback", this.callbacksRegistry.codec()).orElse(null);
        if (timercallback != null) {
            String s = pTag.getStringOr("Name", "");
            long i = pTag.getLongOr("TriggerTime", 0L);
            this.schedule(s, i, timercallback);
        }
    }

    private CompoundTag storeEvent(TimerQueue.Event<T> pEvent) {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putString("Name", pEvent.id);
        compoundtag.putLong("TriggerTime", pEvent.triggerTime);
        compoundtag.store("Callback", this.callbacksRegistry.codec(), pEvent.callback);
        return compoundtag;
    }

    public ListTag store() {
        ListTag listtag = new ListTag();
        this.queue.stream().sorted(createComparator()).map(this::storeEvent).forEach(listtag::add);
        return listtag;
    }

    public static class Event<T> {
        public final long triggerTime;
        public final UnsignedLong sequentialId;
        public final String id;
        public final TimerCallback<T> callback;

        Event(long pTriggerTime, UnsignedLong pSequentialId, String pId, TimerCallback<T> pCallback) {
            this.triggerTime = pTriggerTime;
            this.sequentialId = pSequentialId;
            this.id = pId;
            this.callback = pCallback;
        }
    }
}