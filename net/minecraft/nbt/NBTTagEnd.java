package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTTagEnd implements NBTBase {
    private static final int SELF_SIZE_IN_BITS = 64;
    public static final NBTTagType<NBTTagEnd> TYPE = new NBTTagType<NBTTagEnd>() {
        @Override
        public NBTTagEnd load(DataInput dataInput, int i, NBTReadLimiter nbtAccounter) {
            nbtAccounter.accountBits(64L);
            return NBTTagEnd.INSTANCE;
        }

        @Override
        public String getName() {
            return "END";
        }

        @Override
        public String getPrettyName() {
            return "TAG_End";
        }

        @Override
        public boolean isValue() {
            return true;
        }
    };
    public static final NBTTagEnd INSTANCE = new NBTTagEnd();

    private NBTTagEnd() {
    }

    @Override
    public void write(DataOutput output) throws IOException {
    }

    @Override
    public byte getTypeId() {
        return 0;
    }

    @Override
    public NBTTagType<NBTTagEnd> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return this.asString();
    }

    @Override
    public NBTTagEnd copy() {
        return this;
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitEnd(this);
    }
}
