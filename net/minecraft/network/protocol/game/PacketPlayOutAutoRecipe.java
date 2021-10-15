package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.crafting.IRecipe;

public class PacketPlayOutAutoRecipe implements Packet<PacketListenerPlayOut> {
    private final int containerId;
    private final MinecraftKey recipe;

    public PacketPlayOutAutoRecipe(int syncId, IRecipe<?> recipe) {
        this.containerId = syncId;
        this.recipe = recipe.getKey();
    }

    public PacketPlayOutAutoRecipe(PacketDataSerializer buf) {
        this.containerId = buf.readByte();
        this.recipe = buf.readResourceLocation();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeByte(this.containerId);
        buf.writeResourceLocation(this.recipe);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handlePlaceRecipe(this);
    }

    public MinecraftKey getRecipe() {
        return this.recipe;
    }

    public int getContainerId() {
        return this.containerId;
    }
}
