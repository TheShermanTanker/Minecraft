package net.minecraft.world.level.timers;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.primitives.UnsignedLong;
import com.mojang.serialization.Dynamic;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CustomFunctionCallbackTimerQueue<T> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CALLBACK_DATA_TAG = "Callback";
    private static final String TIMER_NAME_TAG = "Name";
    private static final String TIMER_TRIGGER_TIME_TAG = "TriggerTime";
    private final CustomFunctionCallbackTimers<T> callbacksRegistry;
    private final Queue<CustomFunctionCallbackTimerQueue.Event<T>> queue = new PriorityQueue<>(createComparator());
    private UnsignedLong sequentialId = UnsignedLong.ZERO;
    private final Table<String, Long, CustomFunctionCallbackTimerQueue.Event<T>> events = HashBasedTable.create();

    private static <T> Comparator<CustomFunctionCallbackTimerQueue.Event<T>> createComparator() {
        return Comparator.comparingLong((event) -> {
            return event.triggerTime;
        }).thenComparing((event) -> {
            return event.sequentialId;
        });
    }

    public CustomFunctionCallbackTimerQueue(CustomFunctionCallbackTimers<T> timerCallbackSerializer, Stream<Dynamic<NBTBase>> stream) {
        this(timerCallbackSerializer);
        this.queue.clear();
        this.events.clear();
        this.sequentialId = UnsignedLong.ZERO;
        stream.forEach((dynamic) -> {
            if (!(dynamic.getValue() instanceof NBTTagCompound)) {
                LOGGER.warn("Invalid format of events: {}", (Object)dynamic);
            } else {
                this.loadEvent((NBTTagCompound)dynamic.getValue());
            }
        });
    }

    public CustomFunctionCallbackTimerQueue(CustomFunctionCallbackTimers<T> timerCallbackSerializer) {
        this.callbacksRegistry = timerCallbackSerializer;
    }

    public void tick(T server, long time) {
        while(true) {
            CustomFunctionCallbackTimerQueue.Event<T> event = this.queue.peek();
            if (event == null || event.triggerTime > time) {
                return;
            }

            this.queue.remove();
            this.events.remove(event.id, time);
            event.callback.handle(server, this, time);
        }
    }

    public void schedule(String name, long triggerTime, CustomFunctionCallbackTimer<T> callback) {
        if (!this.events.contains(name, triggerTime)) {
            this.sequentialId = this.sequentialId.plus(UnsignedLong.ONE);
            CustomFunctionCallbackTimerQueue.Event<T> event = new CustomFunctionCallbackTimerQueue.Event<>(triggerTime, this.sequentialId, name, callback);
            this.events.put(name, triggerTime, event);
            this.queue.add(event);
        }
    }

    public int remove(String string) {
        Collection<CustomFunctionCallbackTimerQueue.Event<T>> collection = this.events.row(string).values();
        collection.forEach(this.queue::remove);
        int i = collection.size();
        collection.clear();
        return i;
    }

    public Set<String> getEventsIds() {
        return Collections.unmodifiableSet(this.events.rowKeySet());
    }

    private void loadEvent(NBTTagCompound nbt) {
        NBTTagCompound compoundTag = nbt.getCompound("Callback");
        CustomFunctionCallbackTimer<T> timerCallback = this.callbacksRegistry.deserialize(compoundTag);
        if (timerCallback != null) {
            String string = nbt.getString("Name");
            long l = nbt.getLong("TriggerTime");
            this.schedule(string, l, timerCallback);
        }

    }

    private NBTTagCompound storeEvent(CustomFunctionCallbackTimerQueue.Event<T> event) {
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setString("Name", event.id);
        compoundTag.setLong("TriggerTime", event.triggerTime);
        compoundTag.set("Callback", this.callbacksRegistry.serialize(event.callback));
        return compoundTag;
    }

    public NBTTagList store() {
        NBTTagList listTag = new NBTTagList();
        this.queue.stream().sorted(createComparator()).map(this::storeEvent).forEach(listTag::add);
        return listTag;
    }

    public static class Event<T> {
        public final long triggerTime;
        public final UnsignedLong sequentialId;
        public final String id;
        public final CustomFunctionCallbackTimer<T> callback;

        Event(long l, UnsignedLong unsignedLong, String string, CustomFunctionCallbackTimer<T> timerCallback) {
            this.triggerTime = l;
            this.sequentialId = unsignedLong;
            this.id = string;
            this.callback = timerCallback;
        }
    }
}
