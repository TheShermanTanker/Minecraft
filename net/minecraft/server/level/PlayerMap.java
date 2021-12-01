package net.minecraft.server.level;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.Set;

public final class PlayerMap {
    private final Object2BooleanMap<EntityPlayer> players = new Object2BooleanOpenHashMap<>();

    public Set<EntityPlayer> getPlayers(long l) {
        return this.players.keySet();
    }

    public void addPlayer(long l, EntityPlayer player, boolean watchDisabled) {
        this.players.put(player, watchDisabled);
    }

    public void removePlayer(long l, EntityPlayer player) {
        this.players.removeBoolean(player);
    }

    public void ignorePlayer(EntityPlayer player) {
        this.players.replace(player, true);
    }

    public void unIgnorePlayer(EntityPlayer player) {
        this.players.replace(player, false);
    }

    public boolean ignoredOrUnknown(EntityPlayer player) {
        return this.players.getOrDefault(player, true);
    }

    public boolean ignored(EntityPlayer player) {
        return this.players.getBoolean(player);
    }

    public void updatePlayer(long prevPos, long currentPos, EntityPlayer player) {
    }
}
