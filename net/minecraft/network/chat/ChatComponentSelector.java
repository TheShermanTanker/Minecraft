package net.minecraft.network.chat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.selector.ArgumentParserSelector;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChatComponentSelector extends ChatBaseComponent implements ChatComponentContextual {
    private static final Logger LOGGER = LogManager.getLogger();
    private final String pattern;
    @Nullable
    private final EntitySelector selector;
    protected final Optional<IChatBaseComponent> separator;

    public ChatComponentSelector(String pattern, Optional<IChatBaseComponent> separator) {
        this.pattern = pattern;
        this.separator = separator;
        EntitySelector entitySelector = null;

        try {
            ArgumentParserSelector entitySelectorParser = new ArgumentParserSelector(new StringReader(pattern));
            entitySelector = entitySelectorParser.parse();
        } catch (CommandSyntaxException var5) {
            LOGGER.warn("Invalid selector component: {}: {}", pattern, var5.getMessage());
        }

        this.selector = entitySelector;
    }

    public String getPattern() {
        return this.pattern;
    }

    @Nullable
    public EntitySelector getSelector() {
        return this.selector;
    }

    public Optional<IChatBaseComponent> getSeparator() {
        return this.separator;
    }

    @Override
    public IChatMutableComponent resolve(@Nullable CommandListenerWrapper source, @Nullable Entity sender, int depth) throws CommandSyntaxException {
        if (source != null && this.selector != null) {
            Optional<? extends IChatBaseComponent> optional = ChatComponentUtils.updateForEntity(source, this.separator, sender, depth);
            return ChatComponentUtils.formatList(this.selector.getEntities(source), optional, Entity::getScoreboardDisplayName);
        } else {
            return new ChatComponentText("");
        }
    }

    @Override
    public String getContents() {
        return this.pattern;
    }

    @Override
    public ChatComponentSelector plainCopy() {
        return new ChatComponentSelector(this.pattern, this.separator);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof ChatComponentSelector)) {
            return false;
        } else {
            ChatComponentSelector selectorComponent = (ChatComponentSelector)object;
            return this.pattern.equals(selectorComponent.pattern) && super.equals(object);
        }
    }

    @Override
    public String toString() {
        return "SelectorComponent{pattern='" + this.pattern + "', siblings=" + this.siblings + ", style=" + this.getChatModifier() + "}";
    }
}
