package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.inventory.RecipeBookType;

public class PacketPlayInRecipeSettings implements Packet<PacketListenerPlayIn> {
    private final RecipeBookType bookType;
    private final boolean isOpen;
    private final boolean isFiltering;

    public PacketPlayInRecipeSettings(RecipeBookType category, boolean guiOpen, boolean filteringCraftable) {
        this.bookType = category;
        this.isOpen = guiOpen;
        this.isFiltering = filteringCraftable;
    }

    public PacketPlayInRecipeSettings(PacketDataSerializer buf) {
        this.bookType = buf.readEnum(RecipeBookType.class);
        this.isOpen = buf.readBoolean();
        this.isFiltering = buf.readBoolean();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeEnum(this.bookType);
        buf.writeBoolean(this.isOpen);
        buf.writeBoolean(this.isFiltering);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleRecipeBookChangeSettingsPacket(this);
    }

    public RecipeBookType getBookType() {
        return this.bookType;
    }

    public boolean isOpen() {
        return this.isOpen;
    }

    public boolean isFiltering() {
        return this.isFiltering;
    }
}
