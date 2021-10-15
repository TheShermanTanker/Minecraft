package net.minecraft.advancements;

import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.CriterionSlideDownBlock;
import net.minecraft.advancements.critereon.CriterionTriggerBeeNestDestroyed;
import net.minecraft.advancements.critereon.CriterionTriggerBredAnimals;
import net.minecraft.advancements.critereon.CriterionTriggerBrewedPotion;
import net.minecraft.advancements.critereon.CriterionTriggerChangedDimension;
import net.minecraft.advancements.critereon.CriterionTriggerChanneledLightning;
import net.minecraft.advancements.critereon.CriterionTriggerConstructBeacon;
import net.minecraft.advancements.critereon.CriterionTriggerConsumeItem;
import net.minecraft.advancements.critereon.CriterionTriggerCuredZombieVillager;
import net.minecraft.advancements.critereon.CriterionTriggerEffectsChanged;
import net.minecraft.advancements.critereon.CriterionTriggerEnchantedItem;
import net.minecraft.advancements.critereon.CriterionTriggerEnterBlock;
import net.minecraft.advancements.critereon.CriterionTriggerEntityHurtPlayer;
import net.minecraft.advancements.critereon.CriterionTriggerFilledBucket;
import net.minecraft.advancements.critereon.CriterionTriggerFishingRodHooked;
import net.minecraft.advancements.critereon.CriterionTriggerImpossible;
import net.minecraft.advancements.critereon.CriterionTriggerInteractBlock;
import net.minecraft.advancements.critereon.CriterionTriggerInventoryChanged;
import net.minecraft.advancements.critereon.CriterionTriggerItemDurabilityChanged;
import net.minecraft.advancements.critereon.CriterionTriggerKilled;
import net.minecraft.advancements.critereon.CriterionTriggerKilledByCrossbow;
import net.minecraft.advancements.critereon.CriterionTriggerLevitation;
import net.minecraft.advancements.critereon.CriterionTriggerLocation;
import net.minecraft.advancements.critereon.CriterionTriggerNetherTravel;
import net.minecraft.advancements.critereon.CriterionTriggerPlacedBlock;
import net.minecraft.advancements.critereon.CriterionTriggerPlayerGeneratesContainerLoot;
import net.minecraft.advancements.critereon.CriterionTriggerPlayerHurtEntity;
import net.minecraft.advancements.critereon.CriterionTriggerPlayerInteractedWithEntity;
import net.minecraft.advancements.critereon.CriterionTriggerRecipeUnlocked;
import net.minecraft.advancements.critereon.CriterionTriggerShotCrossbow;
import net.minecraft.advancements.critereon.CriterionTriggerSummonedEntity;
import net.minecraft.advancements.critereon.CriterionTriggerTamedAnimal;
import net.minecraft.advancements.critereon.CriterionTriggerTargetHit;
import net.minecraft.advancements.critereon.CriterionTriggerThrownItemPickedUpByEntity;
import net.minecraft.advancements.critereon.CriterionTriggerTick;
import net.minecraft.advancements.critereon.CriterionTriggerUsedEnderEye;
import net.minecraft.advancements.critereon.CriterionTriggerUsedTotem;
import net.minecraft.advancements.critereon.CriterionTriggerVillagerTrade;
import net.minecraft.advancements.critereon.LightningStrikeTrigger;
import net.minecraft.advancements.critereon.StartRidingTrigger;
import net.minecraft.advancements.critereon.UsingItemTrigger;
import net.minecraft.resources.MinecraftKey;

