package net.minecraft.nbt.visitors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagType;
import net.minecraft.nbt.StreamTagVisitor;

public class CollectFields extends CollectToTag {
    private int fieldsToGetCount;
    private final Set<NBTTagType<?>> wantedTypes;
    private final Deque<CollectFields.StackFrame> stack = new ArrayDeque<>();

    public CollectFields(CollectFields.WantedField... queries) {
        this.fieldsToGetCount = queries.length;
        Builder<NBTTagType<?>> builder = ImmutableSet.builder();
        CollectFields.StackFrame stackFrame = new CollectFields.StackFrame(1);

        for(CollectFields.WantedField wantedField : queries) {
            stackFrame.addEntry(wantedField);
            builder.add(wantedField.type);
        }

        this.stack.push(stackFrame);
        builder.add(NBTTagCompound.TYPE);
        this.wantedTypes = builder.build();
    }

    @Override
    public StreamTagVisitor.ValueResult visitRootEntry(NBTTagType<?> rootType) {
        return rootType != NBTTagCompound.TYPE ? StreamTagVisitor.ValueResult.HALT : super.visitRootEntry(rootType);
    }

    @Override
    public StreamTagVisitor.EntryResult visitEntry(NBTTagType<?> type) {
        CollectFields.StackFrame stackFrame = this.stack.element();
        if (this.depth() > stackFrame.depth()) {
            return super.visitEntry(type);
        } else if (this.fieldsToGetCount <= 0) {
            return StreamTagVisitor.EntryResult.HALT;
        } else {
            return !this.wantedTypes.contains(type) ? StreamTagVisitor.EntryResult.SKIP : super.visitEntry(type);
        }
    }

    @Override
    public StreamTagVisitor.EntryResult visitEntry(NBTTagType<?> type, String key) {
        CollectFields.StackFrame stackFrame = this.stack.element();
        if (this.depth() > stackFrame.depth()) {
            return super.visitEntry(type, key);
        } else if (stackFrame.fieldsToGet.remove(key, type)) {
            --this.fieldsToGetCount;
            return super.visitEntry(type, key);
        } else {
            if (type == NBTTagCompound.TYPE) {
                CollectFields.StackFrame stackFrame2 = stackFrame.fieldsToRecurse.get(key);
                if (stackFrame2 != null) {
                    this.stack.push(stackFrame2);
                    return super.visitEntry(type, key);
                }
            }

            return StreamTagVisitor.EntryResult.SKIP;
        }
    }

    @Override
    public StreamTagVisitor.ValueResult visitContainerEnd() {
        if (this.depth() == this.stack.element().depth()) {
            this.stack.pop();
        }

        return super.visitContainerEnd();
    }

    public int getMissingFieldCount() {
        return this.fieldsToGetCount;
    }

    static record StackFrame(int depth, Map<String, NBTTagType<?>> fieldsToGet, Map<String, CollectFields.StackFrame> fieldsToRecurse) {
        public StackFrame(int depth) {
            this(depth, new HashMap<>(), new HashMap<>());
        }

        private StackFrame(int i, Map<String, NBTTagType<?>> map, Map<String, CollectFields.StackFrame> map2) {
            this.depth = i;
            this.fieldsToGet = map;
            this.fieldsToRecurse = map2;
        }

        public void addEntry(CollectFields.WantedField query) {
            if (this.depth <= query.path.size()) {
                this.fieldsToRecurse.computeIfAbsent(query.path.get(this.depth - 1), (path) -> {
                    return new CollectFields.StackFrame(this.depth + 1);
                }).addEntry(query);
            } else {
                this.fieldsToGet.put(query.name, query.type);
            }

        }

        public int depth() {
            return this.depth;
        }

        public Map<String, NBTTagType<?>> fieldsToGet() {
            return this.fieldsToGet;
        }

        public Map<String, CollectFields.StackFrame> fieldsToRecurse() {
            return this.fieldsToRecurse;
        }
    }

    public static record WantedField(List<String> path, NBTTagType<?> type, String name) {
        public WantedField(NBTTagType<?> type, String key) {
            this(List.of(), type, key);
        }

        public WantedField(String path, NBTTagType<?> type, String key) {
            this(List.of(path), type, key);
        }

        public WantedField(String path1, String path2, NBTTagType<?> type, String key) {
            this(List.of(path1, path2), type, key);
        }

        public WantedField(List<String> list, NBTTagType<?> tagType, String string) {
            this.path = list;
            this.type = tagType;
            this.name = string;
        }

        public List<String> path() {
            return this.path;
        }

        public NBTTagType<?> type() {
            return this.type;
        }

        public String name() {
            return this.name;
        }
    }
}
