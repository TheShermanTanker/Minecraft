package net.minecraft.world.scores;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;

public abstract class ScoreboardTeamBase {
    public boolean isAlly(@Nullable ScoreboardTeamBase team) {
        if (team == null) {
            return false;
        } else {
            return this == team;
        }
    }

    public abstract String getName();

    public abstract IChatMutableComponent getFormattedName(IChatBaseComponent name);

    public abstract boolean canSeeFriendlyInvisibles();

    public abstract boolean allowFriendlyFire();

    public abstract ScoreboardTeamBase.EnumNameTagVisibility getNameTagVisibility();

    public abstract EnumChatFormat getColor();

    public abstract Collection<String> getPlayerNameSet();

    public abstract ScoreboardTeamBase.EnumNameTagVisibility getDeathMessageVisibility();

    public abstract ScoreboardTeamBase.EnumTeamPush getCollisionRule();

    public static enum EnumNameTagVisibility {
        ALWAYS("always", 0),
        NEVER("never", 1),
        HIDE_FOR_OTHER_TEAMS("hideForOtherTeams", 2),
        HIDE_FOR_OWN_TEAM("hideForOwnTeam", 3);

        private static final Map<String, ScoreboardTeamBase.EnumNameTagVisibility> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap((visibilityRule) -> {
            return visibilityRule.name;
        }, (visibility) -> {
            return visibility;
        }));
        public final String name;
        public final int id;

        public static String[] getAllNames() {
            return BY_NAME.keySet().toArray(new String[0]);
        }

        @Nullable
        public static ScoreboardTeamBase.EnumNameTagVisibility byName(String name) {
            return BY_NAME.get(name);
        }

        private EnumNameTagVisibility(String name, int value) {
            this.name = name;
            this.id = value;
        }

        public IChatBaseComponent getDisplayName() {
            return new ChatMessage("team.visibility." + this.name);
        }
    }

    public static enum EnumTeamPush {
        ALWAYS("always", 0),
        NEVER("never", 1),
        PUSH_OTHER_TEAMS("pushOtherTeams", 2),
        PUSH_OWN_TEAM("pushOwnTeam", 3);

        private static final Map<String, ScoreboardTeamBase.EnumTeamPush> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap((collisionRule) -> {
            return collisionRule.name;
        }, (collisionRule) -> {
            return collisionRule;
        }));
        public final String name;
        public final int id;

        @Nullable
        public static ScoreboardTeamBase.EnumTeamPush byName(String name) {
            return BY_NAME.get(name);
        }

        private EnumTeamPush(String name, int value) {
            this.name = name;
            this.id = value;
        }

        public IChatBaseComponent getDisplayName() {
            return new ChatMessage("team.collision." + this.name);
        }
    }
}
