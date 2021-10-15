package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import java.util.List;
import net.minecraft.network.chat.ChatModifier;

@FunctionalInterface
public interface FormattedString {
    FormattedString EMPTY = (formattedCharSink) -> {
        return true;
    };

    boolean accept(FormattedStringEmpty visitor);

    static FormattedString codepoint(int codePoint, ChatModifier style) {
        return (visitor) -> {
            return visitor.accept(0, style, codePoint);
        };
    }

    static FormattedString forward(String string, ChatModifier style) {
        return string.isEmpty() ? EMPTY : (visitor) -> {
            return StringDecomposer.iterate(string, style, visitor);
        };
    }

    static FormattedString forward(String string, ChatModifier style, Int2IntFunction codePointMapper) {
        return string.isEmpty() ? EMPTY : (visitor) -> {
            return StringDecomposer.iterate(string, style, decorateOutput(visitor, codePointMapper));
        };
    }

    static FormattedString backward(String string, ChatModifier style) {
        return string.isEmpty() ? EMPTY : (visitor) -> {
            return StringDecomposer.iterateBackwards(string, style, visitor);
        };
    }

    static FormattedString backward(String string, ChatModifier style, Int2IntFunction codePointMapper) {
        return string.isEmpty() ? EMPTY : (visitor) -> {
            return StringDecomposer.iterateBackwards(string, style, decorateOutput(visitor, codePointMapper));
        };
    }

    static FormattedStringEmpty decorateOutput(FormattedStringEmpty visitor, Int2IntFunction codePointMapper) {
        return (charIndex, style, charPoint) -> {
            return visitor.accept(charIndex, style, codePointMapper.apply(Integer.valueOf(charPoint)));
        };
    }

    static FormattedString composite() {
        return EMPTY;
    }

    static FormattedString composite(FormattedString text) {
        return text;
    }

    static FormattedString composite(FormattedString first, FormattedString second) {
        return fromPair(first, second);
    }

    static FormattedString composite(FormattedString... texts) {
        return fromList(ImmutableList.copyOf(texts));
    }

    static FormattedString composite(List<FormattedString> texts) {
        int i = texts.size();
        switch(i) {
        case 0:
            return EMPTY;
        case 1:
            return texts.get(0);
        case 2:
            return fromPair(texts.get(0), texts.get(1));
        default:
            return fromList(ImmutableList.copyOf(texts));
        }
    }

    static FormattedString fromPair(FormattedString text1, FormattedString text2) {
        return (visitor) -> {
            return text1.accept(visitor) && text2.accept(visitor);
        };
    }

    static FormattedString fromList(List<FormattedString> texts) {
        return (visitor) -> {
            for(FormattedString formattedCharSequence : texts) {
                if (!formattedCharSequence.accept(visitor)) {
                    return false;
                }
            }

            return true;
        };
    }
}
