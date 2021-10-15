package net.minecraft.server.level;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutBoss;
import net.minecraft.util.MathHelper;
import net.minecraft.world.BossBattle;

public class BossBattleServer extends BossBattle {
    private final Set<EntityPlayer> players = Sets.newHashSet();
    private final Set<EntityPlayer> unmodifiablePlayers = Collections.unmodifiableSet(this.players);
    public boolean visible = true;

    public BossBattleServer(IChatBaseComponent displayName, BossBattle.BarColor color, BossBattle.BarStyle style) {
        super(MathHelper.createInsecureUUID(), displayName, color, style);
    }

    @Override
    public void setProgress(float percentage) {
        if (percentage != this.progress) {
            super.setProgress(percentage);
            this.sendUpdate(PacketPlayOutBoss::createUpdateProgressPacket);
        }

    }

    @Override
    public void setColor(BossBattle.BarColor color) {
        if (color != this.color) {
            super.setColor(color);
            this.sendUpdate(PacketPlayOutBoss::createUpdateStylePacket);
        }

    }

    @Override
    public void setOverlay(BossBattle.BarStyle style) {
        if (style != this.overlay) {
            super.setOverlay(style);
            this.sendUpdate(PacketPlayOutBoss::createUpdateStylePacket);
        }

    }

    @Override
    public BossBattle setDarkenSky(boolean darkenSky) {
        if (darkenSky != this.darkenScreen) {
            super.setDarkenSky(darkenSky);
            this.sendUpdate(PacketPlayOutBoss::createUpdatePropertiesPacket);
        }

        return this;
    }

    @Override
    public BossBattle setPlayMusic(boolean dragonMusic) {
        if (dragonMusic != this.playBossMusic) {
            super.setPlayMusic(dragonMusic);
            this.sendUpdate(PacketPlayOutBoss::createUpdatePropertiesPacket);
        }

        return this;
    }

    @Override
    public BossBattle setCreateFog(boolean thickenFog) {
        if (thickenFog != this.createWorldFog) {
            super.setCreateFog(thickenFog);
            this.sendUpdate(PacketPlayOutBoss::createUpdatePropertiesPacket);
        }

        return this;
    }

    @Override
    public void setName(IChatBaseComponent name) {
        if (!Objects.equal(name, this.name)) {
            super.setName(name);
            this.sendUpdate(PacketPlayOutBoss::createUpdateNamePacket);
        }

    }

    public void sendUpdate(Function<BossBattle, PacketPlayOutBoss> bossBarToPacketFunction) {
        if (this.visible) {
            PacketPlayOutBoss clientboundBossEventPacket = bossBarToPacketFunction.apply(this);

            for(EntityPlayer serverPlayer : this.players) {
                serverPlayer.connection.sendPacket(clientboundBossEventPacket);
            }
        }

    }

    public void addPlayer(EntityPlayer player) {
        if (this.players.add(player) && this.visible) {
            player.connection.sendPacket(PacketPlayOutBoss.createAddPacket(this));
        }

    }

    public void removePlayer(EntityPlayer player) {
        if (this.players.remove(player) && this.visible) {
            player.connection.sendPacket(PacketPlayOutBoss.createRemovePacket(this.getId()));
        }

    }

    public void removeAllPlayers() {
        if (!this.players.isEmpty()) {
            for(EntityPlayer serverPlayer : Lists.newArrayList(this.players)) {
                this.removePlayer(serverPlayer);
            }
        }

    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        if (visible != this.visible) {
            this.visible = visible;

            for(EntityPlayer serverPlayer : this.players) {
                serverPlayer.connection.sendPacket(visible ? PacketPlayOutBoss.createAddPacket(this) : PacketPlayOutBoss.createRemovePacket(this.getId()));
            }
        }

    }

    public Collection<EntityPlayer> getPlayers() {
        return this.unmodifiablePlayers;
    }
}
