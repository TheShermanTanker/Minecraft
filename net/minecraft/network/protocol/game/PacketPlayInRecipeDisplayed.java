package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.crafting.IRecipe;

public class PacketPlayInRecipeDisplayed implements Packet<PacketListenerPlayIn> {
    private final MinecraftKey recipe;

    public PacketPlayInRecipeDisplayed(IRecipe<?> recipe) {
        this.recipe = recipe.getKey();
    }

    public PacketPlayInRecipeDisplayed(PacketDataSerializer buf) {
        this.recipe = buf.readResourceLocation();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeResourceLocation(this.recipe);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleRecipeBookSeenRecipePacket(this);
    }

    public MinecraftKey getRecipe() {
        return this.recipe;
    }
}
