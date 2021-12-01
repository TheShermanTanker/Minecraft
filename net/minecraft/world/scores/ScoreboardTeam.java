package net.minecraft.world.scores;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.ChatModifier;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;

public class ScoreboardTeam extends ScoreboardTeamBase {
    private static final int BIT_FRIENDLY_FIRE = 0;
    private static final int BIT_SEE_INVISIBLES = 1;
    private final Scoreboard scoreboard;
    private final String name;
    private final Set<String> players = Sets.newHashSet();
    private IChatBaseComponent displayName;
    private IChatBaseComponent playerPrefix = ChatComponentText.EMPTY;
    private IChatBaseComponent playerSuffix = ChatComponentText.EMPTY;
    private boolean allowFriendlyFire = true;
    private boolean seeFriendlyInvisibles = true;
    private ScoreboardTeamBase.EnumNameTagVisibility nameTagVisibility = ScoreboardTeamBase.EnumNameTagVisibility.ALWAYS;
    private ScoreboardTeamBase.EnumNameTagVisibility deathMessageVisibility = ScoreboardTeamBase.EnumNameTagVisibility.ALWAYS;
    private EnumChatFormat color = EnumChatFormat.RESET;
    private ScoreboardTeamBase.EnumTeamPush collisionRule = ScoreboardTeamBase.EnumTeamPush.ALWAYS;
    private final ChatModifier displayNameStyle;

    public ScoreboardTeam(Scoreboard scoreboard, String name) {
        this.scoreboard = scoreboard;
        this.name = name;
        this.displayName = new ChatComponentText(name);
        this.displayNameStyle = ChatModifier.EMPTY.setInsertion(name).setChatHoverable(new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_TEXT, new ChatComponentText(name)));
    }

    public Scoreboard getScoreboard() {
        return this.scoreboard;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public IChatBaseComponent getDisplayName() {
        return this.displayName;
    }

    public IChatMutableComponent getFormattedDisplayName() {
        IChatMutableComponent mutableComponent = ChatComponentUtils.wrapInSquareBrackets(this.displayName.mutableCopy().withStyle(this.displayNameStyle));
        EnumChatFormat chatFormatting = this.getColor();
        if (chatFormatting != EnumChatFormat.RESET) {
            mutableComponent.withStyle(chatFormatting);
        }

        return mutableComponent;
    }

    public void setDisplayName(IChatBaseComponent displayName) {
        if (displayName == null) {
            throw new IllegalArgumentException("Name cannot be null");
        } else {
            this.displayName = displayName;
            this.scoreboard.handleTeamChanged(this);
        }
    }

    public void setPrefix(@Nullable IChatBaseComponent prefix) {
        this.playerPrefix = prefix == null ? ChatComponentText.EMPTY : prefix;
        this.scoreboard.handleTeamChanged(this);
    }

    public IChatBaseComponent getPrefix() {
        return this.playerPrefix;
    }

    public void setSuffix(@Nullable IChatBaseComponent suffix) {
        this.playerSuffix = suffix == null ? ChatComponentText.EMPTY : suffix;
        this.scoreboard.handleTeamChanged(this);
    }

    public IChatBaseComponent getSuffix() {
        return this.playerSuffix;
    }

    @Override
    public Collection<String> getPlayerNameSet() {
        return this.players;
    }

    @Override
    public IChatMutableComponent getFormattedName(IChatBaseComponent name) {
        IChatMutableComponent mutableComponent = (new ChatComponentText("")).addSibling(this.playerPrefix).addSibling(name).addSibling(this.playerSuffix);
        EnumChatFormat chatFormatting = this.getColor();
        if (chatFormatting != EnumChatFormat.RESET) {
            mutableComponent.withStyle(chatFormatting);
        }

        return mutableComponent;
    }

    public static IChatMutableComponent formatNameForTeam(@Nullable ScoreboardTeamBase team, IChatBaseComponent name) {
        return team == null ? name.mutableCopy() : team.getFormattedName(name);
    }

    @Override
    public boolean allowFriendlyFire() {
        return this.allowFriendlyFire;
    }

    public void setAllowFriendlyFire(boolean friendlyFire) {
        this.allowFriendlyFire = friendlyFire;
        this.scoreboard.handleTeamChanged(this);
    }

    @Override
    public boolean canSeeFriendlyInvisibles() {
        return this.seeFriendlyInvisibles;
    }

    public void setCanSeeFriendlyInvisibles(boolean showFriendlyInvisible) {
        this.seeFriendlyInvisibles = showFriendlyInvisible;
        this.scoreboard.handleTeamChanged(this);
    }

    @Override
    public ScoreboardTeamBase.EnumNameTagVisibility getNameTagVisibility() {
        return this.nameTagVisibility;
    }

    @Override
    public ScoreboardTeamBase.EnumNameTagVisibility getDeathMessageVisibility() {
        return this.deathMessageVisibility;
    }

    public void setNameTagVisibility(ScoreboardTeamBase.EnumNameTagVisibility nameTagVisibilityRule) {
        this.nameTagVisibility = nameTagVisibilityRule;
        this.scoreboard.handleTeamChanged(this);
    }

    public void setDeathMessageVisibility(ScoreboardTeamBase.EnumNameTagVisibility deathMessageVisibilityRule) {
        this.deathMessageVisibility = deathMessageVisibilityRule;
        this.scoreboard.handleTeamChanged(this);
    }

    @Override
    public ScoreboardTeamBase.EnumTeamPush getCollisionRule() {
        return this.collisionRule;
    }

    public void setCollisionRule(ScoreboardTeamBase.EnumTeamPush collisionRule) {
        this.collisionRule = collisionRule;
        this.scoreboard.handleTeamChanged(this);
    }

    public int packOptionData() {
        int i = 0;
        if (this.allowFriendlyFire()) {
            i |= 1;
        }

        if (this.canSeeFriendlyInvisibles()) {
            i |= 2;
        }

        return i;
    }

    public void unpackOptions(int flags) {
        this.setAllowFriendlyFire((flags & 1) > 0);
        this.setCanSeeFriendlyInvisibles((flags & 2) > 0);
    }

    public void setColor(EnumChatFormat color) {
        this.color = color;
        this.scoreboard.handleTeamChanged(this);
    }

    @Override
    public EnumChatFormat getColor() {
        return this.color;
    }
}
