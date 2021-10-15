package net.minecraft.world.scores;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.scores.criteria.IScoreboardCriteria;

public class Scoreboard {
    public static final int DISPLAY_SLOT_LIST = 0;
    public static final int DISPLAY_SLOT_SIDEBAR = 1;
    public static final int DISPLAY_SLOT_BELOW_NAME = 2;
    public static final int DISPLAY_SLOT_TEAMS_SIDEBAR_START = 3;
    public static final int DISPLAY_SLOT_TEAMS_SIDEBAR_END = 18;
    public static final int DISPLAY_SLOTS = 19;
    public static final int MAX_NAME_LENGTH = 40;
    private final Map<String, ScoreboardObjective> objectivesByName = Maps.newHashMap();
    private final Map<IScoreboardCriteria, List<ScoreboardObjective>> objectivesByCriteria = Maps.newHashMap();
    private final Map<String, Map<ScoreboardObjective, ScoreboardScore>> playerScores = Maps.newHashMap();
    private final ScoreboardObjective[] displayObjectives = new ScoreboardObjective[19];
    private final Map<String, ScoreboardTeam> teamsByName = Maps.newHashMap();
    private final Map<String, ScoreboardTeam> teamsByPlayer = Maps.newHashMap();
    private static String[] displaySlotNames;

    public boolean hasObjective(String name) {
        return this.objectivesByName.containsKey(name);
    }

    public ScoreboardObjective getOrCreateObjective(String name) {
        return this.objectivesByName.get(name);
    }

    @Nullable
    public ScoreboardObjective getObjective(@Nullable String name) {
        return this.objectivesByName.get(name);
    }

    public ScoreboardObjective registerObjective(String name, IScoreboardCriteria criterion, IChatBaseComponent displayName, IScoreboardCriteria.EnumScoreboardHealthDisplay renderType) {
        if (name.length() > 16) {
            throw new IllegalArgumentException("The objective name '" + name + "' is too long!");
        } else if (this.objectivesByName.containsKey(name)) {
            throw new IllegalArgumentException("An objective with the name '" + name + "' already exists!");
        } else {
            ScoreboardObjective objective = new ScoreboardObjective(this, name, criterion, displayName, renderType);
            this.objectivesByCriteria.computeIfAbsent(criterion, (criterionx) -> {
                return Lists.newArrayList();
            }).add(objective);
            this.objectivesByName.put(name, objective);
            this.handleObjectiveAdded(objective);
            return objective;
        }
    }

    public final void getObjectivesForCriteria(IScoreboardCriteria criterion, String player, Consumer<ScoreboardScore> action) {
        this.objectivesByCriteria.getOrDefault(criterion, Collections.emptyList()).forEach((objective) -> {
            action.accept(this.getPlayerScoreForObjective(player, objective));
        });
    }

    public boolean hasPlayerScore(String playerName, ScoreboardObjective objective) {
        Map<ScoreboardObjective, ScoreboardScore> map = this.playerScores.get(playerName);
        if (map == null) {
            return false;
        } else {
            ScoreboardScore score = map.get(objective);
            return score != null;
        }
    }

    public ScoreboardScore getPlayerScoreForObjective(String player, ScoreboardObjective objective) {
        if (player.length() > 40) {
            throw new IllegalArgumentException("The player name '" + player + "' is too long!");
        } else {
            Map<ScoreboardObjective, ScoreboardScore> map = this.playerScores.computeIfAbsent(player, (string) -> {
                return Maps.newHashMap();
            });
            return map.computeIfAbsent(objective, (objectivex) -> {
                ScoreboardScore score = new ScoreboardScore(this, objectivex, player);
                score.setScore(0);
                return score;
            });
        }
    }

    public Collection<ScoreboardScore> getScoresForObjective(ScoreboardObjective objective) {
        List<ScoreboardScore> list = Lists.newArrayList();

        for(Map<ScoreboardObjective, ScoreboardScore> map : this.playerScores.values()) {
            ScoreboardScore score = map.get(objective);
            if (score != null) {
                list.add(score);
            }
        }

        list.sort(ScoreboardScore.SCORE_COMPARATOR);
        return list;
    }

    public Collection<ScoreboardObjective> getObjectives() {
        return this.objectivesByName.values();
    }

