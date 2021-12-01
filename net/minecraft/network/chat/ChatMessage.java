package net.minecraft.network.chat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.locale.LocaleLanguage;
import net.minecraft.world.entity.Entity;

public class ChatMessage extends ChatBaseComponent implements ChatComponentContextual {
    private static final Object[] NO_ARGS = new Object[0];
    private static final IChatFormatted TEXT_PERCENT = IChatFormatted.of("%");
    private static final IChatFormatted TEXT_NULL = IChatFormatted.of("null");
    private final String key;
    private final Object[] args;
    @Nullable
    private LocaleLanguage decomposedWith;
    private List<IChatFormatted> decomposedParts = ImmutableList.of();
    private static final Pattern FORMAT_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

    public ChatMessage(String key) {
        this.key = key;
        this.args = NO_ARGS;
    }

    public ChatMessage(String key, Object... args) {
        this.key = key;
        this.args = args;
    }

    private void decompose() {
        LocaleLanguage language = LocaleLanguage.getInstance();
        if (language != this.decomposedWith) {
            this.decomposedWith = language;
            String string = language.getOrDefault(this.key);

            try {
                Builder<IChatFormatted> builder = ImmutableList.builder();
                this.decomposeTemplate(string, builder::add);
                this.decomposedParts = builder.build();
            } catch (ChatMessageException var4) {
                this.decomposedParts = ImmutableList.of(IChatFormatted.of(string));
            }

        }
    }

    private void decomposeTemplate(String translation, Consumer<IChatFormatted> partsConsumer) {
        Matcher matcher = FORMAT_PATTERN.matcher(translation);

        try {
            int i = 0;

            int j;
            int l;
            for(j = 0; matcher.find(j); j = l) {
                int k = matcher.start();
                l = matcher.end();
                if (k > j) {
                    String string = translation.substring(j, k);
                    if (string.indexOf(37) != -1) {
                        throw new IllegalArgumentException();
                    }

                    partsConsumer.accept(IChatFormatted.of(string));
                }

                String string2 = matcher.group(2);
                String string3 = translation.substring(k, l);
                if ("%".equals(string2) && "%%".equals(string3)) {
                    partsConsumer.accept(TEXT_PERCENT);
                } else {
                    if (!"s".equals(string2)) {
                        throw new ChatMessageException(this, "Unsupported format: '" + string3 + "'");
                    }

                    String string4 = matcher.group(1);
                    int m = string4 != null ? Integer.parseInt(string4) - 1 : i++;
                    if (m < this.args.length) {
                        partsConsumer.accept(this.getArgument(m));
                    }
                }
            }

            if (j < translation.length()) {
                String string5 = translation.substring(j);
                if (string5.indexOf(37) != -1) {
                    throw new IllegalArgumentException();
                }

                partsConsumer.accept(IChatFormatted.of(string5));
            }

        } catch (IllegalArgumentException var12) {
            throw new ChatMessageException(this, var12);
        }
    }

    private IChatFormatted getArgument(int index) {
        if (index >= this.args.length) {
            throw new ChatMessageException(this, index);
        } else {
            Object object = this.args[index];
            if (object instanceof IChatBaseComponent) {
                return (IChatBaseComponent)object;
            } else {
                return object == null ? TEXT_NULL : IChatFormatted.of(object.toString());
            }
        }
    }

    @Override
    public ChatMessage plainCopy() {
        return new ChatMessage(this.key, this.args);
    }

    @Override
    public <T> Optional<T> visitSelf(IChatFormatted.StyledContentConsumer<T> visitor, ChatModifier style) {
        this.decompose();

        for(IChatFormatted formattedText : this.decomposedParts) {
            Optional<T> optional = formattedText.visit(visitor, style);
            if (optional.isPresent()) {
                return optional;
            }
        }

        return Optional.empty();
    }

    @Override
    public <T> Optional<T> visitSelf(IChatFormatted.ContentConsumer<T> visitor) {
        this.decompose();

        for(IChatFormatted formattedText : this.decomposedParts) {
            Optional<T> optional = formattedText.visit(visitor);
            if (optional.isPresent()) {
                return optional;
            }
        }

        return Optional.empty();
    }

    @Override
    public IChatMutableComponent resolve(@Nullable CommandListenerWrapper source, @Nullable Entity sender, int depth) throws CommandSyntaxException {
        Object[] objects = new Object[this.args.length];

        for(int i = 0; i < objects.length; ++i) {
            Object object = this.args[i];
            if (object instanceof IChatBaseComponent) {
                objects[i] = ChatComponentUtils.filterForDisplay(source, (IChatBaseComponent)object, sender, depth);
            } else {
                objects[i] = object;
            }
        }

        return new ChatMessage(this.key, objects);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof ChatMessage)) {
            return false;
        } else {
            ChatMessage translatableComponent = (ChatMessage)object;
            return Arrays.equals(this.args, translatableComponent.args) && this.key.equals(translatableComponent.key) && super.equals(object);
        }
    }

    @Override
    public int hashCode() {
        int i = super.hashCode();
        i = 31 * i + this.key.hashCode();
        return 31 * i + Arrays.hashCode(this.args);
    }

    @Override
    public String toString() {
        return "TranslatableComponent{key='" + this.key + "', args=" + Arrays.toString(this.args) + ", siblings=" + this.siblings + ", style=" + this.getChatModifier() + "}";
    }

    public String getKey() {
        return this.key;
    }

    public Object[] getArgs() {
        return this.args;
    }
}
