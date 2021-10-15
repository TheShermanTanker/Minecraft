package net.minecraft.tags;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;

public class TagSet<T> implements Tag<T> {
    private final ImmutableList<T> valuesList;
    private final Set<T> values;
    @VisibleForTesting
    protected final Class<?> closestCommonSuperType;

    protected TagSet(Set<T> values, Class<?> type) {
        this.closestCommonSuperType = type;
        this.values = values;
        this.valuesList = ImmutableList.copyOf(values);
    }

    public static <T> TagSet<T> empty() {
        return new TagSet<>(ImmutableSet.of(), Void.class);
    }

    public static <T> TagSet<T> create(Set<T> values) {
        return new TagSet<>(values, findCommonSuperClass(values));
    }

    @Override
    public boolean isTagged(T entry) {
        return this.closestCommonSuperType.isInstance(entry) && this.values.contains(entry);
    }

    @Override
    public List<T> getTagged() {
        return this.valuesList;
    }

    private static <T> Class<?> findCommonSuperClass(Set<T> values) {
        if (values.isEmpty()) {
            return Void.class;
        } else {
            Class<?> class_ = null;

            for(T object : values) {
                if (class_ == null) {
                    class_ = object.getClass();
                } else {
                    class_ = findClosestAncestor(class_, object.getClass());
                }
            }

            return class_;
        }
    }

    private static Class<?> findClosestAncestor(Class<?> first, Class<?> second) {
        while(!first.isAssignableFrom(second)) {
            first = first.getSuperclass();
        }

        return first;
    }
}
