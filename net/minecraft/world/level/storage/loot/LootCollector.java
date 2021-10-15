package net.minecraft.world.level.storage.loot;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSet;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootCollector {
    private final Multimap<String, String> problems;
    private final Supplier<String> context;
    private final LootContextParameterSet params;
    private final Function<MinecraftKey, LootItemCondition> conditionResolver;
    private final Set<MinecraftKey> visitedConditions;
    private final Function<MinecraftKey, LootTable> tableResolver;
    private final Set<MinecraftKey> visitedTables;
    private String contextCache;

    public LootCollector(LootContextParameterSet contextType, Function<MinecraftKey, LootItemCondition> conditionGetter, Function<MinecraftKey, LootTable> tableFactory) {
        this(HashMultimap.create(), () -> {
            return "";
        }, contextType, conditionGetter, ImmutableSet.of(), tableFactory, ImmutableSet.of());
    }

    public LootCollector(Multimap<String, String> messages, Supplier<String> nameFactory, LootContextParameterSet contextType, Function<MinecraftKey, LootItemCondition> conditionGetter, Set<MinecraftKey> conditions, Function<MinecraftKey, LootTable> tableGetter, Set<MinecraftKey> tables) {
        this.problems = messages;
        this.context = nameFactory;
        this.params = contextType;
        this.conditionResolver = conditionGetter;
        this.visitedConditions = conditions;
        this.tableResolver = tableGetter;
        this.visitedTables = tables;
    }

    private String getContext() {
        if (this.contextCache == null) {
            this.contextCache = this.context.get();
        }

        return this.contextCache;
    }

    public void reportProblem(String message) {
        this.problems.put(this.getContext(), message);
    }

    public LootCollector forChild(String name) {
        return new LootCollector(this.problems, () -> {
            return this.getContext() + name;
        }, this.params, this.conditionResolver, this.visitedConditions, this.tableResolver, this.visitedTables);
    }

    public LootCollector enterTable(String name, MinecraftKey id) {
        ImmutableSet<MinecraftKey> immutableSet = ImmutableSet.<MinecraftKey>builder().addAll(this.visitedTables).add(id).build();
        return new LootCollector(this.problems, () -> {
            return this.getContext() + name;
        }, this.params, this.conditionResolver, this.visitedConditions, this.tableResolver, immutableSet);
    }

    public LootCollector enterCondition(String name, MinecraftKey id) {
        ImmutableSet<MinecraftKey> immutableSet = ImmutableSet.<MinecraftKey>builder().addAll(this.visitedConditions).add(id).build();
        return new LootCollector(this.problems, () -> {
            return this.getContext() + name;
        }, this.params, this.conditionResolver, immutableSet, this.tableResolver, this.visitedTables);
    }

    public boolean hasVisitedTable(MinecraftKey id) {
        return this.visitedTables.contains(id);
    }

    public boolean hasVisitedCondition(MinecraftKey id) {
        return this.visitedConditions.contains(id);
    }

    public Multimap<String, String> getProblems() {
        return ImmutableMultimap.copyOf(this.problems);
    }

    public void validateUser(LootItemUser contextAware) {
        this.params.validateUser(this, contextAware);
    }

    @Nullable
    public LootTable resolveLootTable(MinecraftKey id) {
        return this.tableResolver.apply(id);
    }

    @Nullable
    public LootItemCondition resolveCondition(MinecraftKey id) {
        return this.conditionResolver.apply(id);
    }

    public LootCollector setParams(LootContextParameterSet contextType) {
        return new LootCollector(this.problems, this.context, contextType, this.conditionResolver, this.visitedConditions, this.tableResolver, this.visitedTables);
    }
}
