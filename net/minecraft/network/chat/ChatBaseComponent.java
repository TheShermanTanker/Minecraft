package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.locale.LocaleLanguage;
import net.minecraft.util.FormattedString;

public abstract class ChatBaseComponent implements IChatMutableComponent {
    protected final List<IChatBaseComponent> siblings = Lists.newArrayList();
    private FormattedString visualOrderText = FormattedString.EMPTY;
    @Nullable
    private LocaleLanguage decomposedWith;
    private ChatModifier style = ChatModifier.EMPTY;

    @Override
    public IChatMutableComponent addSibling(IChatBaseComponent text) {
        this.siblings.add(text);
        return this;
    }

    @Override
    public String getContents() {
        return "";
    }

    @Override
    public List<IChatBaseComponent> getSiblings() {
        return this.siblings;
    }

    @Override
    public IChatMutableComponent setChatModifier(ChatModifier style) {
        this.style = style;
        return this;
    }

    @Override
    public ChatModifier getChatModifier() {
        return this.style;
    }

    @Override
    public abstract ChatBaseComponent plainCopy();

    @Override
    public final IChatMutableComponent mutableCopy() {
        ChatBaseComponent baseComponent = this.plainCopy();
        baseComponent.siblings.addAll(this.siblings);
        baseComponent.setChatModifier(this.style);
        return baseComponent;
    }

    @Override
    public FormattedString getVisualOrderText() {
        LocaleLanguage language = LocaleLanguage.getInstance();
        if (this.decomposedWith != language) {
            this.visualOrderText = language.getVisualOrder(this);
            this.decomposedWith = language;
        }

        return this.visualOrderText;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof ChatBaseComponent)) {
            return false;
        } else {
            ChatBaseComponent baseComponent = (ChatBaseComponent)object;
            return this.siblings.equals(baseComponent.siblings) && Objects.equals(this.getChatModifier(), baseComponent.getChatModifier());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getChatModifier(), this.siblings);
    }

    @Override
    public String toString() {
        return "BaseComponent{style=" + this.style + ", siblings=" + this.siblings + "}";
    }
}
