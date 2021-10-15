package net.minecraft.network.protocol.game;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.stats.RecipeBookSettings;

public class PacketPlayOutRecipes implements Packet<PacketListenerPlayOut> {
    private final PacketPlayOutRecipes.Action state;
    private final List<MinecraftKey> recipes;
    private final List<MinecraftKey> toHighlight;
    private final RecipeBookSettings bookSettings;

    public PacketPlayOutRecipes(PacketPlayOutRecipes.Action action, Collection<MinecraftKey> recipeIdsToChange, Collection<MinecraftKey> recipeIdsToInit, RecipeBookSettings options) {
        this.state = action;
        this.recipes = ImmutableList.copyOf(recipeIdsToChange);
        this.toHighlight = ImmutableList.copyOf(recipeIdsToInit);
        this.bookSettings = options;
    }

    public PacketPlayOutRecipes(PacketDataSerializer buf) {
        this.state = buf.readEnum(PacketPlayOutRecipes.Action.class);
        this.bookSettings = RecipeBookSettings.read(buf);
        this.recipes = buf.readList(PacketDataSerializer::readResourceLocation);
        if (this.state == PacketPlayOutRecipes.Action.INIT) {
            this.toHighlight = buf.readList(PacketDataSerializer::readResourceLocation);
        } else {
            this.toHighlight = ImmutableList.of();
        }

    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeEnum(this.state);
        this.bookSettings.write(buf);
        buf.writeCollection(this.recipes, PacketDataSerializer::writeResourceLocation);
        if (this.state == PacketPlayOutRecipes.Action.INIT) {
            buf.writeCollection(this.toHighlight, PacketDataSerializer::writeResourceLocation);
        }

    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleAddOrRemoveRecipes(this);
    }

    public List<MinecraftKey> getRecipes() {
        return this.recipes;
    }

    public List<MinecraftKey> getHighlights() {
        return this.toHighlight;
    }

    public RecipeBookSettings getBookSettings() {
        return this.bookSettings;
    }

    public PacketPlayOutRecipes.Action getState() {
        return this.state;
    }

    public static enum Action {
        INIT,
        ADD,
        REMOVE;
    }
}
