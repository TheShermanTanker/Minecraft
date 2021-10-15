package net.minecraft.world;

import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ITileEntityContainer;

public final class TileInventory implements ITileInventory {
    private final IChatBaseComponent title;
    private final ITileEntityContainer menuConstructor;

    public TileInventory(ITileEntityContainer baseFactory, IChatBaseComponent name) {
        this.menuConstructor = baseFactory;
        this.title = name;
    }

    @Override
    public IChatBaseComponent getScoreboardDisplayName() {
        return this.title;
    }

    @Override
    public Container createMenu(int syncId, PlayerInventory inv, EntityHuman player) {
        return this.menuConstructor.createMenu(syncId, inv, player);
    }
}
