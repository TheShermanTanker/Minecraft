package net.minecraft.world.scores;

import net.minecraft.EnumChatFormat;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.level.saveddata.PersistentBase;
import net.minecraft.world.scores.criteria.IScoreboardCriteria;

public class PersistentScoreboard extends PersistentBase {
    public static final String FILE_ID = "scoreboard";
    private final Scoreboard scoreboard;

    public PersistentScoreboard(Scoreboard scoreboard) {
        this.scoreboard = scoreboard;
    }

    public PersistentScoreboard load(NBTTagCompound nbt) {
        this.loadObjectives(nbt.getList("Objectives", 10));
        this.scoreboard.loadPlayerScores(nbt.getList("PlayerScores", 10));
        if (nbt.hasKeyOfType("DisplaySlots", 10)) {
            this.loadDisplaySlots(nbt.getCompound("DisplaySlots"));
        }

        if (nbt.hasKeyOfType("Teams", 9)) {
            this.loadTeams(nbt.getList("Teams", 10));
        }

        return this;
    }

    private void loadTeams(NBTTagList nbt) {
        for(int i = 0; i < nbt.size(); ++i) {
            NBTTagCompound compoundTag = nbt.getCompound(i);
            String string = compoundTag.getString("Name");
            if (string.length() > 16) {
                string = string.substring(0, 16);
            }

            ScoreboardTeam playerTeam = this.scoreboard.createTeam(string);
            IChatBaseComponent component = IChatBaseComponent.ChatSerializer.fromJson(compoundTag.getString("DisplayName"));
            if (component != null) {
                playerTeam.setDisplayName(component);
            }

            if (compoundTag.hasKeyOfType("TeamColor", 8)) {
                playerTeam.setColor(EnumChatFormat.getByName(compoundTag.getString("TeamColor")));
            }

            if (compoundTag.hasKeyOfType("AllowFriendlyFire", 99)) {
                playerTeam.setAllowFriendlyFire(compoundTag.getBoolean("AllowFriendlyFire"));
            }

            if (compoundTag.hasKeyOfType("SeeFriendlyInvisibles", 99)) {
                playerTeam.setCanSeeFriendlyInvisibles(compoundTag.getBoolean("SeeFriendlyInvisibles"));
            }

            if (compoundTag.hasKeyOfType("MemberNamePrefix", 8)) {
                IChatBaseComponent component2 = IChatBaseComponent.ChatSerializer.fromJson(compoundTag.getString("MemberNamePrefix"));
                if (component2 != null) {
                    playerTeam.setPrefix(component2);
                }
            }

            if (compoundTag.hasKeyOfType("MemberNameSuffix", 8)) {
                IChatBaseComponent component3 = IChatBaseComponent.ChatSerializer.fromJson(compoundTag.getString("MemberNameSuffix"));
                if (component3 != null) {
                    playerTeam.setSuffix(component3);
                }
            }

            if (compoundTag.hasKeyOfType("NameTagVisibility", 8)) {
                ScoreboardTeamBase.EnumNameTagVisibility visibility = ScoreboardTeamBase.EnumNameTagVisibility.byName(compoundTag.getString("NameTagVisibility"));
                if (visibility != null) {
                    playerTeam.setNameTagVisibility(visibility);
                }
            }

            if (compoundTag.hasKeyOfType("DeathMessageVisibility", 8)) {
                ScoreboardTeamBase.EnumNameTagVisibility visibility2 = ScoreboardTeamBase.EnumNameTagVisibility.byName(compoundTag.getString("DeathMessageVisibility"));
                if (visibility2 != null) {
                    playerTeam.setDeathMessageVisibility(visibility2);
                }
            }

            if (compoundTag.hasKeyOfType("CollisionRule", 8)) {
                ScoreboardTeamBase.EnumTeamPush collisionRule = ScoreboardTeamBase.EnumTeamPush.byName(compoundTag.getString("CollisionRule"));
                if (collisionRule != null) {
                    playerTeam.setCollisionRule(collisionRule);
                }
            }

            this.loadTeamPlayers(playerTeam, compoundTag.getList("Players", 8));
        }

    }

    private void loadTeamPlayers(ScoreboardTeam team, NBTTagList nbt) {
        for(int i = 0; i < nbt.size(); ++i) {
            this.scoreboard.addPlayerToTeam(nbt.getString(i), team);
        }

    }