    public Collection<String> getObjectiveNames() {
        return this.objectivesByName.keySet();
    }

    public Collection<String> getPlayers() {
        return Lists.newArrayList(this.playerScores.keySet());
    }

    public void resetPlayerScores(String playerName, @Nullable ScoreboardObjective objective) {
        if (objective == null) {
            Map<ScoreboardObjective, ScoreboardScore> map = this.playerScores.remove(playerName);
            if (map != null) {
                this.handlePlayerRemoved(playerName);
            }
        } else {
            Map<ScoreboardObjective, ScoreboardScore> map2 = this.playerScores.get(playerName);
            if (map2 != null) {
                ScoreboardScore score = map2.remove(objective);
                if (map2.size() < 1) {
                    Map<ScoreboardObjective, ScoreboardScore> map3 = this.playerScores.remove(playerName);
                    if (map3 != null) {
                        this.handlePlayerRemoved(playerName);
                    }
                } else if (score != null) {
                    this.onPlayerScoreRemoved(playerName, objective);
                }
            }
        }

    }

    public Map<ScoreboardObjective, ScoreboardScore> getPlayerObjectives(String string) {
        Map<ScoreboardObjective, ScoreboardScore> map = this.playerScores.get(string);
        if (map == null) {
            map = Maps.newHashMap();
        }

        return map;
    }

    public void unregisterObjective(ScoreboardObjective objective) {
        this.objectivesByName.remove(objective.getName());

        for(int i = 0; i < 19; ++i) {
            if (this.getObjectiveForSlot(i) == objective) {
                this.setDisplaySlot(i, (ScoreboardObjective)null);
            }
        }

        List<ScoreboardObjective> list = this.objectivesByCriteria.get(objective.getCriteria());
        if (list != null) {
            list.remove(objective);
        }

        for(Map<ScoreboardObjective, ScoreboardScore> map : this.playerScores.values()) {
            map.remove(objective);
        }

        this.handleObjectiveRemoved(objective);
    }

    public void setDisplaySlot(int slot, @Nullable ScoreboardObjective objective) {
        this.displayObjectives[slot] = objective;
    }

    @Nullable
    public ScoreboardObjective getObjectiveForSlot(int slot) {
        return this.displayObjectives[slot];
    }

    @Nullable
    public ScoreboardTeam getTeam(String name) {
        return this.teamsByName.get(name);
    }

    public ScoreboardTeam createTeam(String name) {
        if (name.length() > 16) {
            throw new IllegalArgumentException("The team name '" + name + "' is too long!");
        } else {
            ScoreboardTeam playerTeam = this.getTeam(name);
            if (playerTeam != null) {
                throw new IllegalArgumentException("A team with the name '" + name + "' already exists!");
            } else {
                playerTeam = new ScoreboardTeam(this, name);
                this.teamsByName.put(name, playerTeam);
                this.handleTeamAdded(playerTeam);
                return playerTeam;
            }
        }
    }

    public void removeTeam(ScoreboardTeam team) {
        this.teamsByName.remove(team.getName());

        for(String string : team.getPlayerNameSet()) {
            this.teamsByPlayer.remove(string);
        }

        this.handleTeamRemoved(team);
    }

    public boolean addPlayerToTeam(String playerName, ScoreboardTeam team) {
        if (playerName.length() > 40) {
            throw new IllegalArgumentException("The player name '" + playerName + "' is too long!");
        } else {
            if (this.getPlayerTeam(playerName) != null) {
                this.removePlayerFromTeam(playerName);
            }

            this.teamsByPlayer.put(playerName, team);
            return team.getPlayerNameSet().add(playerName);
        }
    }

    public boolean removePlayerFromTeam(String playerName) {
        ScoreboardTeam playerTeam = this.getPlayerTeam(playerName);
        if (playerTeam != null) {
            this.removePlayerFromTeam(playerName, playerTeam);
            return true;
        } else {
            return false;
        }
    }

    public void removePlayerFromTeam(String playerName, ScoreboardTeam team) {
        if (this.getPlayerTeam(playerName) != team) {
            throw new IllegalStateException("Player is either on another team or not on any team. Cannot remove from team '" + team.getName() + "'.");
        } else {
            this.teamsByPlayer.remove(playerName);
            team.getPlayerNameSet().remove(playerName);
        }
    }

