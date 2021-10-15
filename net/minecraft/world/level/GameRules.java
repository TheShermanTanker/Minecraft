package net.minecraft.world.level;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.DynamicLike;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandDispatcher;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutEntityStatus;
import net.minecraft.network.protocol.game.PacketPlayOutGameStateChange;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GameRules {
    public static final int DEFAULT_RANDOM_TICK_SPEED = 3;
    static final Logger LOGGER = LogManager.getLogger();
    private static final Map<GameRules.GameRuleKey<?>, GameRules.GameRuleDefinition<?>> GAME_RULE_TYPES = Maps.newTreeMap(Comparator.comparing((key) -> {
        return key.id;
    }));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_DOFIRETICK = register("doFireTick", GameRules.GameRuleCategory.UPDATES, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_MOBGRIEFING = register("mobGriefing", GameRules.GameRuleCategory.MOBS, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_KEEPINVENTORY = register("keepInventory", GameRules.GameRuleCategory.PLAYER, GameRules.GameRuleBoolean.create(false));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_DOMOBSPAWNING = register("doMobSpawning", GameRules.GameRuleCategory.SPAWNING, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_DOMOBLOOT = register("doMobLoot", GameRules.GameRuleCategory.DROPS, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_DOBLOCKDROPS = register("doTileDrops", GameRules.GameRuleCategory.DROPS, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_DOENTITYDROPS = register("doEntityDrops", GameRules.GameRuleCategory.DROPS, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_COMMANDBLOCKOUTPUT = register("commandBlockOutput", GameRules.GameRuleCategory.CHAT, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_NATURAL_REGENERATION = register("naturalRegeneration", GameRules.GameRuleCategory.PLAYER, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_DAYLIGHT = register("doDaylightCycle", GameRules.GameRuleCategory.UPDATES, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_LOGADMINCOMMANDS = register("logAdminCommands", GameRules.GameRuleCategory.CHAT, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_SHOWDEATHMESSAGES = register("showDeathMessages", GameRules.GameRuleCategory.CHAT, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleInt> RULE_RANDOMTICKING = register("randomTickSpeed", GameRules.GameRuleCategory.UPDATES, GameRules.GameRuleInt.create(3));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_SENDCOMMANDFEEDBACK = register("sendCommandFeedback", GameRules.GameRuleCategory.CHAT, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_REDUCEDDEBUGINFO = register("reducedDebugInfo", GameRules.GameRuleCategory.MISC, GameRules.GameRuleBoolean.create(false, (server, rule) -> {
        byte b = (byte)(rule.get() ? 22 : 23);

        for(EntityPlayer serverPlayer : server.getPlayerList().getPlayers()) {
            serverPlayer.connection.sendPacket(new PacketPlayOutEntityStatus(serverPlayer, b));
        }

    }));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_SPECTATORSGENERATECHUNKS = register("spectatorsGenerateChunks", GameRules.GameRuleCategory.PLAYER, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleInt> RULE_SPAWN_RADIUS = register("spawnRadius", GameRules.GameRuleCategory.PLAYER, GameRules.GameRuleInt.create(10));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_DISABLE_ELYTRA_MOVEMENT_CHECK = register("disableElytraMovementCheck", GameRules.GameRuleCategory.PLAYER, GameRules.GameRuleBoolean.create(false));
    public static final GameRules.GameRuleKey<GameRules.GameRuleInt> RULE_MAX_ENTITY_CRAMMING = register("maxEntityCramming", GameRules.GameRuleCategory.MOBS, GameRules.GameRuleInt.create(24));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_WEATHER_CYCLE = register("doWeatherCycle", GameRules.GameRuleCategory.UPDATES, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_LIMITED_CRAFTING = register("doLimitedCrafting", GameRules.GameRuleCategory.PLAYER, GameRules.GameRuleBoolean.create(false));
    public static final GameRules.GameRuleKey<GameRules.GameRuleInt> RULE_MAX_COMMAND_CHAIN_LENGTH = register("maxCommandChainLength", GameRules.GameRuleCategory.MISC, GameRules.GameRuleInt.create(65536));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_ANNOUNCE_ADVANCEMENTS = register("announceAdvancements", GameRules.GameRuleCategory.CHAT, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_DISABLE_RAIDS = register("disableRaids", GameRules.GameRuleCategory.MOBS, GameRules.GameRuleBoolean.create(false));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_DOINSOMNIA = register("doInsomnia", GameRules.GameRuleCategory.SPAWNING, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_DO_IMMEDIATE_RESPAWN = register("doImmediateRespawn", GameRules.GameRuleCategory.PLAYER, GameRules.GameRuleBoolean.create(false, (server, rule) -> {
        for(EntityPlayer serverPlayer : server.getPlayerList().getPlayers()) {
            serverPlayer.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.IMMEDIATE_RESPAWN, rule.get() ? 1.0F : 0.0F));
        }

    }));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_DROWNING_DAMAGE = register("drowningDamage", GameRules.GameRuleCategory.PLAYER, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_FALL_DAMAGE = register("fallDamage", GameRules.GameRuleCategory.PLAYER, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_FIRE_DAMAGE = register("fireDamage", GameRules.GameRuleCategory.PLAYER, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_FREEZE_DAMAGE = register("freezeDamage", GameRules.GameRuleCategory.PLAYER, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_DO_PATROL_SPAWNING = register("doPatrolSpawning", GameRules.GameRuleCategory.SPAWNING, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_DO_TRADER_SPAWNING = register("doTraderSpawning", GameRules.GameRuleCategory.SPAWNING, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_FORGIVE_DEAD_PLAYERS = register("forgiveDeadPlayers", GameRules.GameRuleCategory.MOBS, GameRules.GameRuleBoolean.create(true));
    public static final GameRules.GameRuleKey<GameRules.GameRuleBoolean> RULE_UNIVERSAL_ANGER = register("universalAnger", GameRules.GameRuleCategory.MOBS, GameRules.GameRuleBoolean.create(false));
    public static final GameRules.GameRuleKey<GameRules.GameRuleInt> RULE_PLAYERS_SLEEPING_PERCENTAGE = register("playersSleepingPercentage", GameRules.GameRuleCategory.PLAYER, GameRules.GameRuleInt.create(100));
    private final Map<GameRules.GameRuleKey<?>, GameRules.GameRuleValue<?>> rules;

    private static <T extends GameRules.GameRuleValue<T>> GameRules.GameRuleKey<T> register(String name, GameRules.GameRuleCategory category, GameRules.GameRuleDefinition<T> type) {
        GameRules.GameRuleKey<T> key = new GameRules.GameRuleKey<>(name, category);
        GameRules.GameRuleDefinition<?> type2 = GAME_RULE_TYPES.put(key, type);
        if (type2 != null) {
            throw new IllegalStateException("Duplicate game rule registration for " + name);
        } else {
            return key;
        }
    }

    public GameRules(DynamicLike<?> dynamicLike) {
        this();
        this.loadFromTag(dynamicLike);
    }

    public GameRules() {
        this.rules = GAME_RULE_TYPES.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, (e) -> {
            return e.getValue().getValue();
        }));
    }

    private GameRules(Map<GameRules.GameRuleKey<?>, GameRules.GameRuleValue<?>> rules) {
        this.rules = rules;
    }

    public <T extends GameRules.GameRuleValue<T>> T get(GameRules.GameRuleKey<T> key) {
        return (T)(this.rules.get(key));
    }

    public NBTTagCompound createTag() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        this.rules.forEach((key, rule) -> {
            compoundTag.setString(key.id, rule.getValue());
        });
        return compoundTag;
    }

    private void loadFromTag(DynamicLike<?> dynamicLike) {
        this.rules.forEach((key, rule) -> {
            dynamicLike.get(key.id).asString().result().ifPresent(rule::setValue);
        });
    }

    public GameRules copy() {
        return new GameRules(this.rules.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, (entry) -> {
            return entry.getValue().copy();
        })));
    }

    public static void visitGameRuleTypes(GameRules.GameRuleVisitor visitor) {
        GAME_RULE_TYPES.forEach((key, type) -> {
            callVisitorCap(visitor, key, type);
        });
    }

    private static <T extends GameRules.GameRuleValue<T>> void callVisitorCap(GameRules.GameRuleVisitor consumer, GameRules.GameRuleKey<?> key, GameRules.GameRuleDefinition<?> type) {
        consumer.visit(key, type);
        type.callVisitor(consumer, key);
    }

    public void assignFrom(GameRules rules, @Nullable MinecraftServer server) {
        rules.rules.keySet().forEach((key) -> {
            this.assignCap(key, rules, server);
        });
    }

    private <T extends GameRules.GameRuleValue<T>> void assignCap(GameRules.GameRuleKey<T> key, GameRules rules, @Nullable MinecraftServer server) {
        T value = rules.get(key);
        this.<T>get(key).setFrom(value, server);
    }

    public boolean getBoolean(GameRules.GameRuleKey<GameRules.GameRuleBoolean> rule) {
        return this.get(rule).get();
    }

    public int getInt(GameRules.GameRuleKey<GameRules.GameRuleInt> rule) {
        return this.get(rule).get();
    }

    public static class GameRuleBoolean extends GameRules.GameRuleValue<GameRules.GameRuleBoolean> {
        private boolean value;

        static GameRules.GameRuleDefinition<GameRules.GameRuleBoolean> create(boolean initialValue, BiConsumer<MinecraftServer, GameRules.GameRuleBoolean> changeCallback) {
            return new GameRules.GameRuleDefinition<>(BoolArgumentType::bool, (type) -> {
                return new GameRules.GameRuleBoolean(type, initialValue);
            }, changeCallback, GameRules.GameRuleVisitor::visitBoolean);
        }

        static GameRules.GameRuleDefinition<GameRules.GameRuleBoolean> create(boolean initialValue) {
            return create(initialValue, (server, rule) -> {
            });
        }

        public GameRuleBoolean(GameRules.GameRuleDefinition<GameRules.GameRuleBoolean> type, boolean initialValue) {
            super(type);
            this.value = initialValue;
        }

        @Override
        protected void updateFromArgument(CommandContext<CommandListenerWrapper> context, String name) {
            this.value = BoolArgumentType.getBool(context, name);
        }

        public boolean get() {
            return this.value;
        }

        public void set(boolean value, @Nullable MinecraftServer server) {
            this.value = value;
            this.onChange(server);
        }

        @Override
        public String getValue() {
            return Boolean.toString(this.value);
        }

        @Override
        protected void setValue(String value) {
            this.value = Boolean.parseBoolean(value);
        }

        @Override
        public int getIntValue() {
            return this.value ? 1 : 0;
        }

        @Override
        protected GameRules.GameRuleBoolean getSelf() {
            return this;
        }

        @Override
        protected GameRules.GameRuleBoolean copy() {
            return new GameRules.GameRuleBoolean(this.type, this.value);
        }

        @Override
        public void setFrom(GameRules.GameRuleBoolean rule, @Nullable MinecraftServer server) {
            this.value = rule.value;
            this.onChange(server);
        }
    }

    public static enum GameRuleCategory {
        PLAYER("gamerule.category.player"),
        MOBS("gamerule.category.mobs"),
        SPAWNING("gamerule.category.spawning"),
        DROPS("gamerule.category.drops"),
        UPDATES("gamerule.category.updates"),
        CHAT("gamerule.category.chat"),
        MISC("gamerule.category.misc");

        private final String descriptionId;

        private GameRuleCategory(String category) {
            this.descriptionId = category;
        }

        public String getDescriptionId() {
            return this.descriptionId;
        }
    }

    public static class GameRuleDefinition<T extends GameRules.GameRuleValue<T>> {
        private final Supplier<ArgumentType<?>> argument;
        private final Function<GameRules.GameRuleDefinition<T>, T> constructor;
        final BiConsumer<MinecraftServer, T> callback;
        private final GameRules.VisitorCaller<T> visitorCaller;

        GameRuleDefinition(Supplier<ArgumentType<?>> argumentType, Function<GameRules.GameRuleDefinition<T>, T> ruleFactory, BiConsumer<MinecraftServer, T> changeCallback, GameRules.VisitorCaller<T> ruleAcceptor) {
            this.argument = argumentType;
            this.constructor = ruleFactory;
            this.callback = changeCallback;
            this.visitorCaller = ruleAcceptor;
        }

        public RequiredArgumentBuilder<CommandListenerWrapper, ?> createArgument(String name) {
            return CommandDispatcher.argument(name, this.argument.get());
        }

        public T getValue() {
            return this.constructor.apply(this);
        }

        public void callVisitor(GameRules.GameRuleVisitor consumer, GameRules.GameRuleKey<T> key) {
            this.visitorCaller.call(consumer, key, this);
        }
    }

    public static class GameRuleInt extends GameRules.GameRuleValue<GameRules.GameRuleInt> {
        private int value;

        private static GameRules.GameRuleDefinition<GameRules.GameRuleInt> create(int initialValue, BiConsumer<MinecraftServer, GameRules.GameRuleInt> changeCallback) {
            return new GameRules.GameRuleDefinition<>(IntegerArgumentType::integer, (type) -> {
                return new GameRules.GameRuleInt(type, initialValue);
            }, changeCallback, GameRules.GameRuleVisitor::visitInteger);
        }

        static GameRules.GameRuleDefinition<GameRules.GameRuleInt> create(int initialValue) {
            return create(initialValue, (server, rule) -> {
            });
        }

        public GameRuleInt(GameRules.GameRuleDefinition<GameRules.GameRuleInt> rule, int initialValue) {
            super(rule);
            this.value = initialValue;
        }

        @Override
        protected void updateFromArgument(CommandContext<CommandListenerWrapper> context, String name) {
            this.value = IntegerArgumentType.getInteger(context, name);
        }

        public int get() {
            return this.value;
        }

        public void set(int value, @Nullable MinecraftServer server) {
            this.value = value;
            this.onChange(server);
        }

        @Override
        public String getValue() {
            return Integer.toString(this.value);
        }

        @Override
        protected void setValue(String value) {
            this.value = safeParse(value);
        }

        public boolean tryDeserialize(String input) {
            try {
                this.value = Integer.parseInt(input);
                return true;
            } catch (NumberFormatException var3) {
                return false;
            }
        }

        private static int safeParse(String input) {
            if (!input.isEmpty()) {
                try {
                    return Integer.parseInt(input);
                } catch (NumberFormatException var2) {
                    GameRules.LOGGER.warn("Failed to parse integer {}", (Object)input);
                }
            }

            return 0;
        }

        @Override
        public int getIntValue() {
            return this.value;
        }

        @Override
        protected GameRules.GameRuleInt getSelf() {
            return this;
        }

        @Override
        protected GameRules.GameRuleInt copy() {
            return new GameRules.GameRuleInt(this.type, this.value);
        }

        @Override
        public void setFrom(GameRules.GameRuleInt rule, @Nullable MinecraftServer server) {
            this.value = rule.value;
            this.onChange(server);
        }
    }

    public static final class GameRuleKey<T extends GameRules.GameRuleValue<T>> {
        final String id;
        private final GameRules.GameRuleCategory category;

        public GameRuleKey(String name, GameRules.GameRuleCategory category) {
            this.id = name;
            this.category = category;
        }

        @Override
        public String toString() {
            return this.id;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else {
                return object instanceof GameRules.GameRuleKey && ((GameRules.GameRuleKey)object).id.equals(this.id);
            }
        }

        @Override
        public int hashCode() {
            return this.id.hashCode();
        }

        public String getId() {
            return this.id;
        }

        public String getDescriptionId() {
            return "gamerule." + this.id;
        }

        public GameRules.GameRuleCategory getCategory() {
            return this.category;
        }
    }

    public abstract static class GameRuleValue<T extends GameRules.GameRuleValue<T>> {
        protected final GameRules.GameRuleDefinition<T> type;

        public GameRuleValue(GameRules.GameRuleDefinition<T> type) {
            this.type = type;
        }

        protected abstract void updateFromArgument(CommandContext<CommandListenerWrapper> context, String name);

        public void setFromArgument(CommandContext<CommandListenerWrapper> context, String name) {
            this.updateFromArgument(context, name);
            this.onChange(context.getSource().getServer());
        }

        public void onChange(@Nullable MinecraftServer server) {
            if (server != null) {
                this.type.callback.accept(server, this.getSelf());
            }

        }

        protected abstract void setValue(String value);

        public abstract String getValue();

        @Override
        public String toString() {
            return this.getValue();
        }

        public abstract int getIntValue();

        protected abstract T getSelf();

        protected abstract T copy();

        public abstract void setFrom(T rule, @Nullable MinecraftServer server);
    }

    public interface GameRuleVisitor {
        default <T extends GameRules.GameRuleValue<T>> void visit(GameRules.GameRuleKey<T> key, GameRules.GameRuleDefinition<T> type) {
        }

        default void visitBoolean(GameRules.GameRuleKey<GameRules.GameRuleBoolean> key, GameRules.GameRuleDefinition<GameRules.GameRuleBoolean> type) {
        }

        default void visitInteger(GameRules.GameRuleKey<GameRules.GameRuleInt> key, GameRules.GameRuleDefinition<GameRules.GameRuleInt> type) {
        }
    }

    interface VisitorCaller<T extends GameRules.GameRuleValue<T>> {
        void call(GameRules.GameRuleVisitor consumer, GameRules.GameRuleKey<T> key, GameRules.GameRuleDefinition<T> type);
    }
}
