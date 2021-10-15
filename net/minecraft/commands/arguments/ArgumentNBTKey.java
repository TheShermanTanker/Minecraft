package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.MojangsonParser;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.ChatMessage;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class ArgumentNBTKey implements ArgumentType<ArgumentNBTKey.NbtPath> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo.bar", "foo[0]", "[0]", "[]", "{foo=bar}");
    public static final SimpleCommandExceptionType ERROR_INVALID_NODE = new SimpleCommandExceptionType(new ChatMessage("arguments.nbtpath.node.invalid"));
    public static final DynamicCommandExceptionType ERROR_NOTHING_FOUND = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("arguments.nbtpath.nothing_found", object);
    });
    private static final char INDEX_MATCH_START = '[';
    private static final char INDEX_MATCH_END = ']';
    private static final char KEY_MATCH_START = '{';
    private static final char KEY_MATCH_END = '}';
    private static final char QUOTED_KEY_START = '"';

    public static ArgumentNBTKey nbtPath() {
        return new ArgumentNBTKey();
    }

    public static ArgumentNBTKey.NbtPath getPath(CommandContext<CommandListenerWrapper> context, String name) {
        return context.getArgument(name, ArgumentNBTKey.NbtPath.class);
    }

    @Override
    public ArgumentNBTKey.NbtPath parse(StringReader stringReader) throws CommandSyntaxException {
        List<ArgumentNBTKey.Node> list = Lists.newArrayList();
        int i = stringReader.getCursor();
        Object2IntMap<ArgumentNBTKey.Node> object2IntMap = new Object2IntOpenHashMap<>();
        boolean bl = true;

        while(stringReader.canRead() && stringReader.peek() != ' ') {
            ArgumentNBTKey.Node node = parseNode(stringReader, bl);
            list.add(node);
            object2IntMap.put(node, stringReader.getCursor() - i);
            bl = false;
            if (stringReader.canRead()) {
                char c = stringReader.peek();
                if (c != ' ' && c != '[' && c != '{') {
                    stringReader.expect('.');
                }
            }
        }

        return new ArgumentNBTKey.NbtPath(stringReader.getString().substring(i, stringReader.getCursor()), list.toArray(new ArgumentNBTKey.Node[0]), object2IntMap);
    }

    private static ArgumentNBTKey.Node parseNode(StringReader reader, boolean root) throws CommandSyntaxException {
        switch(reader.peek()) {
        case '"':
            String string = reader.readString();
            return readObjectNode(reader, string);
        case '[':
            reader.skip();
            int i = reader.peek();
            if (i == 123) {
                NBTTagCompound compoundTag2 = (new MojangsonParser(reader)).readStruct();
                reader.expect(']');
                return new ArgumentNBTKey.MatchElementNode(compoundTag2);
            } else {
                if (i == 93) {
                    reader.skip();
                    return ArgumentNBTKey.AllElementsNode.INSTANCE;
                }

                int j = reader.readInt();
                reader.expect(']');
                return new ArgumentNBTKey.IndexedElementNode(j);
            }
        case '{':
            if (!root) {
                throw ERROR_INVALID_NODE.createWithContext(reader);
            }

            NBTTagCompound compoundTag = (new MojangsonParser(reader)).readStruct();
            return new ArgumentNBTKey.MatchRootObjectNode(compoundTag);
        default:
            String string2 = readUnquotedName(reader);
            return readObjectNode(reader, string2);
        }
    }

    private static ArgumentNBTKey.Node readObjectNode(StringReader reader, String name) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '{') {
            NBTTagCompound compoundTag = (new MojangsonParser(reader)).readStruct();
            return new ArgumentNBTKey.MatchObjectNode(name, compoundTag);
        } else {
            return new ArgumentNBTKey.CompoundChildNode(name);
        }
    }

    private static String readUnquotedName(StringReader reader) throws CommandSyntaxException {
        int i = reader.getCursor();

        while(reader.canRead() && isAllowedInUnquotedName(reader.peek())) {
            reader.skip();
        }

        if (reader.getCursor() == i) {
            throw ERROR_INVALID_NODE.createWithContext(reader);
        } else {
            return reader.getString().substring(i, reader.getCursor());
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    private static boolean isAllowedInUnquotedName(char c) {
        return c != ' ' && c != '"' && c != '[' && c != ']' && c != '.' && c != '{' && c != '}';
    }

    static Predicate<NBTBase> createTagPredicate(NBTTagCompound filter) {
        return (tag) -> {
            return GameProfileSerializer.compareNbt(filter, tag, true);
        };
    }

    static class AllElementsNode implements ArgumentNBTKey.Node {
        public static final ArgumentNBTKey.AllElementsNode INSTANCE = new ArgumentNBTKey.AllElementsNode();

        private AllElementsNode() {
        }

        @Override
        public void getTag(NBTBase current, List<NBTBase> results) {
            if (current instanceof NBTList) {
                results.addAll((NBTList)current);
            }

        }

        @Override
        public void getOrCreateTag(NBTBase current, Supplier<NBTBase> source, List<NBTBase> results) {
            if (current instanceof NBTList) {
                NBTList<?> collectionTag = (NBTList)current;
                if (collectionTag.isEmpty()) {
                    NBTBase tag = source.get();
                    if (collectionTag.addTag(0, tag)) {
                        results.add(tag);
                    }
                } else {
                    results.addAll(collectionTag);
                }
            }

        }

        @Override
        public NBTBase createPreferredParentTag() {
            return new NBTTagList();
        }

        @Override
        public int setTag(NBTBase current, Supplier<NBTBase> source) {
            if (!(current instanceof NBTList)) {
                return 0;
            } else {
                NBTList<?> collectionTag = (NBTList)current;
                int i = collectionTag.size();
                if (i == 0) {
                    collectionTag.addTag(0, source.get());
                    return 1;
                } else {
                    NBTBase tag = source.get();
                    int j = i - (int)collectionTag.stream().filter(tag::equals).count();
                    if (j == 0) {
                        return 0;
                    } else {
                        collectionTag.clear();
                        if (!collectionTag.addTag(0, tag)) {
                            return 0;
                        } else {
                            for(int k = 1; k < i; ++k) {
                                collectionTag.addTag(k, source.get());
                            }

                            return j;
                        }
                    }
                }
            }
        }

        @Override
        public int removeTag(NBTBase current) {
            if (current instanceof NBTList) {
                NBTList<?> collectionTag = (NBTList)current;
                int i = collectionTag.size();
                if (i > 0) {
                    collectionTag.clear();
                    return i;
                }
            }

            return 0;
        }
    }

    static class CompoundChildNode implements ArgumentNBTKey.Node {
        private final String name;

        public CompoundChildNode(String name) {
            this.name = name;
        }

        @Override
        public void getTag(NBTBase current, List<NBTBase> results) {
            if (current instanceof NBTTagCompound) {
                NBTBase tag = ((NBTTagCompound)current).get(this.name);
                if (tag != null) {
                    results.add(tag);
                }
            }

        }

        @Override
        public void getOrCreateTag(NBTBase current, Supplier<NBTBase> source, List<NBTBase> results) {
            if (current instanceof NBTTagCompound) {
                NBTTagCompound compoundTag = (NBTTagCompound)current;
                NBTBase tag;
                if (compoundTag.hasKey(this.name)) {
                    tag = compoundTag.get(this.name);
                } else {
                    tag = source.get();
                    compoundTag.set(this.name, tag);
                }

                results.add(tag);
            }

        }

        @Override
        public NBTBase createPreferredParentTag() {
            return new NBTTagCompound();
        }

        @Override
        public int setTag(NBTBase current, Supplier<NBTBase> source) {
            if (current instanceof NBTTagCompound) {
                NBTTagCompound compoundTag = (NBTTagCompound)current;
                NBTBase tag = source.get();
                NBTBase tag2 = compoundTag.set(this.name, tag);
                if (!tag.equals(tag2)) {
                    return 1;
                }
            }

            return 0;
        }

        @Override
        public int removeTag(NBTBase current) {
            if (current instanceof NBTTagCompound) {
                NBTTagCompound compoundTag = (NBTTagCompound)current;
                if (compoundTag.hasKey(this.name)) {
                    compoundTag.remove(this.name);
                    return 1;
                }
            }

            return 0;
        }
    }

    static class IndexedElementNode implements ArgumentNBTKey.Node {
        private final int index;

        public IndexedElementNode(int index) {
            this.index = index;
        }

        @Override
        public void getTag(NBTBase current, List<NBTBase> results) {
            if (current instanceof NBTList) {
                NBTList<?> collectionTag = (NBTList)current;
                int i = collectionTag.size();
                int j = this.index < 0 ? i + this.index : this.index;
                if (0 <= j && j < i) {
                    results.add(collectionTag.get(j));
                }
            }

        }

        @Override
        public void getOrCreateTag(NBTBase current, Supplier<NBTBase> source, List<NBTBase> results) {
            this.getTag(current, results);
        }

        @Override
        public NBTBase createPreferredParentTag() {
            return new NBTTagList();
        }

        @Override
        public int setTag(NBTBase current, Supplier<NBTBase> source) {
            if (current instanceof NBTList) {
                NBTList<?> collectionTag = (NBTList)current;
                int i = collectionTag.size();
                int j = this.index < 0 ? i + this.index : this.index;
                if (0 <= j && j < i) {
                    NBTBase tag = collectionTag.get(j);
                    NBTBase tag2 = source.get();
                    if (!tag2.equals(tag) && collectionTag.setTag(j, tag2)) {
                        return 1;
                    }
                }
            }

            return 0;
        }

        @Override
        public int removeTag(NBTBase current) {
            if (current instanceof NBTList) {
                NBTList<?> collectionTag = (NBTList)current;
                int i = collectionTag.size();
                int j = this.index < 0 ? i + this.index : this.index;
                if (0 <= j && j < i) {
                    collectionTag.remove(j);
                    return 1;
                }
            }

            return 0;
        }
    }

    static class MatchElementNode implements ArgumentNBTKey.Node {
        private final NBTTagCompound pattern;
        private final Predicate<NBTBase> predicate;

        public MatchElementNode(NBTTagCompound filter) {
            this.pattern = filter;
            this.predicate = ArgumentNBTKey.createTagPredicate(filter);
        }

        @Override
        public void getTag(NBTBase current, List<NBTBase> results) {
            if (current instanceof NBTTagList) {
                NBTTagList listTag = (NBTTagList)current;
                listTag.stream().filter(this.predicate).forEach(results::add);
            }

        }

        @Override
        public void getOrCreateTag(NBTBase current, Supplier<NBTBase> source, List<NBTBase> results) {
            MutableBoolean mutableBoolean = new MutableBoolean();
            if (current instanceof NBTTagList) {
                NBTTagList listTag = (NBTTagList)current;
                listTag.stream().filter(this.predicate).forEach((tag) -> {
                    results.add(tag);
                    mutableBoolean.setTrue();
                });
                if (mutableBoolean.isFalse()) {
                    NBTTagCompound compoundTag = this.pattern.c();
                    listTag.add(compoundTag);
                    results.add(compoundTag);
                }
            }

        }

        @Override
        public NBTBase createPreferredParentTag() {
            return new NBTTagList();
        }

        @Override
        public int setTag(NBTBase current, Supplier<NBTBase> source) {
            int i = 0;
            if (current instanceof NBTTagList) {
                NBTTagList listTag = (NBTTagList)current;
                int j = listTag.size();
                if (j == 0) {
                    listTag.add(source.get());
                    ++i;
                } else {
                    for(int k = 0; k < j; ++k) {
                        NBTBase tag = listTag.get(k);
                        if (this.predicate.test(tag)) {
                            NBTBase tag2 = source.get();
                            if (!tag2.equals(tag) && listTag.setTag(k, tag2)) {
                                ++i;
                            }
                        }
                    }
                }
            }

            return i;
        }

        @Override
        public int removeTag(NBTBase current) {
            int i = 0;
            if (current instanceof NBTTagList) {
                NBTTagList listTag = (NBTTagList)current;

                for(int j = listTag.size() - 1; j >= 0; --j) {
                    if (this.predicate.test(listTag.get(j))) {
                        listTag.remove(j);
                        ++i;
                    }
                }
            }

            return i;
        }
    }

    static class MatchObjectNode implements ArgumentNBTKey.Node {
        private final String name;
        private final NBTTagCompound pattern;
        private final Predicate<NBTBase> predicate;

        public MatchObjectNode(String name, NBTTagCompound filter) {
            this.name = name;
            this.pattern = filter;
            this.predicate = ArgumentNBTKey.createTagPredicate(filter);
        }

        @Override
        public void getTag(NBTBase current, List<NBTBase> results) {
            if (current instanceof NBTTagCompound) {
                NBTBase tag = ((NBTTagCompound)current).get(this.name);
                if (this.predicate.test(tag)) {
                    results.add(tag);
                }
            }

        }

        @Override
        public void getOrCreateTag(NBTBase current, Supplier<NBTBase> source, List<NBTBase> results) {
            if (current instanceof NBTTagCompound) {
                NBTTagCompound compoundTag = (NBTTagCompound)current;
                NBTBase tag = compoundTag.get(this.name);
                if (tag == null) {
                    NBTBase var6 = this.pattern.c();
                    compoundTag.set(this.name, var6);
                    results.add(var6);
                } else if (this.predicate.test(tag)) {
                    results.add(tag);
                }
            }

        }

        @Override
        public NBTBase createPreferredParentTag() {
            return new NBTTagCompound();
        }

        @Override
        public int setTag(NBTBase current, Supplier<NBTBase> source) {
            if (current instanceof NBTTagCompound) {
                NBTTagCompound compoundTag = (NBTTagCompound)current;
                NBTBase tag = compoundTag.get(this.name);
                if (this.predicate.test(tag)) {
                    NBTBase tag2 = source.get();
                    if (!tag2.equals(tag)) {
                        compoundTag.set(this.name, tag2);
                        return 1;
                    }
                }
            }

            return 0;
        }

        @Override
        public int removeTag(NBTBase current) {
            if (current instanceof NBTTagCompound) {
                NBTTagCompound compoundTag = (NBTTagCompound)current;
                NBTBase tag = compoundTag.get(this.name);
                if (this.predicate.test(tag)) {
                    compoundTag.remove(this.name);
                    return 1;
                }
            }

            return 0;
        }
    }

    static class MatchRootObjectNode implements ArgumentNBTKey.Node {
        private final Predicate<NBTBase> predicate;

        public MatchRootObjectNode(NBTTagCompound filter) {
            this.predicate = ArgumentNBTKey.createTagPredicate(filter);
        }

        @Override
        public void getTag(NBTBase current, List<NBTBase> results) {
            if (current instanceof NBTTagCompound && this.predicate.test(current)) {
                results.add(current);
            }

        }

        @Override
        public void getOrCreateTag(NBTBase current, Supplier<NBTBase> source, List<NBTBase> results) {
            this.getTag(current, results);
        }

        @Override
        public NBTBase createPreferredParentTag() {
            return new NBTTagCompound();
        }

        @Override
        public int setTag(NBTBase current, Supplier<NBTBase> source) {
            return 0;
        }

        @Override
        public int removeTag(NBTBase current) {
            return 0;
        }
    }

    public static class NbtPath {
        private final String original;
        private final Object2IntMap<ArgumentNBTKey.Node> nodeToOriginalPosition;
        private final ArgumentNBTKey.Node[] nodes;

        public NbtPath(String string, ArgumentNBTKey.Node[] nodes, Object2IntMap<ArgumentNBTKey.Node> nodeEndIndices) {
            this.original = string;
            this.nodes = nodes;
            this.nodeToOriginalPosition = nodeEndIndices;
        }

        public List<NBTBase> get(NBTBase element) throws CommandSyntaxException {
            List<NBTBase> list = Collections.singletonList(element);

            for(ArgumentNBTKey.Node node : this.nodes) {
                list = node.get(list);
                if (list.isEmpty()) {
                    throw this.createNotFoundException(node);
                }
            }

            return list;
        }

        public int countMatching(NBTBase element) {
            List<NBTBase> list = Collections.singletonList(element);

            for(ArgumentNBTKey.Node node : this.nodes) {
                list = node.get(list);
                if (list.isEmpty()) {
                    return 0;
                }
            }

            return list.size();
        }

        private List<NBTBase> getOrCreateParents(NBTBase start) throws CommandSyntaxException {
            List<NBTBase> list = Collections.singletonList(start);

            for(int i = 0; i < this.nodes.length - 1; ++i) {
                ArgumentNBTKey.Node node = this.nodes[i];
                int j = i + 1;
                list = node.getOrCreate(list, this.nodes[j]::createPreferredParentTag);
                if (list.isEmpty()) {
                    throw this.createNotFoundException(node);
                }
            }

            return list;
        }

        public List<NBTBase> getOrCreate(NBTBase element, Supplier<NBTBase> source) throws CommandSyntaxException {
            List<NBTBase> list = this.getOrCreateParents(element);
            ArgumentNBTKey.Node node = this.nodes[this.nodes.length - 1];
            return node.getOrCreate(list, source);
        }

        private static int apply(List<NBTBase> elements, Function<NBTBase, Integer> operation) {
            return elements.stream().map(operation).reduce(0, (integer, integer2) -> {
                return integer + integer2;
            });
        }

        public int set(NBTBase element, NBTBase source) throws CommandSyntaxException {
            return this.set(element, source::clone);
        }

        public int set(NBTBase element, Supplier<NBTBase> source) throws CommandSyntaxException {
            List<NBTBase> list = this.getOrCreateParents(element);
            ArgumentNBTKey.Node node = this.nodes[this.nodes.length - 1];
            return apply(list, (tag) -> {
                return node.setTag(tag, source);
            });
        }

        public int remove(NBTBase element) {
            List<NBTBase> list = Collections.singletonList(element);

            for(int i = 0; i < this.nodes.length - 1; ++i) {
                list = this.nodes[i].get(list);
            }

            ArgumentNBTKey.Node node = this.nodes[this.nodes.length - 1];
            return apply(list, node::removeTag);
        }

        private CommandSyntaxException createNotFoundException(ArgumentNBTKey.Node node) {
            int i = this.nodeToOriginalPosition.getInt(node);
            return ArgumentNBTKey.ERROR_NOTHING_FOUND.create(this.original.substring(0, i));
        }

        @Override
        public String toString() {
            return this.original;
        }
    }

    interface Node {
        void getTag(NBTBase current, List<NBTBase> results);

        void getOrCreateTag(NBTBase current, Supplier<NBTBase> source, List<NBTBase> results);

        NBTBase createPreferredParentTag();

        int setTag(NBTBase current, Supplier<NBTBase> source);

        int removeTag(NBTBase current);

        default List<NBTBase> get(List<NBTBase> elements) {
            return this.collect(elements, this::getTag);
        }

        default List<NBTBase> getOrCreate(List<NBTBase> elements, Supplier<NBTBase> supplier) {
            return this.collect(elements, (current, results) -> {
                this.getOrCreateTag(current, supplier, results);
            });
        }

        default List<NBTBase> collect(List<NBTBase> elements, BiConsumer<NBTBase, List<NBTBase>> action) {
            List<NBTBase> list = Lists.newArrayList();

            for(NBTBase tag : elements) {
                action.accept(tag, list);
            }

            return list;
        }
    }
}
