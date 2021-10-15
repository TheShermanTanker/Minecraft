package net.minecraft.network.chat;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class ChatComponentKeybind extends ChatBaseComponent {
    private static Function<String, Supplier<IChatBaseComponent>> keyResolver = (key) -> {
        return () -> {
            return new ChatComponentText(key);
        };
    };
    private final String name;
    private Supplier<IChatBaseComponent> nameResolver;

    public ChatComponentKeybind(String key) {
        this.name = key;
    }

    public static void setKeyResolver(Function<String, Supplier<IChatBaseComponent>> translator) {
        keyResolver = translator;
    }

    private IChatBaseComponent getNestedComponent() {
        if (this.nameResolver == null) {
            this.nameResolver = keyResolver.apply(this.name);
        }

        return this.nameResolver.get();
    }

    @Override
    public <T> Optional<T> visitSelf(IChatFormatted.ContentConsumer<T> visitor) {
        return this.getNestedComponent().visit(visitor);
    }

    @Override
    public <T> Optional<T> visitSelf(IChatFormatted.StyledContentConsumer<T> visitor, ChatModifier style) {
        return this.getNestedComponent().visit(visitor, style);
    }

    @Override
    public ChatComponentKeybind plainCopy() {
        return new ChatComponentKeybind(this.name);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof ChatComponentKeybind)) {
            return false;
        } else {
            ChatComponentKeybind keybindComponent = (ChatComponentKeybind)object;
            return this.name.equals(keybindComponent.name) && super.equals(object);
        }
    }

    @Override
    public String toString() {
        return "KeybindComponent{keybind='" + this.name + "', siblings=" + this.siblings + ", style=" + this.getChatModifier() + "}";
    }

    public String getName() {
        return this.name;
    }
}