public class CriterionTriggers {
    private static final Map<MinecraftKey, CriterionTrigger<?>> CRITERIA = Maps.newHashMap();
    public static final CriterionTriggerImpossible IMPOSSIBLE = register(new CriterionTriggerImpossible());
    public static final CriterionTriggerKilled PLAYER_KILLED_ENTITY = register(new CriterionTriggerKilled(new MinecraftKey("player_killed_entity")));
    public static final CriterionTriggerKilled ENTITY_KILLED_PLAYER = register(new CriterionTriggerKilled(new MinecraftKey("entity_killed_player")));
    public static final CriterionTriggerEnterBlock ENTER_BLOCK = register(new CriterionTriggerEnterBlock());
    public static final CriterionTriggerInventoryChanged INVENTORY_CHANGED = register(new CriterionTriggerInventoryChanged());
    public static final CriterionTriggerRecipeUnlocked RECIPE_UNLOCKED = register(new CriterionTriggerRecipeUnlocked());
    public static final CriterionTriggerPlayerHurtEntity PLAYER_HURT_ENTITY = register(new CriterionTriggerPlayerHurtEntity());
    public static final CriterionTriggerEntityHurtPlayer ENTITY_HURT_PLAYER = register(new CriterionTriggerEntityHurtPlayer());
    public static final CriterionTriggerEnchantedItem ENCHANTED_ITEM = register(new CriterionTriggerEnchantedItem());
    public static final CriterionTriggerFilledBucket FILLED_BUCKET = register(new CriterionTriggerFilledBucket());
    public static final CriterionTriggerBrewedPotion BREWED_POTION = register(new CriterionTriggerBrewedPotion());
    public static final CriterionTriggerConstructBeacon CONSTRUCT_BEACON = register(new CriterionTriggerConstructBeacon());
    public static final CriterionTriggerUsedEnderEye USED_ENDER_EYE = register(new CriterionTriggerUsedEnderEye());
    public static final CriterionTriggerSummonedEntity SUMMONED_ENTITY = register(new CriterionTriggerSummonedEntity());
    public static final CriterionTriggerBredAnimals BRED_ANIMALS = register(new CriterionTriggerBredAnimals());
    public static final CriterionTriggerLocation LOCATION = register(new CriterionTriggerLocation(new MinecraftKey("location")));
    public static final CriterionTriggerLocation SLEPT_IN_BED = register(new CriterionTriggerLocation(new MinecraftKey("slept_in_bed")));
    public static final CriterionTriggerCuredZombieVillager CURED_ZOMBIE_VILLAGER = register(new CriterionTriggerCuredZombieVillager());
    public static final CriterionTriggerVillagerTrade TRADE = register(new CriterionTriggerVillagerTrade());
    public static final CriterionTriggerItemDurabilityChanged ITEM_DURABILITY_CHANGED = register(new CriterionTriggerItemDurabilityChanged());
    public static final CriterionTriggerLevitation LEVITATION = register(new CriterionTriggerLevitation());
    public static final CriterionTriggerChangedDimension CHANGED_DIMENSION = register(new CriterionTriggerChangedDimension());
    public static final CriterionTriggerTick TICK = register(new CriterionTriggerTick());
    public static final CriterionTriggerTamedAnimal TAME_ANIMAL = register(new CriterionTriggerTamedAnimal());
    public static final CriterionTriggerPlacedBlock PLACED_BLOCK = register(new CriterionTriggerPlacedBlock());
    public static final CriterionTriggerConsumeItem CONSUME_ITEM = register(new CriterionTriggerConsumeItem());
    public static final CriterionTriggerEffectsChanged EFFECTS_CHANGED = register(new CriterionTriggerEffectsChanged());
    public static final CriterionTriggerUsedTotem USED_TOTEM = register(new CriterionTriggerUsedTotem());
    public static final CriterionTriggerNetherTravel NETHER_TRAVEL = register(new CriterionTriggerNetherTravel());
    public static final CriterionTriggerFishingRodHooked FISHING_ROD_HOOKED = register(new CriterionTriggerFishingRodHooked());
    public static final CriterionTriggerChanneledLightning CHANNELED_LIGHTNING = register(new CriterionTriggerChanneledLightning());
    public static final CriterionTriggerShotCrossbow SHOT_CROSSBOW = register(new CriterionTriggerShotCrossbow());
    public static final CriterionTriggerKilledByCrossbow KILLED_BY_CROSSBOW = register(new CriterionTriggerKilledByCrossbow());
    public static final CriterionTriggerLocation RAID_WIN = register(new CriterionTriggerLocation(new MinecraftKey("hero_of_the_village")));
    public static final CriterionTriggerLocation BAD_OMEN = register(new CriterionTriggerLocation(new MinecraftKey("voluntary_exile")));
    public static final CriterionSlideDownBlock HONEY_BLOCK_SLIDE = register(new CriterionSlideDownBlock());
    public static final CriterionTriggerBeeNestDestroyed BEE_NEST_DESTROYED = register(new CriterionTriggerBeeNestDestroyed());
    public static final CriterionTriggerTargetHit TARGET_BLOCK_HIT = register(new CriterionTriggerTargetHit());
    public static final CriterionTriggerInteractBlock ITEM_USED_ON_BLOCK = register(new CriterionTriggerInteractBlock());
    public static final CriterionTriggerPlayerGeneratesContainerLoot GENERATE_LOOT = register(new CriterionTriggerPlayerGeneratesContainerLoot());
    public static final CriterionTriggerThrownItemPickedUpByEntity ITEM_PICKED_UP_BY_ENTITY = register(new CriterionTriggerThrownItemPickedUpByEntity());
    public static final CriterionTriggerPlayerInteractedWithEntity PLAYER_INTERACTED_WITH_ENTITY = register(new CriterionTriggerPlayerInteractedWithEntity());
    public static final StartRidingTrigger START_RIDING_TRIGGER = register(new StartRidingTrigger());
    public static final LightningStrikeTrigger LIGHTNING_STRIKE = register(new LightningStrikeTrigger());
    public static final UsingItemTrigger USING_ITEM = register(new UsingItemTrigger());

    private static <T extends CriterionTrigger<?>> T register(T object) {
        if (CRITERIA.containsKey(object.getId())) {
            throw new IllegalArgumentException("Duplicate criterion id " + object.getId());
        } else {
            CRITERIA.put(object.getId(), object);
            return object;
        }
    }

    @Nullable
    public static <T extends CriterionInstance> CriterionTrigger<T> getCriterion(MinecraftKey id) {
        return CRITERIA.get(id);
    }

    public static Iterable<? extends CriterionTrigger<?>> all() {
        return CRITERIA.values();
    }
}
