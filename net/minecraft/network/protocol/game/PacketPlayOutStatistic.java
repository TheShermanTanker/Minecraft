package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.stats.Statistic;
import net.minecraft.stats.StatisticWrapper;

public class PacketPlayOutStatistic implements Packet<PacketListenerPlayOut> {
    private final Object2IntMap<Statistic<?>> stats;

    public PacketPlayOutStatistic(Object2IntMap<Statistic<?>> stats) {
        this.stats = stats;
    }

    public PacketPlayOutStatistic(PacketDataSerializer buf) {
        this.stats = buf.readMap(Object2IntOpenHashMap::new, (bufx) -> {
            int i = bufx.readVarInt();
            int j = bufx.readVarInt();
            return readStatCap(IRegistry.STAT_TYPE.fromId(i), j);
        }, PacketDataSerializer::readVarInt);
    }

    private static <T> Statistic<T> readStatCap(StatisticWrapper<T> statType, int id) {
        return statType.get(statType.getRegistry().fromId(id));
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleAwardStats(this);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeMap(this.stats, (bufx, stat) -> {
            bufx.writeVarInt(IRegistry.STAT_TYPE.getId(stat.getWrapper()));
            bufx.writeVarInt(this.getStatIdCap(stat));
        }, PacketDataSerializer::writeVarInt);
    }

    private <T> int getStatIdCap(Statistic<T> stat) {
        return stat.getWrapper().getRegistry().getId(stat.getValue());
    }

    public Map<Statistic<?>, Integer> getStats() {
        return this.stats;
    }
}
