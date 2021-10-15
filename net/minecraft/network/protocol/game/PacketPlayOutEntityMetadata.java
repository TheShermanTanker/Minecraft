package net.minecraft.network.protocol.game;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.DataWatcher;

public class PacketPlayOutEntityMetadata implements Packet<PacketListenerPlayOut> {
    private final int id;
    @Nullable
    private final List<DataWatcher.Item<?>> packedItems;

    public PacketPlayOutEntityMetadata(int id, DataWatcher tracker, boolean forceUpdateAll) {
        this.id = id;
        if (forceUpdateAll) {
            this.packedItems = tracker.getAll();
            tracker.clearDirty();
        } else {
            this.packedItems = tracker.packDirty();
        }

    }

    public PacketPlayOutEntityMetadata(PacketDataSerializer buf) {
        this.id = buf.readVarInt();
        this.packedItems = DataWatcher.unpack(buf);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.id);
        DataWatcher.pack(this.packedItems, buf);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetEntityData(this);
    }

    @Nullable
    public List<DataWatcher.Item<?>> getUnpackedData() {
        return this.packedItems;
    }

    public int getId() {
        return this.id;
    }
}
