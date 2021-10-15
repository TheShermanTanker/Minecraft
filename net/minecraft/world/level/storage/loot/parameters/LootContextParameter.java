package net.minecraft.world.level.storage.loot.parameters;

import net.minecraft.resources.MinecraftKey;

public class LootContextParameter<T> {
    private final MinecraftKey name;

    public LootContextParameter(MinecraftKey id) {
        this.name = id;
    }

    public MinecraftKey getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return "<parameter " + this.name + ">";
    }
}
