package net.minecraft.world.scores;

import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.scores.criteria.IScoreboardCriteria;

public class ScoreboardObjective {
    private final Scoreboard scoreboard;
    private final String name;
    private final IScoreboardCriteria criteria;
    public IChatBaseComponent displayName;
    private IChatBaseComponent formattedDisplayName;
    private IScoreboardCriteria.EnumScoreboardHealthDisplay renderType;

    public ScoreboardObjective(Scoreboard scoreboard, String name, IScoreboardCriteria criterion, IChatBaseComponent displayName, IScoreboardCriteria.EnumScoreboardHealthDisplay renderType) {
        this.scoreboard = scoreboard;
        this.name = name;
        this.criteria = criterion;
        this.displayName = displayName;
        this.formattedDisplayName = this.createFormattedDisplayName();
        this.renderType = renderType;
    }

    public Scoreboard getScoreboard() {
        return this.scoreboard;
    }

    public String getName() {
        return this.name;
    }

    public IScoreboardCriteria getCriteria() {
        return this.criteria;
    }

    public IChatBaseComponent getDisplayName() {
        return this.displayName;
    }

    private IChatBaseComponent createFormattedDisplayName() {
        return ChatComponentUtils.wrapInSquareBrackets(this.displayName.mutableCopy().format((style) -> {
            return style.setChatHoverable(new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_TEXT, new ChatComponentText(this.name)));
        }));
    }

    public IChatBaseComponent getFormattedDisplayName() {
        return this.formattedDisplayName;
    }

    public void setDisplayName(IChatBaseComponent name) {
        this.displayName = name;
        this.formattedDisplayName = this.createFormattedDisplayName();
        this.scoreboard.handleObjectiveChanged(this);
    }

    public IScoreboardCriteria.EnumScoreboardHealthDisplay getRenderType() {
        return this.renderType;
    }

    public void setRenderType(IScoreboardCriteria.EnumScoreboardHealthDisplay renderType) {
        this.renderType = renderType;
        this.scoreboard.handleObjectiveChanged(this);
    }
}
