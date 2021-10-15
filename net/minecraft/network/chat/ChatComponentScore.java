package net.minecraft.network.chat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.selector.ArgumentParserSelector;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardObjective;
import net.minecraft.world.scores.ScoreboardScore;

public class ChatComponentScore extends ChatBaseComponent implements ChatComponentContextual {
    private static final String SCORER_PLACEHOLDER = "*";
    private final String name;
    @Nullable
    private final EntitySelector selector;
    private final String objective;

    @Nullable
    private static EntitySelector parseSelector(String name) {
        try {
            return (new ArgumentParserSelector(new StringReader(name))).parse();
        } catch (CommandSyntaxException var2) {
            return null;
        }
    }

    public ChatComponentScore(String name, String objective) {
        this(name, parseSelector(name), objective);
    }

    private ChatComponentScore(String name, @Nullable EntitySelector selector, String objective) {
        this.name = name;
        this.selector = selector;
        this.objective = objective;
    }

    public String getName() {
        return this.name;
    }

    @Nullable
    public EntitySelector getSelector() {
        return this.selector;
    }

    public String getObjective() {
        return this.objective;
    }

    private String findTargetName(CommandListenerWrapper source) throws CommandSyntaxException {
        if (this.selector != null) {
            List<? extends Entity> list = this.selector.getEntities(source);
            if (!list.isEmpty()) {
                if (list.size() != 1) {
                    throw ArgumentEntity.ERROR_NOT_SINGLE_ENTITY.create();
                }

                return list.get(0).getName();
            }
        }

        return this.name;
    }

    private String getScore(String playerName, CommandListenerWrapper source) {
        MinecraftServer minecraftServer = source.getServer();
        if (minecraftServer != null) {
            Scoreboard scoreboard = minecraftServer.getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjective(this.objective);
            if (scoreboard.hasPlayerScore(playerName, objective)) {
                ScoreboardScore score = scoreboard.getPlayerScoreForObjective(playerName, objective);
                return Integer.toString(score.getScore());
            }
        }

        return "";
    }

    @Override
    public ChatComponentScore plainCopy() {
        return new ChatComponentScore(this.name, this.selector, this.objective);
    }

    @Override
    public IChatMutableComponent resolve(@Nullable CommandListenerWrapper source, @Nullable Entity sender, int depth) throws CommandSyntaxException {
        if (source == null) {
            return new ChatComponentText("");
        } else {
            String string = this.findTargetName(source);
            String string2 = sender != null && string.equals("*") ? sender.getName() : string;
            return new ChatComponentText(this.getScore(string2, source));
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof ChatComponentScore)) {
            return false;
        } else {
            ChatComponentScore scoreComponent = (ChatComponentScore)object;
            return this.name.equals(scoreComponent.name) && this.objective.equals(scoreComponent.objective) && super.equals(object);
        }
    }

    @Override
    public String toString() {
        return "ScoreComponent{name='" + this.name + "'objective='" + this.objective + "', siblings=" + this.siblings + ", style=" + this.getChatModifier() + "}";
    }
}
