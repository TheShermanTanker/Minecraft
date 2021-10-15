package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardDisplayObjective;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardObjective;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardScore;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.scores.PersistentScoreboard;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardObjective;
import net.minecraft.world.scores.ScoreboardScore;
import net.minecraft.world.scores.ScoreboardTeam;

public class ScoreboardServer extends Scoreboard {
    private final MinecraftServer server;
    private final Set<ScoreboardObjective> trackedObjectives = Sets.newHashSet();
    private final List<Runnable> dirtyListeners = Lists.newArrayList();

    public ScoreboardServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void handleScoreChanged(ScoreboardScore score) {
        super.handleScoreChanged(score);
        if (this.trackedObjectives.contains(score.getObjective())) {
            this.server.getPlayerList().sendAll(new PacketPlayOutScoreboardScore(ScoreboardServer.Action.CHANGE, score.getObjective().getName(), score.getPlayerName(), score.getScore()));
        }

        this.setDirty();
    }

    @Override
    public void handlePlayerRemoved(String playerName) {
        super.handlePlayerRemoved(playerName);
        this.server.getPlayerList().sendAll(new PacketPlayOutScoreboardScore(ScoreboardServer.Action.REMOVE, (String)null, playerName, 0));
        this.setDirty();
    }

    @Override
    public void onPlayerScoreRemoved(String playerName, ScoreboardObjective objective) {
        super.onPlayerScoreRemoved(playerName, objective);
        if (this.trackedObjectives.contains(objective)) {
            this.server.getPlayerList().sendAll(new PacketPlayOutScoreboardScore(ScoreboardServer.Action.REMOVE, objective.getName(), playerName, 0));
        }

        this.setDirty();
    }

    @Override
    public void setDisplaySlot(int slot, @Nullable ScoreboardObjective objective) {
        ScoreboardObjective objective2 = this.getObjectiveForSlot(slot);
        super.setDisplaySlot(slot, objective);
        if (objective2 != objective && objective2 != null) {
            if (this.getObjectiveDisplaySlotCount(objective2) > 0) {
                this.server.getPlayerList().sendAll(new PacketPlayOutScoreboardDisplayObjective(slot, objective));
            } else {
                this.stopTrackingObjective(objective2);
            }
        }

        if (objective != null) {
            if (this.trackedObjectives.contains(objective)) {
                this.server.getPlayerList().sendAll(new PacketPlayOutScoreboardDisplayObjective(slot, objective));
            } else {
                this.startTrackingObjective(objective);
            }
        }

        this.setDirty();
    }

    @Override
    public boolean addPlayerToTeam(String playerName, ScoreboardTeam team) {
        if (super.addPlayerToTeam(playerName, team)) {
            this.server.getPlayerList().sendAll(PacketPlayOutScoreboardTeam.createPlayerPacket(team, playerName, PacketPlayOutScoreboardTeam.Action.ADD));
            this.setDirty();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void removePlayerFromTeam(String playerName, ScoreboardTeam team) {
        super.removePlayerFromTeam(playerName, team);
        this.server.getPlayerList().sendAll(PacketPlayOutScoreboardTeam.createPlayerPacket(team, playerName, PacketPlayOutScoreboardTeam.Action.REMOVE));
        this.setDirty();
    }

    @Override
    public void handleObjectiveAdded(ScoreboardObjective objective) {
        super.handleObjectiveAdded(objective);
        this.setDirty();
    }

    @Override
    public void handleObjectiveChanged(ScoreboardObjective objective) {
        super.handleObjectiveChanged(objective);
        if (this.trackedObjectives.contains(objective)) {
            this.server.getPlayerList().sendAll(new PacketPlayOutScoreboardObjective(objective, 2));
        }

        this.setDirty();
    }

    @Override
    public void handleObjectiveRemoved(ScoreboardObjective objective) {
        super.handleObjectiveRemoved(objective);
        if (this.trackedObjectives.contains(objective)) {
            this.stopTrackingObjective(objective);
        }

        this.setDirty();
    }

    @Override
    public void handleTeamAdded(ScoreboardTeam team) {
        super.handleTeamAdded(team);
        this.server.getPlayerList().sendAll(PacketPlayOutScoreboardTeam.createAddOrModifyPacket(team, true));
        this.setDirty();
    }

    @Override
    public void handleTeamChanged(ScoreboardTeam team) {
        super.handleTeamChanged(team);
        this.server.getPlayerList().sendAll(PacketPlayOutScoreboardTeam.createAddOrModifyPacket(team, false));
        this.setDirty();
    }

    @Override
    public void handleTeamRemoved(ScoreboardTeam team) {
        super.handleTeamRemoved(team);
        this.server.getPlayerList().sendAll(PacketPlayOutScoreboardTeam.createRemovePacket(team));
        this.setDirty();
    }

    public void addDirtyListener(Runnable listener) {
        this.dirtyListeners.add(listener);
    }

    protected void setDirty() {
        for(Runnable runnable : this.dirtyListeners) {
            runnable.run();
        }

    }

    public List<Packet<?>> getScoreboardScorePacketsForObjective(ScoreboardObjective objective) {
        List<Packet<?>> list = Lists.newArrayList();
        list.add(new PacketPlayOutScoreboardObjective(objective, 0));

        for(int i = 0; i < 19; ++i) {
            if (this.getObjectiveForSlot(i) == objective) {
                list.add(new PacketPlayOutScoreboardDisplayObjective(i, objective));
            }
        }

        for(ScoreboardScore score : this.getScoresForObjective(objective)) {
            list.add(new PacketPlayOutScoreboardScore(ScoreboardServer.Action.CHANGE, score.getObjective().getName(), score.getPlayerName(), score.getScore()));
        }

        return list;
    }

    public void startTrackingObjective(ScoreboardObjective objective) {
        List<Packet<?>> list = this.getScoreboardScorePacketsForObjective(objective);

        for(EntityPlayer serverPlayer : this.server.getPlayerList().getPlayers()) {
            for(Packet<?> packet : list) {
                serverPlayer.connection.sendPacket(packet);
            }
        }

        this.trackedObjectives.add(objective);
    }

    public List<Packet<?>> getStopTrackingPackets(ScoreboardObjective objective) {
        List<Packet<?>> list = Lists.newArrayList();
        list.add(new PacketPlayOutScoreboardObjective(objective, 1));

        for(int i = 0; i < 19; ++i) {
            if (this.getObjectiveForSlot(i) == objective) {
                list.add(new PacketPlayOutScoreboardDisplayObjective(i, objective));
            }
        }

        return list;
    }

    public void stopTrackingObjective(ScoreboardObjective objective) {
        List<Packet<?>> list = this.getStopTrackingPackets(objective);

        for(EntityPlayer serverPlayer : this.server.getPlayerList().getPlayers()) {
            for(Packet<?> packet : list) {
                serverPlayer.connection.sendPacket(packet);
            }
        }

        this.trackedObjectives.remove(objective);
    }

    public int getObjectiveDisplaySlotCount(ScoreboardObjective objective) {
        int i = 0;

        for(int j = 0; j < 19; ++j) {
            if (this.getObjectiveForSlot(j) == objective) {
                ++i;
            }
        }

        return i;
    }

    public PersistentScoreboard createData() {
        PersistentScoreboard scoreboardSaveData = new PersistentScoreboard(this);
        this.addDirtyListener(scoreboardSaveData::setDirty);
        return scoreboardSaveData;
    }

    public PersistentScoreboard createData(NBTTagCompound nbt) {
        return this.createData().load(nbt);
    }

    public static enum Action {
        CHANGE,
        REMOVE;
    }
}
