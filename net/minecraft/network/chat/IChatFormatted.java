package net.minecraft.network.chat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.Unit;

public interface IChatFormatted {
    Optional<Unit> STOP_ITERATION = Optional.of(Unit.INSTANCE);
    IChatFormatted EMPTY = new IChatFormatted() {
        @Override
        public <T> Optional<T> visit(IChatFormatted.ContentConsumer<T> visitor) {
            return Optional.empty();
        }

        @Override
        public <T> Optional<T> visit(IChatFormatted.StyledContentConsumer<T> styledVisitor, ChatModifier style) {
            return Optional.empty();
        }
    };

    <T> Optional<T> visit(IChatFormatted.ContentConsumer<T> visitor);

    <T> Optional<T> visit(IChatFormatted.StyledContentConsumer<T> styledVisitor, ChatModifier style);

    static IChatFormatted of(String string) {
        return new IChatFormatted() {
            @Override
            public <T> Optional<T> visit(IChatFormatted.ContentConsumer<T> visitor) {
                return visitor.accept(string);
            }

            @Override
            public <T> Optional<T> visit(IChatFormatted.StyledContentConsumer<T> styledVisitor, ChatModifier style) {
                return styledVisitor.accept(style, string);
            }
        };
    }

    static IChatFormatted of(String string, ChatModifier style) {
        return new IChatFormatted() {
            @Override
            public <T> Optional<T> visit(IChatFormatted.ContentConsumer<T> visitor) {
                return visitor.accept(string);
            }

            @Override
            public <T> Optional<T> visit(IChatFormatted.StyledContentConsumer<T> styledVisitor, ChatModifier style) {
                return styledVisitor.accept(style.setChatModifier(style), string);
            }
        };
    }

    static IChatFormatted composite(IChatFormatted... visitables) {
        return composite(ImmutableList.copyOf(visitables));
    }

    static IChatFormatted composite(List<? extends IChatFormatted> visitables) {
        return new IChatFormatted() {
            @Override
            public <T> Optional<T> visit(IChatFormatted.ContentConsumer<T> visitor) {
                for(IChatFormatted formattedText : visitables) {
                    Optional<T> optional = formattedText.visit(visitor);
                    if (optional.isPresent()) {
                        return optional;
                    }
                }

                return Optional.empty();
            }

            @Override
            public <T> Optional<T> visit(IChatFormatted.StyledContentConsumer<T> styledVisitor, ChatModifier style) {
                for(IChatFormatted formattedText : visitables) {
                    Optional<T> optional = formattedText.visit(styledVisitor, style);
                    if (optional.isPresent()) {
                        return optional;
                    }
                }

                return Optional.empty();
            }
        };
    }

    default String getString() {
        StringBuilder stringBuilder = new StringBuilder();
        this.visit((string) -> {
            stringBuilder.append(string);
            return Optional.empty();
        });
        return stringBuilder.toString();
    }

    public interface ContentConsumer<T> {
        Optional<T> accept(String asString);
    }

    public interface StyledContentConsumer<T> {
        Optional<T> accept(ChatModifier style, String asString);
    }
}
