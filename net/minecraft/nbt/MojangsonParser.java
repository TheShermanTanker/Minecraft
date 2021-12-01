package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.network.chat.ChatMessage;

public class MojangsonParser {
    public static final SimpleCommandExceptionType ERROR_TRAILING_DATA = new SimpleCommandExceptionType(new ChatMessage("argument.nbt.trailing"));
    public static final SimpleCommandExceptionType ERROR_EXPECTED_KEY = new SimpleCommandExceptionType(new ChatMessage("argument.nbt.expected.key"));
    public static final SimpleCommandExceptionType ERROR_EXPECTED_VALUE = new SimpleCommandExceptionType(new ChatMessage("argument.nbt.expected.value"));
    public static final Dynamic2CommandExceptionType ERROR_INSERT_MIXED_LIST = new Dynamic2CommandExceptionType((receivedType, expectedType) -> {
        return new ChatMessage("argument.nbt.list.mixed", receivedType, expectedType);
    });
    public static final Dynamic2CommandExceptionType ERROR_INSERT_MIXED_ARRAY = new Dynamic2CommandExceptionType((receivedType, expectedType) -> {
        return new ChatMessage("argument.nbt.array.mixed", receivedType, expectedType);
    });
    public static final DynamicCommandExceptionType ERROR_INVALID_ARRAY = new DynamicCommandExceptionType((type) -> {
        return new ChatMessage("argument.nbt.array.invalid", type);
    });
    public static final char ELEMENT_SEPARATOR = ',';
    public static final char NAME_VALUE_SEPARATOR = ':';
    private static final char LIST_OPEN = '[';
    private static final char LIST_CLOSE = ']';
    private static final char STRUCT_CLOSE = '}';
    private static final char STRUCT_OPEN = '{';
    private static final Pattern DOUBLE_PATTERN_NOSUFFIX = Pattern.compile("[-+]?(?:[0-9]+[.]|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?", 2);
    private static final Pattern DOUBLE_PATTERN = Pattern.compile("[-+]?(?:[0-9]+[.]?|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?d", 2);
    private static final Pattern FLOAT_PATTERN = Pattern.compile("[-+]?(?:[0-9]+[.]?|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?f", 2);
    private static final Pattern BYTE_PATTERN = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)b", 2);
    private static final Pattern LONG_PATTERN = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)l", 2);
    private static final Pattern SHORT_PATTERN = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)s", 2);
    private static final Pattern INT_PATTERN = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)");
    private final StringReader reader;

    public static NBTTagCompound parse(String string) throws CommandSyntaxException {
        return (new MojangsonParser(new StringReader(string))).readSingleStruct();
    }

    @VisibleForTesting
    NBTTagCompound readSingleStruct() throws CommandSyntaxException {
        NBTTagCompound compoundTag = this.readStruct();
        this.reader.skipWhitespace();
        if (this.reader.canRead()) {
            throw ERROR_TRAILING_DATA.createWithContext(this.reader);
        } else {
            return compoundTag;
        }
    }

    public MojangsonParser(StringReader reader) {
        this.reader = reader;
    }

    protected String readKey() throws CommandSyntaxException {
        this.reader.skipWhitespace();
        if (!this.reader.canRead()) {
            throw ERROR_EXPECTED_KEY.createWithContext(this.reader);
        } else {
            return this.reader.readString();
        }
    }

    protected NBTBase readTypedValue() throws CommandSyntaxException {
        this.reader.skipWhitespace();
        int i = this.reader.getCursor();
        if (StringReader.isQuotedStringStart(this.reader.peek())) {
            return NBTTagString.valueOf(this.reader.readQuotedString());
        } else {
            String string = this.reader.readUnquotedString();
            if (string.isEmpty()) {
                this.reader.setCursor(i);
                throw ERROR_EXPECTED_VALUE.createWithContext(this.reader);
            } else {
                return this.parseLiteral(string);
            }
        }
    }

    public NBTBase parseLiteral(String input) {
        try {
            if (FLOAT_PATTERN.matcher(input).matches()) {
                return NBTTagFloat.valueOf(Float.parseFloat(input.substring(0, input.length() - 1)));
            }

            if (BYTE_PATTERN.matcher(input).matches()) {
                return NBTTagByte.valueOf(Byte.parseByte(input.substring(0, input.length() - 1)));
            }

            if (LONG_PATTERN.matcher(input).matches()) {
                return NBTTagLong.valueOf(Long.parseLong(input.substring(0, input.length() - 1)));
            }

            if (SHORT_PATTERN.matcher(input).matches()) {
                return NBTTagShort.valueOf(Short.parseShort(input.substring(0, input.length() - 1)));
            }

            if (INT_PATTERN.matcher(input).matches()) {
                return NBTTagInt.valueOf(Integer.parseInt(input));
            }

            if (DOUBLE_PATTERN.matcher(input).matches()) {
                return NBTTagDouble.valueOf(Double.parseDouble(input.substring(0, input.length() - 1)));
            }

            if (DOUBLE_PATTERN_NOSUFFIX.matcher(input).matches()) {
                return NBTTagDouble.valueOf(Double.parseDouble(input));
            }

            if ("true".equalsIgnoreCase(input)) {
                return NBTTagByte.ONE;
            }

            if ("false".equalsIgnoreCase(input)) {
                return NBTTagByte.ZERO;
            }
        } catch (NumberFormatException var3) {
        }

        return NBTTagString.valueOf(input);
    }

    public NBTBase readValue() throws CommandSyntaxException {
        this.reader.skipWhitespace();
        if (!this.reader.canRead()) {
            throw ERROR_EXPECTED_VALUE.createWithContext(this.reader);
        } else {
            char c = this.reader.peek();
            if (c == '{') {
                return this.readStruct();
            } else {
                return c == '[' ? this.readList() : this.readTypedValue();
            }
        }
    }

    protected NBTBase readList() throws CommandSyntaxException {
        return this.reader.canRead(3) && !StringReader.isQuotedStringStart(this.reader.peek(1)) && this.reader.peek(2) == ';' ? this.parseArray() : this.readListTag();
    }

    public NBTTagCompound readStruct() throws CommandSyntaxException {
        this.expect('{');
        NBTTagCompound compoundTag = new NBTTagCompound();
        this.reader.skipWhitespace();

        while(this.reader.canRead() && this.reader.peek() != '}') {
            int i = this.reader.getCursor();
            String string = this.readKey();
            if (string.isEmpty()) {
                this.reader.setCursor(i);
                throw ERROR_EXPECTED_KEY.createWithContext(this.reader);
            }

            this.expect(':');
            compoundTag.set(string, this.readValue());
            if (!this.hasElementSeparator()) {
                break;
            }

            if (!this.reader.canRead()) {
                throw ERROR_EXPECTED_KEY.createWithContext(this.reader);
            }
        }

        this.expect('}');
        return compoundTag;
    }

    private NBTBase readListTag() throws CommandSyntaxException {
        this.expect('[');
        this.reader.skipWhitespace();
        if (!this.reader.canRead()) {
            throw ERROR_EXPECTED_VALUE.createWithContext(this.reader);
        } else {
            NBTTagList listTag = new NBTTagList();
            NBTTagType<?> tagType = null;

            while(this.reader.peek() != ']') {
                int i = this.reader.getCursor();
                NBTBase tag = this.readValue();
                NBTTagType<?> tagType2 = tag.getType();
                if (tagType == null) {
                    tagType = tagType2;
                } else if (tagType2 != tagType) {
                    this.reader.setCursor(i);
                    throw ERROR_INSERT_MIXED_LIST.createWithContext(this.reader, tagType2.getPrettyName(), tagType.getPrettyName());
                }

                listTag.add(tag);
                if (!this.hasElementSeparator()) {
                    break;
                }

                if (!this.reader.canRead()) {
                    throw ERROR_EXPECTED_VALUE.createWithContext(this.reader);
                }
            }

            this.expect(']');
            return listTag;
        }
    }

    public NBTBase parseArray() throws CommandSyntaxException {
        this.expect('[');
        int i = this.reader.getCursor();
        char c = this.reader.read();
        this.reader.read();
        this.reader.skipWhitespace();
        if (!this.reader.canRead()) {
            throw ERROR_EXPECTED_VALUE.createWithContext(this.reader);
        } else if (c == 'B') {
            return new NBTTagByteArray(this.readArray(NBTTagByteArray.TYPE, NBTTagByte.TYPE));
        } else if (c == 'L') {
            return new NBTTagLongArray(this.readArray(NBTTagLongArray.TYPE, NBTTagLong.TYPE));
        } else if (c == 'I') {
            return new NBTTagIntArray(this.readArray(NBTTagIntArray.TYPE, NBTTagInt.TYPE));
        } else {
            this.reader.setCursor(i);
            throw ERROR_INVALID_ARRAY.createWithContext(this.reader, String.valueOf(c));
        }
    }

    private <T extends Number> List<T> readArray(NBTTagType<?> arrayTypeReader, NBTTagType<?> typeReader) throws CommandSyntaxException {
        List<T> list = Lists.newArrayList();

        while(true) {
            if (this.reader.peek() != ']') {
                int i = this.reader.getCursor();
                NBTBase tag = this.readValue();
                NBTTagType<?> tagType = tag.getType();
                if (tagType != typeReader) {
                    this.reader.setCursor(i);
                    throw ERROR_INSERT_MIXED_ARRAY.createWithContext(this.reader, tagType.getPrettyName(), arrayTypeReader.getPrettyName());
                }

                if (typeReader == NBTTagByte.TYPE) {
                    list.add((T)((NBTNumber)tag).asByte());
                } else if (typeReader == NBTTagLong.TYPE) {
                    list.add((T)((NBTNumber)tag).asLong());
                } else {
                    list.add((T)((NBTNumber)tag).asInt());
                }

                if (this.hasElementSeparator()) {
                    if (!this.reader.canRead()) {
                        throw ERROR_EXPECTED_VALUE.createWithContext(this.reader);
                    }
                    continue;
                }
            }

            this.expect(']');
            return list;
        }
    }

    private boolean hasElementSeparator() {
        this.reader.skipWhitespace();
        if (this.reader.canRead() && this.reader.peek() == ',') {
            this.reader.skip();
            this.reader.skipWhitespace();
            return true;
        } else {
            return false;
        }
    }

    private void expect(char c) throws CommandSyntaxException {
        this.reader.skipWhitespace();
        this.reader.expect(c);
    }
}
