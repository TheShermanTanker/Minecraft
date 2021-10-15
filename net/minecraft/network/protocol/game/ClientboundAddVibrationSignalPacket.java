package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.gameevent.vibrations.VibrationPath;

public class ClientboundAddVibrationSignalPacket implements Packet<PacketListenerPlayOut> {
    private final VibrationPath vibrationPath;

    public ClientboundAddVibrationSignalPacket(VibrationPath vibration) {
        this.vibrationPath = vibration;
    }

    public ClientboundAddVibrationSignalPacket(PacketDataSerializer buf) {
        this.vibrationPath = VibrationPath.read(buf);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        VibrationPath.write(buf, this.vibrationPath);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleAddVibrationSignal(this);
    }

    public VibrationPath getVibrationPath() {
        return this.vibrationPath;
    }
}
