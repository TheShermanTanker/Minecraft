package net.minecraft.world.level.storage.loot.parameters;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.world.level.storage.loot.LootCollector;
import net.minecraft.world.level.storage.loot.LootItemUser;

public class LootContextParameterSet {
    private final Set<LootContextParameter<?>> required;
    private final Set<LootContextParameter<?>> all;

    LootContextParameterSet(Set<LootContextParameter<?>> required, Set<LootContextParameter<?>> allowed) {
        this.required = ImmutableSet.copyOf(required);
        this.all = ImmutableSet.copyOf(Sets.union(required, allowed));
    }

    public boolean isAllowed(LootContextParameter<?> parameter) {
        return this.all.contains(parameter);
    }

    public Set<LootContextParameter<?>> getRequired() {
        return this.required;
    }

    public Set<LootContextParameter<?>> getOptional() {
        return this.all;
    }

    @Override
    public String toString() {
        return "[" + Joiner.on(", ").join(this.all.stream().map((parameter) -> {
            return (this.required.contains(parameter) ? "!" : "") + parameter.getName();
        }).iterator()) + "]";
    }

    public void validateUser(LootCollector reporter, LootItemUser parameterConsumer) {
        Set<LootContextParameter<?>> set = parameterConsumer.getReferencedContextParams();
        Set<LootContextParameter<?>> set2 = Sets.difference(set, this.all);
        if (!set2.isEmpty()) {
            reporter.reportProblem("Parameters " + set2 + " are not provided in this context");
        }

    }

    public static LootContextParameterSet.Builder builder() {
        return new LootContextParameterSet.Builder();
    }

    public static class Builder {
        private final Set<LootContextParameter<?>> required = Sets.newIdentityHashSet();
        private final Set<LootContextParameter<?>> optional = Sets.newIdentityHashSet();

        public LootContextParameterSet.Builder addRequired(LootContextParameter<?> parameter) {
            if (this.optional.contains(parameter)) {
                throw new IllegalArgumentException("Parameter " + parameter.getName() + " is already optional");
            } else {
                this.required.add(parameter);
                return this;
            }
        }

        public LootContextParameterSet.Builder addOptional(LootContextParameter<?> parameter) {
            if (this.required.contains(parameter)) {
                throw new IllegalArgumentException("Parameter " + parameter.getName() + " is already required");
            } else {
                this.optional.add(parameter);
                return this;
            }
        }

        public LootContextParameterSet build() {
            return new LootContextParameterSet(this.required, this.optional);
        }
    }
}
