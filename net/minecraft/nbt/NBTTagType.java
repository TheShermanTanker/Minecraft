package net.minecraft.nbt;

import java.io.DataInput;
import java.io.IOException;

public interface NBTTagType<T extends NBTBase> {
    T load(DataInput input, int depth, NBTReadLimiter tracker) throws IOException;

    StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor visitor) throws IOException;

    default void parseRoot(DataInput input, StreamTagVisitor visitor) throws IOException {
        switch(visitor.visitRootEntry(this)) {
        case CONTINUE:
            this.parse(input, visitor);
        case HALT:
        default:
            break;
        case BREAK:
            this.skip(input);
        }

    }

    void skip(DataInput input, int count) throws IOException;

    void skip(DataInput input) throws IOException;

    default boolean isValue() {
        return false;
    }

    String getName();

    String getPrettyName();

    static NBTTagType<NBTTagEnd> createInvalid(int type) {
        return new NBTTagType<NBTTagEnd>() {
            private IOException createException() {
                return new IOException("Invalid tag id: " + type);
            }

            @Override
            public NBTTagEnd load(DataInput dataInput, int i, NBTReadLimiter nbtAccounter) throws IOException {
                throw this.createException();
            }

            @Override
            public StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor visitor) throws IOException {
                throw this.createException();
            }

            @Override
            public void skip(DataInput input, int count) throws IOException {
                throw this.createException();
            }

            @Override
            public void skip(DataInput input) throws IOException {
                throw this.createException();
            }

            @Override
            public String getName() {
                return "INVALID[" + type + "]";
            }

            @Override
            public String getPrettyName() {
                return "UNKNOWN_" + type;
            }
        };
    }
}
