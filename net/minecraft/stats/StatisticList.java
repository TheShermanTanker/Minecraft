package net.minecraft.stats;

import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class StatisticList {
    public static final StatisticWrapper<Block> BLOCK_MINED = makeRegistryStatType("mined", IRegistry.BLOCK);
    public static final StatisticWrapper<Item> ITEM_CRAFTED = makeRegistryStatType("crafted", IRegistry.ITEM);
    public static final StatisticWrapper<Item> ITEM_USED = makeRegistryStatType("used", IRegistry.ITEM);
    public static final StatisticWrapper<Item> ITEM_BROKEN = makeRegistryStatType("broken", IRegistry.ITEM);
    public static final StatisticWrapper<Item> ITEM_PICKED_UP = makeRegistryStatType("picked_up", IRegistry.ITEM);
    public static final StatisticWrapper<Item> ITEM_DROPPED = makeRegistryStatType("dropped", IRegistry.ITEM);
    public static final StatisticWrapper<EntityTypes<?>> ENTITY_KILLED = makeRegistryStatType("killed", IRegistry.ENTITY_TYPE);
    public static final StatisticWrapper<EntityTypes<?>> ENTITY_KILLED_BY = makeRegistryStatType("killed_by", IRegistry.ENTITY_TYPE);
    public static final StatisticWrapper<MinecraftKey> CUSTOM = makeRegistryStatType("custom", IRegistry.CUSTOM_STAT);
    public static final MinecraftKey LEAVE_GAME = makeCustomStat("leave_game", ICounter.DEFAULT);
    public static final MinecraftKey PLAY_TIME = makeCustomStat("play_time", ICounter.TIME);
    public static final MinecraftKey TOTAL_WORLD_TIME = makeCustomStat("total_world_time", ICounter.TIME);
    public static final MinecraftKey TIME_SINCE_DEATH = makeCustomStat("time_since_death", ICounter.TIME);
    public static final MinecraftKey TIME_SINCE_REST = makeCustomStat("time_since_rest", ICounter.TIME);
    public static final MinecraftKey CROUCH_TIME = makeCustomStat("sneak_time", ICounter.TIME);
    public static final MinecraftKey WALK_ONE_CM = makeCustomStat("walk_one_cm", ICounter.DISTANCE);
    public static final MinecraftKey CROUCH_ONE_CM = makeCustomStat("crouch_one_cm", ICounter.DISTANCE);
    public static final MinecraftKey SPRINT_ONE_CM = makeCustomStat("sprint_one_cm", ICounter.DISTANCE);
    public static final MinecraftKey WALK_ON_WATER_ONE_CM = makeCustomStat("walk_on_water_one_cm", ICounter.DISTANCE);
    public static final MinecraftKey FALL_ONE_CM = makeCustomStat("fall_one_cm", ICounter.DISTANCE);
    public static final MinecraftKey CLIMB_ONE_CM = makeCustomStat("climb_one_cm", ICounter.DISTANCE);
    public static final MinecraftKey FLY_ONE_CM = makeCustomStat("fly_one_cm", ICounter.DISTANCE);
    public static final MinecraftKey WALK_UNDER_WATER_ONE_CM = makeCustomStat("walk_under_water_one_cm", ICounter.DISTANCE);
    public static final MinecraftKey MINECART_ONE_CM = makeCustomStat("minecart_one_cm", ICounter.DISTANCE);
    public static final MinecraftKey BOAT_ONE_CM = makeCustomStat("boat_one_cm", ICounter.DISTANCE);
    public static final MinecraftKey PIG_ONE_CM = makeCustomStat("pig_one_cm", ICounter.DISTANCE);
    public static final MinecraftKey HORSE_ONE_CM = makeCustomStat("horse_one_cm", ICounter.DISTANCE);
    public static final MinecraftKey AVIATE_ONE_CM = makeCustomStat("aviate_one_cm", ICounter.DISTANCE);
    public static final MinecraftKey SWIM_ONE_CM = makeCustomStat("swim_one_cm", ICounter.DISTANCE);
    public static final MinecraftKey STRIDER_ONE_CM = makeCustomStat("strider_one_cm", ICounter.DISTANCE);
    public static final MinecraftKey JUMP = makeCustomStat("jump", ICounter.DEFAULT);
    public static final MinecraftKey DROP = makeCustomStat("drop", ICounter.DEFAULT);
    public static final MinecraftKey DAMAGE_DEALT = makeCustomStat("damage_dealt", ICounter.DIVIDE_BY_TEN);
    public static final MinecraftKey DAMAGE_DEALT_ABSORBED = makeCustomStat("damage_dealt_absorbed", ICounter.DIVIDE_BY_TEN);
    public static final MinecraftKey DAMAGE_DEALT_RESISTED = makeCustomStat("damage_dealt_resisted", ICounter.DIVIDE_BY_TEN);
    public static final MinecraftKey DAMAGE_TAKEN = makeCustomStat("damage_taken", ICounter.DIVIDE_BY_TEN);
    public static final MinecraftKey DAMAGE_BLOCKED_BY_SHIELD = makeCustomStat("damage_blocked_by_shield", ICounter.DIVIDE_BY_TEN);
    public static final MinecraftKey DAMAGE_ABSORBED = makeCustomStat("damage_absorbed", ICounter.DIVIDE_BY_TEN);
    public static final MinecraftKey DAMAGE_RESISTED = makeCustomStat("damage_resisted", ICounter.DIVIDE_BY_TEN);
    public static final MinecraftKey DEATHS = makeCustomStat("deaths", ICounter.DEFAULT);
    public static final MinecraftKey MOB_KILLS = makeCustomStat("mob_kills", ICounter.DEFAULT);
    public static final MinecraftKey ANIMALS_BRED = makeCustomStat("animals_bred", ICounter.DEFAULT);
    public static final MinecraftKey PLAYER_KILLS = makeCustomStat("player_kills", ICounter.DEFAULT);
    public static final MinecraftKey FISH_CAUGHT = makeCustomStat("fish_caught", ICounter.DEFAULT);
    public static final MinecraftKey TALKED_TO_VILLAGER = makeCustomStat("talked_to_villager", ICounter.DEFAULT);
    public static final MinecraftKey TRADED_WITH_VILLAGER = makeCustomStat("traded_with_villager", ICounter.DEFAULT);
    public static final MinecraftKey EAT_CAKE_SLICE = makeCustomStat("eat_cake_slice", ICounter.DEFAULT);
    public static final MinecraftKey FILL_CAULDRON = makeCustomStat("fill_cauldron", ICounter.DEFAULT);
    public static final MinecraftKey USE_CAULDRON = makeCustomStat("use_cauldron", ICounter.DEFAULT);
    public static final MinecraftKey CLEAN_ARMOR = makeCustomStat("clean_armor", ICounter.DEFAULT);
    public static final MinecraftKey CLEAN_BANNER = makeCustomStat("clean_banner", ICounter.DEFAULT);
    public static final MinecraftKey CLEAN_SHULKER_BOX = makeCustomStat("clean_shulker_box", ICounter.DEFAULT);
    public static final MinecraftKey INTERACT_WITH_BREWINGSTAND = makeCustomStat("interact_with_brewingstand", ICounter.DEFAULT);
    public static final MinecraftKey INTERACT_WITH_BEACON = makeCustomStat("interact_with_beacon", ICounter.DEFAULT);
    public static final MinecraftKey INSPECT_DROPPER = makeCustomStat("inspect_dropper", ICounter.DEFAULT);
    public static final MinecraftKey INSPECT_HOPPER = makeCustomStat("inspect_hopper", ICounter.DEFAULT);
    public static final MinecraftKey INSPECT_DISPENSER = makeCustomStat("inspect_dispenser", ICounter.DEFAULT);
    public static final MinecraftKey PLAY_NOTEBLOCK = makeCustomStat("play_noteblock", ICounter.DEFAULT);
    public static final MinecraftKey TUNE_NOTEBLOCK = makeCustomStat("tune_noteblock", ICounter.DEFAULT);
    public static final MinecraftKey POT_FLOWER = makeCustomStat("pot_flower", ICounter.DEFAULT);
    public static final MinecraftKey TRIGGER_TRAPPED_CHEST = makeCustomStat("trigger_trapped_chest", ICounter.DEFAULT);
    public static final MinecraftKey OPEN_ENDERCHEST = makeCustomStat("open_enderchest", ICounter.DEFAULT);
    public static final MinecraftKey ENCHANT_ITEM = makeCustomStat("enchant_item", ICounter.DEFAULT);
    public static final MinecraftKey PLAY_RECORD = makeCustomStat("play_record", ICounter.DEFAULT);
    public static final MinecraftKey INTERACT_WITH_FURNACE = makeCustomStat("interact_with_furnace", ICounter.DEFAULT);
    public static final MinecraftKey INTERACT_WITH_CRAFTING_TABLE = makeCustomStat("interact_with_crafting_table", ICounter.DEFAULT);
    public static final MinecraftKey OPEN_CHEST = makeCustomStat("open_chest", ICounter.DEFAULT);
    public static final MinecraftKey SLEEP_IN_BED = makeCustomStat("sleep_in_bed", ICounter.DEFAULT);
    public static final MinecraftKey OPEN_SHULKER_BOX = makeCustomStat("open_shulker_box", ICounter.DEFAULT);
    public static final MinecraftKey OPEN_BARREL = makeCustomStat("open_barrel", ICounter.DEFAULT);
    public static final MinecraftKey INTERACT_WITH_BLAST_FURNACE = makeCustomStat("interact_with_blast_furnace", ICounter.DEFAULT);
    public static final MinecraftKey INTERACT_WITH_SMOKER = makeCustomStat("interact_with_smoker", ICounter.DEFAULT);
    public static final MinecraftKey INTERACT_WITH_LECTERN = makeCustomStat("interact_with_lectern", ICounter.DEFAULT);
    public static final MinecraftKey INTERACT_WITH_CAMPFIRE = makeCustomStat("interact_with_campfire", ICounter.DEFAULT);
    public static final MinecraftKey INTERACT_WITH_CARTOGRAPHY_TABLE = makeCustomStat("interact_with_cartography_table", ICounter.DEFAULT);
    public static final MinecraftKey INTERACT_WITH_LOOM = makeCustomStat("interact_with_loom", ICounter.DEFAULT);
    public static final MinecraftKey INTERACT_WITH_STONECUTTER = makeCustomStat("interact_with_stonecutter", ICounter.DEFAULT);
    public static final MinecraftKey BELL_RING = makeCustomStat("bell_ring", ICounter.DEFAULT);
    public static final MinecraftKey RAID_TRIGGER = makeCustomStat("raid_trigger", ICounter.DEFAULT);
    public static final MinecraftKey RAID_WIN = makeCustomStat("raid_win", ICounter.DEFAULT);
    public static final MinecraftKey INTERACT_WITH_ANVIL = makeCustomStat("interact_with_anvil", ICounter.DEFAULT);
    public static final MinecraftKey INTERACT_WITH_GRINDSTONE = makeCustomStat("interact_with_grindstone", ICounter.DEFAULT);
    public static final MinecraftKey TARGET_HIT = makeCustomStat("target_hit", ICounter.DEFAULT);
    public static final MinecraftKey INTERACT_WITH_SMITHING_TABLE = makeCustomStat("interact_with_smithing_table", ICounter.DEFAULT);

    private static MinecraftKey makeCustomStat(String id, ICounter formatter) {
        MinecraftKey resourceLocation = new MinecraftKey(id);
        IRegistry.register(IRegistry.CUSTOM_STAT, id, resourceLocation);
        CUSTOM.get(resourceLocation, formatter);
        return resourceLocation;
    }

    private static <T> StatisticWrapper<T> makeRegistryStatType(String id, IRegistry<T> registry) {
        return IRegistry.register(IRegistry.STAT_TYPE, id, new StatisticWrapper<>(registry));
    }
}
