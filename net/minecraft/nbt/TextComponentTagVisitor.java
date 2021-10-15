package net.minecraft.nbt;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.bytes.ByteCollection;
import it.unimi.dsi.fastutil.bytes.ByteOpenHashSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.EnumChatFormat;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TextComponentTagVisitor implements TagVisitor {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int INLINE_LIST_THRESHOLD = 8;
    private static final ByteCollection INLINE_ELEMENT_TYPES = new ByteOpenHashSet(Arrays.asList((byte)1, (byte)2, (byte)3, (byte)4, (byte)5, (byte)6));
    private static final EnumChatFormat SYNTAX_HIGHLIGHTING_KEY = EnumChatFormat.AQUA;
    private static final EnumChatFormat SYNTAX_HIGHLIGHTING_STRING = EnumChatFormat.GREEN;
    private static final EnumChatFormat SYNTAX_HIGHLIGHTING_NUMBER = EnumChatFormat.GOLD;
    private static final EnumChatFormat SYNTAX_HIGHLIGHTING_NUMBER_TYPE = EnumChatFormat.RED;
    private static final Pattern SIMPLE_VALUE = Pattern.compile("[A-Za-z0-9._+-]+");
    private static final String NAME_VALUE_SEPARATOR = String.valueOf(':');
    private static final String ELEMENT_SEPARATOR = String.valueOf(',');
    private static final String LIST_OPEN = "[";
    private static final String LIST_CLOSE = "]";
    private static final String LIST_TYPE_SEPARATOR = ";";
    private static final String ELEMENT_SPACING = " ";
    private static final String STRUCT_OPEN = "{";
    private static final String STRUCT_CLOSE = "}";
    private static final String NEWLINE = "\n";
    private final String indentation;
    private final int depth;
    private IChatBaseComponent result;

    public TextComponentTagVisitor(String prefix, int indentationLevel) {
        this.indentation = prefix;
        this.depth = indentationLevel;
    }

    public IChatBaseComponent visit(NBTBase element) {
        element.accept(this);
        return this.result;
    }

    @Override
    public void visitString(NBTTagString element) {
        String string = NBTTagString.quoteAndEscape(element.asString());
        String string2 = string.substring(0, 1);
        IChatBaseComponent component = (new ChatComponentText(string.substring(1, string.length() - 1))).withStyle(SYNTAX_HIGHLIGHTING_STRING);
        this.result = (new ChatComponentText(string2)).addSibling(component).append(string2);
    }

    @Override
    public void visitByte(NBTTagByte element) {
        IChatBaseComponent component = (new ChatComponentText("b")).withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
        this.result = (new ChatComponentText(String.valueOf((Object)element.getAsNumber()))).addSibling(component).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
    }

    @Override
    public void visitShort(NBTTagShort element) {
        IChatBaseComponent component = (new ChatComponentText("s")).withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
        this.result = (new ChatComponentText(String.valueOf((Object)element.getAsNumber()))).addSibling(component).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
    }

    @Override
    public void visitInt(NBTTagInt element) {
        this.result = (new ChatComponentText(String.valueOf((Object)element.getAsNumber()))).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
    }

    @Override
    public void visitLong(NBTTagLong element) {
        IChatBaseComponent component = (new ChatComponentText("L")).withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
        this.result = (new ChatComponentText(String.valueOf((Object)element.getAsNumber()))).addSibling(component).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
    }

    @Override
    public void visitFloat(NBTTagFloat element) {
        IChatBaseComponent component = (new ChatComponentText("f")).withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
        this.result = (new ChatComponentText(String.valueOf(element.asFloat()))).addSibling(component).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
    }

    @Override
    public void visitDouble(NBTTagDouble element) {
        IChatBaseComponent component = (new ChatComponentText("d")).withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
        this.result = (new ChatComponentText(String.valueOf(element.asDouble()))).addSibling(component).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
    }

    @Override
    public void visitByteArray(NBTTagByteArray element) {
        IChatBaseComponent component = (new ChatComponentText("B")).withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
        IChatMutableComponent mutableComponent = (new ChatComponentText("[")).addSibling(component).append(";");
        byte[] bs = element.getBytes();

        for(int i = 0; i < bs.length; ++i) {
            IChatMutableComponent mutableComponent2 = (new ChatComponentText(String.valueOf((int)bs[i]))).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
            mutableComponent.append(" ").addSibling(mutableComponent2).addSibling(component);
            if (i != bs.length - 1) {
                mutableComponent.append(ELEMENT_SEPARATOR);
            }
        }

        mutableComponent.append("]");
        this.result = mutableComponent;
    }

    @Override
    public void visitIntArray(NBTTagIntArray element) {
        IChatBaseComponent component = (new ChatComponentText("I")).withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
        IChatMutableComponent mutableComponent = (new ChatComponentText("[")).addSibling(component).append(";");
        int[] is = element.getInts();

        for(int i = 0; i < is.length; ++i) {
            mutableComponent.append(" ").addSibling((new ChatComponentText(String.valueOf(is[i]))).withStyle(SYNTAX_HIGHLIGHTING_NUMBER));
            if (i != is.length - 1) {
                mutableComponent.append(ELEMENT_SEPARATOR);
            }
        }

        mutableComponent.append("]");
        this.result = mutableComponent;
    }

    @Override
    public void visitLongArray(NBTTagLongArray element) {
        IChatBaseComponent component = (new ChatComponentText("L")).withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
        IChatMutableComponent mutableComponent = (new ChatComponentText("[")).addSibling(component).append(";");
        long[] ls = element.getLongs();

        for(int i = 0; i < ls.length; ++i) {
            IChatBaseComponent component2 = (new ChatComponentText(String.valueOf(ls[i]))).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
            mutableComponent.append(" ").addSibling(component2).addSibling(component);
            if (i != ls.length - 1) {
                mutableComponent.append(ELEMENT_SEPARATOR);
            }
        }

        mutableComponent.append("]");
        this.result = mutableComponent;
    }

    @Override
    public void visitList(NBTTagList element) {
        if (element.isEmpty()) {
            this.result = new ChatComponentText("[]");
        } else if (INLINE_ELEMENT_TYPES.contains(element.getElementType()) && element.size() <= 8) {
            String string = ELEMENT_SEPARATOR + " ";
            IChatMutableComponent mutableComponent = new ChatComponentText("[");

            for(int i = 0; i < element.size(); ++i) {
                if (i != 0) {
                    mutableComponent.append(string);
                }

                mutableComponent.addSibling((new TextComponentTagVisitor(this.indentation, this.depth)).visit(element.get(i)));
            }

            mutableComponent.append("]");
            this.result = mutableComponent;
        } else {
            IChatMutableComponent mutableComponent2 = new ChatComponentText("[");
            if (!this.indentation.isEmpty()) {
                mutableComponent2.append("\n");
            }

            for(int j = 0; j < element.size(); ++j) {
                IChatMutableComponent mutableComponent3 = new ChatComponentText(Strings.repeat(this.indentation, this.depth + 1));
                mutableComponent3.addSibling((new TextComponentTagVisitor(this.indentation, this.depth + 1)).visit(element.get(j)));
                if (j != element.size() - 1) {
                    mutableComponent3.append(ELEMENT_SEPARATOR).append(this.indentation.isEmpty() ? " " : "\n");
                }

                mutableComponent2.addSibling(mutableComponent3);
            }

            if (!this.indentation.isEmpty()) {
                mutableComponent2.append("\n").append(Strings.repeat(this.indentation, this.depth));
            }

            mutableComponent2.append("]");
            this.result = mutableComponent2;
        }
    }

    @Override
    public void visitCompound(NBTTagCompound compound) {
        if (compound.isEmpty()) {
            this.result = new ChatComponentText("{}");
        } else {
            IChatMutableComponent mutableComponent = new ChatComponentText("{");
            Collection<String> collection = compound.getKeys();
            if (LOGGER.isDebugEnabled()) {
                List<String> list = Lists.newArrayList(compound.getKeys());
                Collections.sort(list);
                collection = list;
            }

            if (!this.indentation.isEmpty()) {
                mutableComponent.append("\n");
            }

            IChatMutableComponent mutableComponent2;
            for(Iterator<String> iterator = collection.iterator(); iterator.hasNext(); mutableComponent.addSibling(mutableComponent2)) {
                String string = iterator.next();
                mutableComponent2 = (new ChatComponentText(Strings.repeat(this.indentation, this.depth + 1))).addSibling(handleEscapePretty(string)).append(NAME_VALUE_SEPARATOR).append(" ").addSibling((new TextComponentTagVisitor(this.indentation, this.depth + 1)).visit(compound.get(string)));
                if (iterator.hasNext()) {
                    mutableComponent2.append(ELEMENT_SEPARATOR).append(this.indentation.isEmpty() ? " " : "\n");
                }
            }

            if (!this.indentation.isEmpty()) {
                mutableComponent.append("\n").append(Strings.repeat(this.indentation, this.depth));
            }

            mutableComponent.append("}");
            this.result = mutableComponent;
        }
    }

    protected static IChatBaseComponent handleEscapePretty(String name) {
        if (SIMPLE_VALUE.matcher(name).matches()) {
            return (new ChatComponentText(name)).withStyle(SYNTAX_HIGHLIGHTING_KEY);
        } else {
            String string = NBTTagString.quoteAndEscape(name);
            String string2 = string.substring(0, 1);
            IChatBaseComponent component = (new ChatComponentText(string.substring(1, string.length() - 1))).withStyle(SYNTAX_HIGHLIGHTING_KEY);
            return (new ChatComponentText(string2)).addSibling(component).append(string2);
        }
    }

    @Override
    public void visitEnd(NBTTagEnd element) {
        this.result = ChatComponentText.EMPTY;
    }
}
