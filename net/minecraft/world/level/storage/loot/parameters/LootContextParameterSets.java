package net.minecraft.world.level.storage.loot.parameters;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.resources.MinecraftKey;

public class LootContextParameterSets {
    private static final BiMap<MinecraftKey, LootContextParameterSet> REGISTRY = HashBiMap.create();
    public static final LootContextParameterSet EMPTY = register("empty", (builder) -> {
    });
    public static final LootContextParameterSet CHEST = register("chest", (builder) -> {
        builder.addRequired(LootContextParameters.ORIGIN).addOptional(LootContextParameters.THIS_ENTITY);
    });
    public static final LootContextParameterSet COMMAND = register("command", (builder) -> {
        builder.addRequired(LootContextParameters.ORIGIN).addOptional(LootContextParameters.THIS_ENTITY);
    });
    public static final LootContextParameterSet SELECTOR = register("selector", (builder) -> {
        builder.addRequired(LootContextParameters.ORIGIN).addRequired(LootContextParameters.THIS_ENTITY);
    });
    public static final LootContextParameterSet FISHING = register("fishing", (builder) -> {
        builder.addRequired(LootContextParameters.ORIGIN).addRequired(LootContextParameters.TOOL).addOptional(LootContextParameters.THIS_ENTITY);
    });
    public static final LootContextParameterSet ENTITY = register("entity", (builder) -> {
        builder.addRequired(LootContextParameters.THIS_ENTITY).addRequired(LootContextParameters.ORIGIN).addRequired(LootContextParameters.DAMAGE_SOURCE).addOptional(LootContextParameters.KILLER_ENTITY).addOptional(LootContextParameters.DIRECT_KILLER_ENTITY).addOptional(LootContextParameters.LAST_DAMAGE_PLAYER);
    });
    public static final LootContextParameterSet GIFT = register("gift", (builder) -> {
        builder.addRequired(LootContextParameters.ORIGIN).addRequired(LootContextParameters.THIS_ENTITY);
    });
    public static final LootContextParameterSet PIGLIN_BARTER = register("barter", (builder) -> {
        builder.addRequired(LootContextParameters.THIS_ENTITY);
    });
    public static final LootContextParameterSet ADVANCEMENT_REWARD = register("advancement_reward", (builder) -> {
        builder.addRequired(LootContextParameters.THIS_ENTITY).addRequired(LootContextParameters.ORIGIN);
    });
    public static final LootContextParameterSet ADVANCEMENT_ENTITY = register("advancement_entity", (builder) -> {
        builder.addRequired(LootContextParameters.THIS_ENTITY).addRequired(LootContextParameters.ORIGIN);
    });
    public static final LootContextParameterSet ALL_PARAMS = register("generic", (builder) -> {
        builder.addRequired(LootContextParameters.THIS_ENTITY).addRequired(LootContextParameters.LAST_DAMAGE_PLAYER).addRequired(LootContextParameters.DAMAGE_SOURCE).addRequired(LootContextParameters.KILLER_ENTITY).addRequired(LootContextParameters.DIRECT_KILLER_ENTITY).addRequired(LootContextParameters.ORIGIN).addRequired(LootContextParameters.BLOCK_STATE).addRequired(LootContextParameters.BLOCK_ENTITY).addRequired(LootContextParameters.TOOL).addRequired(LootContextParameters.EXPLOSION_RADIUS);
    });
    public static final LootContextParameterSet BLOCK = register("block", (builder) -> {
        builder.addRequired(LootContextParameters.BLOCK_STATE).addRequired(LootContextParameters.ORIGIN).addRequired(LootContextParameters.TOOL).addOptional(LootContextParameters.THIS_ENTITY).addOptional(LootContextParameters.BLOCK_ENTITY).addOptional(LootContextParameters.EXPLOSION_RADIUS);
    });

    private static LootContextParameterSet register(String name, Consumer<LootContextParameterSet.Builder> type) {
        LootContextParameterSet.Builder builder = new LootContextParameterSet.Builder();
        type.accept(builder);
        LootContextParameterSet lootContextParamSet = builder.build();
        MinecraftKey resourceLocation = new MinecraftKey(name);
        LootContextParameterSet lootContextParamSet2 = REGISTRY.put(resourceLocation, lootContextParamSet);
        if (lootContextParamSet2 != null) {
            throw new IllegalStateException("Loot table parameter set " + resourceLocation + " is already registered");
        } else {
            return lootContextParamSet;
        }
    }

    @Nullable
    public static LootContextParameterSet get(MinecraftKey id) {
        return REGISTRY.get(id);
    }

    @Nullable
    public static MinecraftKey getKey(LootContextParameterSet type) {
        return REGISTRY.inverse().get(type);
    }
}