    private void loadDisplaySlots(NBTTagCompound nbt) {
        for(int i = 0; i < 19; ++i) {
            if (nbt.hasKeyOfType("slot_" + i, 8)) {
                String string = nbt.getString("slot_" + i);
                ScoreboardObjective objective = this.scoreboard.getObjective(string);
                this.scoreboard.setDisplaySlot(i, objective);
            }
        }

    }

    private void loadObjectives(NBTTagList nbt) {
        for(int i = 0; i < nbt.size(); ++i) {
            NBTTagCompound compoundTag = nbt.getCompound(i);
            IScoreboardCriteria.byName(compoundTag.getString("CriteriaName")).ifPresent((objectiveCriteria) -> {
                String string = compoundTag.getString("Name");
                if (string.length() > 16) {
                    string = string.substring(0, 16);
                }

                IChatBaseComponent component = IChatBaseComponent.ChatSerializer.fromJson(compoundTag.getString("DisplayName"));
                IScoreboardCriteria.EnumScoreboardHealthDisplay renderType = IScoreboardCriteria.EnumScoreboardHealthDisplay.byId(compoundTag.getString("RenderType"));
                this.scoreboard.registerObjective(string, objectiveCriteria, component, renderType);
            });
        }

    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        nbt.set("Objectives", this.saveObjectives());
        nbt.set("PlayerScores", this.scoreboard.savePlayerScores());
        nbt.set("Teams", this.saveTeams());
        this.saveDisplaySlots(nbt);
        return nbt;
    }

    private NBTTagList saveTeams() {
        NBTTagList listTag = new NBTTagList();

        for(ScoreboardTeam playerTeam : this.scoreboard.getTeams()) {
            NBTTagCompound compoundTag = new NBTTagCompound();
            compoundTag.setString("Name", playerTeam.getName());
            compoundTag.setString("DisplayName", IChatBaseComponent.ChatSerializer.toJson(playerTeam.getDisplayName()));
            if (playerTeam.getColor().getId() >= 0) {
                compoundTag.setString("TeamColor", playerTeam.getColor().getName());
            }

            compoundTag.setBoolean("AllowFriendlyFire", playerTeam.allowFriendlyFire());
            compoundTag.setBoolean("SeeFriendlyInvisibles", playerTeam.canSeeFriendlyInvisibles());
            compoundTag.setString("MemberNamePrefix", IChatBaseComponent.ChatSerializer.toJson(playerTeam.getPrefix()));
            compoundTag.setString("MemberNameSuffix", IChatBaseComponent.ChatSerializer.toJson(playerTeam.getSuffix()));
            compoundTag.setString("NameTagVisibility", playerTeam.getNameTagVisibility().name);
            compoundTag.setString("DeathMessageVisibility", playerTeam.getDeathMessageVisibility().name);
            compoundTag.setString("CollisionRule", playerTeam.getCollisionRule().name);
            NBTTagList listTag2 = new NBTTagList();

            for(String string : playerTeam.getPlayerNameSet()) {
                listTag2.add(NBTTagString.valueOf(string));
            }

            compoundTag.set("Players", listTag2);
            listTag.add(compoundTag);
        }

        return listTag;
    }

    private void saveDisplaySlots(NBTTagCompound nbt) {
        NBTTagCompound compoundTag = new NBTTagCompound();
        boolean bl = false;

        for(int i = 0; i < 19; ++i) {
            ScoreboardObjective objective = this.scoreboard.getObjectiveForSlot(i);
            if (objective != null) {
                compoundTag.setString("slot_" + i, objective.getName());
                bl = true;
            }
        }

        if (bl) {
            nbt.set("DisplaySlots", compoundTag);
        }

    }

    private NBTTagList saveObjectives() {
        NBTTagList listTag = new NBTTagList();

        for(ScoreboardObjective objective : this.scoreboard.getObjectives()) {
            if (objective.getCriteria() != null) {
                NBTTagCompound compoundTag = new NBTTagCompound();
                compoundTag.setString("Name", objective.getName());
                compoundTag.setString("CriteriaName", objective.getCriteria().getName());
                compoundTag.setString("DisplayName", IChatBaseComponent.ChatSerializer.toJson(objective.getDisplayName()));
                compoundTag.setString("RenderType", objective.getRenderType().getId());
                listTag.add(compoundTag);
            }
        }

        return listTag;
    }
}