    public Collection<String> getTeamNames() {
        return this.teamsByName.keySet();
    }

    public Collection<ScoreboardTeam> getTeams() {
        return this.teamsByName.values();
    }

    @Nullable
    public ScoreboardTeam getPlayerTeam(String playerName) {
        return this.teamsByPlayer.get(playerName);
    }

    public void handleObjectiveAdded(ScoreboardObjective objective) {
    }

    public void handleObjectiveChanged(ScoreboardObjective objective) {
    }

    public void handleObjectiveRemoved(ScoreboardObjective objective) {
    }

    public void handleScoreChanged(ScoreboardScore score) {
    }

    public void handlePlayerRemoved(String playerName) {
    }

    public void onPlayerScoreRemoved(String playerName, ScoreboardObjective objective) {
    }

    public void handleTeamAdded(ScoreboardTeam team) {
    }

    public void handleTeamChanged(ScoreboardTeam team) {
    }

    public void handleTeamRemoved(ScoreboardTeam team) {
    }

    public static String getSlotName(int slotId) {
        switch(slotId) {
        case 0:
            return "list";
        case 1:
            return "sidebar";
        case 2:
            return "belowName";
        default:
            if (slotId >= 3 && slotId <= 18) {
                EnumChatFormat chatFormatting = EnumChatFormat.getById(slotId - 3);
                if (chatFormatting != null && chatFormatting != EnumChatFormat.RESET) {
                    return "sidebar.team." + chatFormatting.getName();
                }
            }

            return null;
        }
    }

    public static int getSlotForName(String slotName) {
        if ("list".equalsIgnoreCase(slotName)) {
            return 0;
        } else if ("sidebar".equalsIgnoreCase(slotName)) {
            return 1;
        } else if ("belowName".equalsIgnoreCase(slotName)) {
            return 2;
        } else {
            if (slotName.startsWith("sidebar.team.")) {
                String string = slotName.substring("sidebar.team.".length());
                EnumChatFormat chatFormatting = EnumChatFormat.getByName(string);
                if (chatFormatting != null && chatFormatting.getId() >= 0) {
                    return chatFormatting.getId() + 3;
                }
            }

            return -1;
        }
    }

    public static String[] getDisplaySlotNames() {
        if (displaySlotNames == null) {
            displaySlotNames = new String[19];

            for(int i = 0; i < 19; ++i) {
                displaySlotNames[i] = getSlotName(i);
            }
        }

        return displaySlotNames;
    }

    public void entityRemoved(Entity entity) {
        if (entity != null && !(entity instanceof EntityHuman) && !entity.isAlive()) {
            String string = entity.getUniqueIDString();
            this.resetPlayerScores(string, (ScoreboardObjective)null);
            this.removePlayerFromTeam(string);
        }
    }

    protected NBTTagList savePlayerScores() {
        NBTTagList listTag = new NBTTagList();
        this.playerScores.values().stream().map(Map::values).forEach((collection) -> {
            collection.stream().filter((score) -> {
                return score.getObjective() != null;
            }).forEach((score) -> {
                NBTTagCompound compoundTag = new NBTTagCompound();
                compoundTag.setString("Name", score.getPlayerName());
                compoundTag.setString("Objective", score.getObjective().getName());
                compoundTag.setInt("Score", score.getScore());
                compoundTag.setBoolean("Locked", score.isLocked());
                listTag.add(compoundTag);
            });
        });
        return listTag;
    }

    protected void loadPlayerScores(NBTTagList list) {
        for(int i = 0; i < list.size(); ++i) {
            NBTTagCompound compoundTag = list.getCompound(i);
            ScoreboardObjective objective = this.getOrCreateObjective(compoundTag.getString("Objective"));
            String string = compoundTag.getString("Name");
            if (string.length() > 40) {
                string = string.substring(0, 40);
            }

            ScoreboardScore score = this.getPlayerScoreForObjective(string, objective);
            score.setScore(compoundTag.getInt("Score"));
            if (compoundTag.hasKey("Locked")) {
                score.setLocked(compoundTag.getBoolean("Locked"));
            }
        }

    }
}
