package net.minecraft.server.level;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.ArraySetSorted;
import net.minecraft.world.level.ChunkCoordIntPair;

public class TickingTracker extends ChunkMap {
    private static final int INITIAL_TICKET_LIST_CAPACITY = 4;
    protected final Long2ByteMap chunks = new Long2ByteOpenHashMap();
    private final Long2ObjectOpenHashMap<ArraySetSorted<Ticket<?>>> tickets = new Long2ObjectOpenHashMap<>();

    public TickingTracker() {
        super(34, 16, 256);
        this.chunks.defaultReturnValue((byte)33);
    }

    private ArraySetSorted<Ticket<?>> getTickets(long pos) {
        return this.tickets.computeIfAbsent(pos, (p) -> {
            return ArraySetSorted.create(4);
        });
    }

    private int getTicketLevelAt(ArraySetSorted<Ticket<?>> ticket) {
        return ticket.isEmpty() ? 34 : ticket.first().getTicketLevel();
    }

    public void addTicket(long pos, Ticket<?> ticket) {
        ArraySetSorted<Ticket<?>> sortedArraySet = this.getTickets(pos);
        int i = this.getTicketLevelAt(sortedArraySet);
        sortedArraySet.add(ticket);
        if (ticket.getTicketLevel() < i) {
            this.update(pos, ticket.getTicketLevel(), true);
        }

    }

    public void removeTicket(long pos, Ticket<?> ticket) {
        ArraySetSorted<Ticket<?>> sortedArraySet = this.getTickets(pos);
        sortedArraySet.remove(ticket);
        if (sortedArraySet.isEmpty()) {
            this.tickets.remove(pos);
        }

        this.update(pos, this.getTicketLevelAt(sortedArraySet), false);
    }

    public <T> void addTicket(TicketType<T> type, ChunkCoordIntPair pos, int level, T argument) {
        this.addTicket(pos.pair(), new Ticket<>(type, level, argument));
    }

    public <T> void removeTicket(TicketType<T> type, ChunkCoordIntPair pos, int level, T argument) {
        Ticket<T> ticket = new Ticket<>(type, level, argument);
        this.removeTicket(pos.pair(), ticket);
    }

    public void replacePlayerTicketsLevel(int level) {
        List<Pair<Ticket<ChunkCoordIntPair>, Long>> list = new ArrayList<>();

        for(Entry<ArraySetSorted<Ticket<?>>> entry : this.tickets.long2ObjectEntrySet()) {
            for(Ticket<?> ticket : entry.getValue()) {
                if (ticket.getTicketType() == TicketType.PLAYER) {
                    list.add(Pair.of(ticket, entry.getLongKey()));
                }
            }
        }

        for(Pair<Ticket<ChunkCoordIntPair>, Long> pair : list) {
            Long long_ = pair.getSecond();
            Ticket<ChunkCoordIntPair> ticket2 = pair.getFirst();
            this.removeTicket(long_, ticket2);
            ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(long_);
            TicketType<ChunkCoordIntPair> ticketType = ticket2.getTicketType();
            this.addTicket(ticketType, chunkPos, level, chunkPos);
        }

    }

    @Override
    protected int getLevelFromSource(long id) {
        ArraySetSorted<Ticket<?>> sortedArraySet = this.tickets.get(id);
        return sortedArraySet != null && !sortedArraySet.isEmpty() ? sortedArraySet.first().getTicketLevel() : Integer.MAX_VALUE;
    }

    public int getLevel(ChunkCoordIntPair pos) {
        return this.getLevel(pos.pair());
    }

    @Override
    protected int getLevel(long id) {
        return this.chunks.get(id);
    }

    @Override
    protected void setLevel(long id, int level) {
        if (level > 33) {
            this.chunks.remove(id);
        } else {
            this.chunks.put(id, (byte)level);
        }

    }

    public void runAllUpdates() {
        this.runUpdates(Integer.MAX_VALUE);
    }

    public String getTicketDebugString(long pos) {
        ArraySetSorted<Ticket<?>> sortedArraySet = this.tickets.get(pos);
        return sortedArraySet != null && !sortedArraySet.isEmpty() ? sortedArraySet.first().toString() : "no_ticket";
    }
}
