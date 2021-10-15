package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutGameStateChange implements Packet<PacketListenerPlayOut> {
    public static final PacketPlayOutGameStateChange.Type NO_RESPAWN_BLOCK_AVAILABLE = new PacketPlayOutGameStateChange.Type(0);
    public static final PacketPlayOutGameStateChange.Type START_RAINING = new PacketPlayOutGameStateChange.Type(1);
    public static final PacketPlayOutGameStateChange.Type STOP_RAINING = new PacketPlayOutGameStateChange.Type(2);
    public static final PacketPlayOutGameStateChange.Type CHANGE_GAME_MODE = new PacketPlayOutGameStateChange.Type(3);
    public static final PacketPlayOutGameStateChange.Type WIN_GAME = new PacketPlayOutGameStateChange.Type(4);
    public static final PacketPlayOutGameStateChange.Type DEMO_EVENT = new PacketPlayOutGameStateChange.Type(5);
    public static final PacketPlayOutGameStateChange.Type ARROW_HIT_PLAYER = new PacketPlayOutGameStateChange.Type(6);
    public static final PacketPlayOutGameStateChange.Type RAIN_LEVEL_CHANGE = new PacketPlayOutGameStateChange.Type(7);
    public static final PacketPlayOutGameStateChange.Type THUNDER_LEVEL_CHANGE = new PacketPlayOutGameStateChange.Type(8);
    public static final PacketPlayOutGameStateChange.Type PUFFER_FISH_STING = new PacketPlayOutGameStateChange.Type(9);
    public static final PacketPlayOutGameStateChange.Type GUARDIAN_ELDER_EFFECT = new PacketPlayOutGameStateChange.Type(10);
    public static final PacketPlayOutGameStateChange.Type IMMEDIATE_RESPAWN = new PacketPlayOutGameStateChange.Type(11);
    public static final int DEMO_PARAM_INTRO = 0;
    public static final int DEMO_PARAM_HINT_1 = 101;
    public static final int DEMO_PARAM_HINT_2 = 102;
    public static final int DEMO_PARAM_HINT_3 = 103;
    public static final int DEMO_PARAM_HINT_4 = 104;
    private final PacketPlayOutGameStateChange.Type event;
    private final float param;

    public PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.Type reason, float value) {
        this.event = reason;
        this.param = value;
    }

    public PacketPlayOutGameStateChange(PacketDataSerializer buf) {
        this.event = PacketPlayOutGameStateChange.Type.TYPES.get(buf.readUnsignedByte());
        this.param = buf.readFloat();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeByte(this.event.id);
        buf.writeFloat(this.param);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleGameEvent(this);
    }

    public PacketPlayOutGameStateChange.Type getEvent() {
        return this.event;
    }

    public float getParam() {
        return this.param;
    }

    public static class Type {
        static final Int2ObjectMap<PacketPlayOutGameStateChange.Type> TYPES = new Int2ObjectOpenHashMap<>();
        final int id;

        public Type(int id) {
            this.id = id;
            TYPES.put(id, this);
        }
    }
}
