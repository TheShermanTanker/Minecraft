package net.minecraft.world.item;

import net.minecraft.network.protocol.game.PacketPlayOutSetCooldown;
import net.minecraft.server.level.EntityPlayer;

public class ItemCooldownPlayer extends ItemCooldown {
    private final EntityPlayer player;

    public ItemCooldownPlayer(EntityPlayer player) {
        this.player = player;
    }

    @Override
    protected void onCooldownStarted(Item item, int duration) {
        super.onCooldownStarted(item, duration);
        this.player.connection.sendPacket(new PacketPlayOutSetCooldown(item, duration));
    }

    @Override
    protected void onCooldownEnded(Item item) {
        super.onCooldownEnded(item);
        this.player.connection.sendPacket(new PacketPlayOutSetCooldown(item, 0));
    }
}
