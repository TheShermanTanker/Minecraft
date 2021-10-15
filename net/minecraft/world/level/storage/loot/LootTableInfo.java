package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootTableInfo {
    private final Random random;
    private final float luck;
    private final WorldServer level;
    private final Function<MinecraftKey, LootTable> lootTables;
    private final Set<LootTable> visitedTables = Sets.newLinkedHashSet();
    private final Function<MinecraftKey, LootItemCondition> conditions;
    private final Set<LootItemCondition> visitedConditions = Sets.newLinkedHashSet();
    private final Map<LootContextParameter<?>, Object> params;
    private final Map<MinecraftKey, LootTableInfo.DynamicDrop> dynamicDrops;

    LootTableInfo(Random random, float luck, WorldServer world, Function<MinecraftKey, LootTable> tableGetter, Function<MinecraftKey, LootItemCondition> conditionGetter, Map<LootContextParameter<?>, Object> parameters, Map<MinecraftKey, LootTableInfo.DynamicDrop> drops) {
        this.random = random;
        this.luck = luck;
        this.level = world;
        this.lootTables = tableGetter;
        this.conditions = conditionGetter;
        this.params = ImmutableMap.copyOf(parameters);
        this.dynamicDrops = ImmutableMap.copyOf(drops);
    }

    public boolean hasContextParameter(LootContextParameter<?> parameter) {
        return this.params.containsKey(parameter);
    }

    public <T> T getParam(LootContextParameter<T> parameter) {
        T object = (T)this.params.get(parameter);
        if (object == null) {
            throw new NoSuchElementException(parameter.getName().toString());
        } else {
            return object;
        }
    }

    public void addDynamicDrops(MinecraftKey id, Consumer<ItemStack> lootConsumer) {
        LootTableInfo.DynamicDrop dynamicDrop = this.dynamicDrops.get(id);
        if (dynamicDrop != null) {
            dynamicDrop.add(this, lootConsumer);
        }

    }

    @Nullable
    public <T> T getContextParameter(LootContextParameter<T> parameter) {
        return (T)this.params.get(parameter);
    }

    public boolean addVisitedTable(LootTable table) {
        return this.visitedTables.add(table);
    }

    public void removeVisitedTable(LootTable table) {
        this.visitedTables.remove(table);
    }

    public boolean addVisitedCondition(LootItemCondition condition) {
        return this.visitedConditions.add(condition);
    }

    public void removeVisitedCondition(LootItemCondition condition) {
        this.visitedConditions.remove(condition);
    }

    public LootTable getLootTable(MinecraftKey id) {
        return this.lootTables.apply(id);
    }

    public LootItemCondition getCondition(MinecraftKey id) {
        return this.conditions.apply(id);
    }

    public Random getRandom() {
        return this.random;
    }

    public float getLuck() {
        return this.luck;
    }

    public WorldServer getWorld() {
        return this.level;
    }

    public static class Builder {
        private final WorldServer level;
        private final Map<LootContextParameter<?>, Object> params = Maps.newIdentityHashMap();
        private final Map<MinecraftKey, LootTableInfo.DynamicDrop> dynamicDrops = Maps.newHashMap();
        private Random random;
        private float luck;

        public Builder(WorldServer world) {
            this.level = world;
        }

        public LootTableInfo.Builder withRandom(Random random) {
            this.random = random;
            return this;
        }

        public LootTableInfo.Builder withOptionalRandomSeed(long seed) {
            if (seed != 0L) {
                this.random = new Random(seed);
            }

            return this;
        }

        public LootTableInfo.Builder withOptionalRandomSeed(long seed, Random random) {
            if (seed == 0L) {
                this.random = random;
            } else {
                this.random = new Random(seed);
            }

            return this;
        }

        public LootTableInfo.Builder withLuck(float luck) {
            this.luck = luck;
            return this;
        }

        public <T> LootTableInfo.Builder set(LootContextParameter<T> key, T value) {
            this.params.put(key, value);
            return this;
        }

        public <T> LootTableInfo.Builder setOptional(LootContextParameter<T> key, @Nullable T value) {
            if (value == null) {
                this.params.remove(key);
            } else {
                this.params.put(key, value);
            }

            return this;
        }

        public LootTableInfo.Builder withDynamicDrop(MinecraftKey id, LootTableInfo.DynamicDrop value) {
            LootTableInfo.DynamicDrop dynamicDrop = this.dynamicDrops.put(id, value);
            if (dynamicDrop != null) {
                throw new IllegalStateException("Duplicated dynamic drop '" + this.dynamicDrops + "'");
            } else {
                return this;
            }
        }

        public WorldServer getLevel() {
            return this.level;
        }

        public <T> T getParameter(LootContextParameter<T> parameter) {
            T object = (T)this.params.get(parameter);
            if (object == null) {
                throw new IllegalArgumentException("No parameter " + parameter);
            } else {
                return object;
            }
        }

        @Nullable
        public <T> T getOptionalParameter(LootContextParameter<T> parameter) {
            return (T)this.params.get(parameter);
        }

        public LootTableInfo build(LootContextParameterSet type) {
            Set<LootContextParameter<?>> set = Sets.difference(this.params.keySet(), type.getOptional());
            if (!set.isEmpty()) {
                throw new IllegalArgumentException("Parameters not allowed in this parameter set: " + set);
            } else {
                Set<LootContextParameter<?>> set2 = Sets.difference(type.getRequired(), this.params.keySet());
                if (!set2.isEmpty()) {
                    throw new IllegalArgumentException("Missing required parameters: " + set2);
                } else {
                    Random random = this.random;
                    if (random == null) {
                        random = new Random();
                    }

                    MinecraftServer minecraftServer = this.level.getMinecraftServer();
                    return new LootTableInfo(random, this.luck, this.level, minecraftServer.getLootTableRegistry()::getLootTable, minecraftServer.getLootPredicateManager()::get, this.params, this.dynamicDrops);
                }
            }
        }
    }

    @FunctionalInterface
    public interface DynamicDrop {
        void add(LootTableInfo context, Consumer<ItemStack> consumer);
    }

    public static enum EntityTarget {
        THIS("this", LootContextParameters.THIS_ENTITY),
        KILLER("killer", LootContextParameters.KILLER_ENTITY),
        DIRECT_KILLER("direct_killer", LootContextParameters.DIRECT_KILLER_ENTITY),
        KILLER_PLAYER("killer_player", LootContextParameters.LAST_DAMAGE_PLAYER);

        final String name;
        private final LootContextParameter<? extends Entity> param;

        private EntityTarget(String type, LootContextParameter<? extends Entity> parameter) {
            this.name = type;
            this.param = parameter;
        }

        public LootContextParameter<? extends Entity> getParam() {
            return this.param;
        }

        public static LootTableInfo.EntityTarget getByName(String type) {
            for(LootTableInfo.EntityTarget entityTarget : values()) {
                if (entityTarget.name.equals(type)) {
                    return entityTarget;
                }
            }

            throw new IllegalArgumentException("Invalid entity target " + type);
        }

        public static class Serializer extends TypeAdapter<LootTableInfo.EntityTarget> {
            @Override
            public void write(JsonWriter jsonWriter, LootTableInfo.EntityTarget entityTarget) throws IOException {
                jsonWriter.value(entityTarget.name);
            }

            @Override
            public LootTableInfo.EntityTarget read(JsonReader jsonReader) throws IOException {
                return LootTableInfo.EntityTarget.getByName(jsonReader.nextString());
            }
        }
    }
}
