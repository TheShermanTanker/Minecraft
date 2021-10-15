package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.crafting.IRecipe;

public class PacketPlayInAutoRecipe implements Packet<PacketListenerPlayIn> {
    private final int containerId;
    private final MinecraftKey recipe;
    private final boolean shiftDown;

    public PacketPlayInAutoRecipe(int syncId, IRecipe<?> recipe, boolean craftAll) {
        this.containerId = syncId;
        this.recipe = recipe.getKey();
        this.shiftDown = craftAll;
    }

    public PacketPlayInAutoRecipe(PacketDataSerializer buf) {
        this.containerId = buf.readByte();
        this.recipe = buf.readResourceLocation();
        this.shiftDown = buf.readBoolean();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeByte(this.containerId);
        buf.writeResourceLocation(this.recipe);
        buf.writeBoolean(this.shiftDown);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handlePlaceRecipe(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public MinecraftKey getRecipe() {
        return this.recipe;
    }

    public boolean isShiftDown() {
        return this.shiftDown;
    }
}
