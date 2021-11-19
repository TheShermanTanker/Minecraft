package net.minecraft.nbt;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class TagVisitorString implements TagVisitor {
    private static final Pattern SIMPLE_VALUE = Pattern.compile("[A-Za-z0-9._+-]+");
    private final StringBuilder builder = new StringBuilder();

    public String visit(NBTBase element) {
        element.accept(this);
        return this.builder.toString();
    }

    @Override
    public void visitString(NBTTagString element) {
        this.builder.append(NBTTagString.quoteAndEscape(element.asString()));
    }

    @Override
    public void visitByte(NBTTagByte element) {
        this.builder.append((Object)element.getAsNumber()).append('b');
    }

    @Override
    public void visitShort(NBTTagShort element) {
        this.builder.append((Object)element.getAsNumber()).append('s');
    }

    @Override
    public void visitInt(NBTTagInt element) {
        this.builder.append((Object)element.getAsNumber());
    }

    @Override
    public void visitLong(NBTTagLong element) {
        this.builder.append((Object)element.getAsNumber()).append('L');
    }

    @Override
    public void visitFloat(NBTTagFloat element) {
        this.builder.append(element.asFloat()).append('f');
    }

    @Override
    public void visitDouble(NBTTagDouble element) {
        this.builder.append(element.asDouble()).append('d');
    }

    @Override
    public void visitByteArray(NBTTagByteArray element) {
        this.builder.append("[B;");
        byte[] bs = element.getBytes();

        for(int i = 0; i < bs.length; ++i) {
            if (i != 0) {
                this.builder.append(',');
            }

            this.builder.append((int)bs[i]).append('B');
        }

        this.builder.append(']');
    }

    @Override
    public void visitIntArray(NBTTagIntArray element) {
        this.builder.append("[I;");
        int[] is = element.getInts();

        for(int i = 0; i < is.length; ++i) {
            if (i != 0) {
                this.builder.append(',');
            }

            this.builder.append(is[i]);
        }

        this.builder.append(']');
    }

    @Override
    public void visitLongArray(NBTTagLongArray element) {
        this.builder.append("[L;");
        long[] ls = element.getLongs();

        for(int i = 0; i < ls.length; ++i) {
            if (i != 0) {
                this.builder.append(',');
            }

            this.builder.append(ls[i]).append('L');
        }

        this.builder.append(']');
    }

    @Override
    public void visitList(NBTTagList element) {
        this.builder.append('[');

        for(int i = 0; i < element.size(); ++i) {
            if (i != 0) {
                this.builder.append(',');
            }

            this.builder.append((new TagVisitorString()).visit(element.get(i)));
        }

        this.builder.append(']');
    }

    @Override
    public void visitCompound(NBTTagCompound compound) {
        this.builder.append('{');
        List<String> list = Lists.newArrayList(compound.getKeys());
        Collections.sort(list);

        for(String string : list) {
            if (this.builder.length() != 1) {
                this.builder.append(',');
            }

            this.builder.append(handleEscape(string)).append(':').append((new TagVisitorString()).visit(compound.get(string)));
        }

        this.builder.append('}');
    }

    protected static String handleEscape(String name) {
        return SIMPLE_VALUE.matcher(name).matches() ? name : NBTTagString.quoteAndEscape(name);
    }

    @Override
    public void visitEnd(NBTTagEnd element) {
        this.builder.append("END");
    }
}
