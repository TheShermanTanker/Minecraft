package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

public class NBTTagString implements NBTBase {
    private static final int SELF_SIZE_IN_BITS = 288;
    public static final NBTTagType<NBTTagString> TYPE = new NBTTagType<NBTTagString>() {
        @Override
        public NBTTagString load(DataInput dataInput, int i, NBTReadLimiter nbtAccounter) throws IOException {
            nbtAccounter.accountBits(288L);
            String string = dataInput.readUTF();
            nbtAccounter.accountBits((long)(16 * string.length()));
            return NBTTagString.valueOf(string);
        }

        @Override
        public String getName() {
            return "STRING";
        }

        @Override
        public String getPrettyName() {
            return "TAG_String";
        }

        @Override
        public boolean isValue() {
            return true;
        }
    };
    private static final NBTTagString EMPTY = new NBTTagString("");
    private static final char DOUBLE_QUOTE = '"';
    private static final char SINGLE_QUOTE = '\'';
    private static final char ESCAPE = '\\';
    private static final char NOT_SET = '\u0000';
    private final String data;

    private NBTTagString(String value) {
        Objects.requireNonNull(value, "Null string not allowed");
        this.data = value;
    }

    public static NBTTagString valueOf(String value) {
        return value.isEmpty() ? EMPTY : new NBTTagString(value);
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeUTF(this.data);
    }

    @Override
    public byte getTypeId() {
        return 8;
    }

    @Override
    public NBTTagType<NBTTagString> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return NBTBase.super.asString();
    }

    @Override
    public NBTTagString copy() {
        return this;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            return object instanceof NBTTagString && Objects.equals(this.data, ((NBTTagString)object).data);
        }
    }

    @Override
    public int hashCode() {
        return this.data.hashCode();
    }

    @Override
    public String asString() {
        return this.data;
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitString(this);
    }

    public static String quoteAndEscape(String value) {
        StringBuilder stringBuilder = new StringBuilder(" ");
        char c = 0;

        for(int i = 0; i < value.length(); ++i) {
            char d = value.charAt(i);
            if (d == '\\') {
                stringBuilder.append('\\');
            } else if (d == '"' || d == '\'') {
                if (c == 0) {
                    c = (char)(d == '"' ? 39 : 34);
                }

                if (c == d) {
                    stringBuilder.append('\\');
                }
            }

            stringBuilder.append(d);
        }

        if (c == 0) {
            c = '"';
        }

        stringBuilder.setCharAt(0, c);
        stringBuilder.append(c);
        return stringBuilder.toString();
    }
}
