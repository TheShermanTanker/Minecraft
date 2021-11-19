package net.minecraft.data.advancements;

import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementFrameType;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.critereon.CriterionConditionBlock;
import net.minecraft.advancements.critereon.CriterionConditionEnchantments;
import net.minecraft.advancements.critereon.CriterionConditionEntity;
import net.minecraft.advancements.critereon.CriterionConditionItem;
import net.minecraft.advancements.critereon.CriterionConditionLocation;
import net.minecraft.advancements.critereon.CriterionConditionValue;
import net.minecraft.advancements.critereon.CriterionTriggerBeeNestDestroyed;
import net.minecraft.advancements.critereon.CriterionTriggerBredAnimals;
import net.minecraft.advancements.critereon.CriterionTriggerConsumeItem;
import net.minecraft.advancements.critereon.CriterionTriggerEffectsChanged;
import net.minecraft.advancements.critereon.CriterionTriggerFilledBucket;
import net.minecraft.advancements.critereon.CriterionTriggerFishingRodHooked;
import net.minecraft.advancements.critereon.CriterionTriggerInteractBlock;
import net.minecraft.advancements.critereon.CriterionTriggerInventoryChanged;
import net.minecraft.advancements.critereon.CriterionTriggerPlacedBlock;
import net.minecraft.advancements.critereon.CriterionTriggerStartRiding;
import net.minecraft.advancements.critereon.CriterionTriggerTamedAnimal;
import net.minecraft.core.IRegistry;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.EntityCat;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;

public class AdvancementsHusbandry implements Consumer<Consumer<Advancement>> {
    private static final EntityTypes<?>[] BREEDABLE_ANIMALS = new EntityTypes[]{EntityTypes.HORSE, EntityTypes.DONKEY, EntityTypes.MULE, EntityTypes.SHEEP, EntityTypes.COW, EntityTypes.MOOSHROOM, EntityTypes.PIG, EntityTypes.CHICKEN, EntityTypes.WOLF, EntityTypes.OCELOT, EntityTypes.RABBIT, EntityTypes.LLAMA, EntityTypes.CAT, EntityTypes.PANDA, EntityTypes.FOX, EntityTypes.BEE, EntityTypes.HOGLIN, EntityTypes.STRIDER, EntityTypes.GOAT, EntityTypes.AXOLOTL};
    private static final Item[] FISH = new Item[]{Items.COD, Items.TROPICAL_FISH, Items.PUFFERFISH, Items.SALMON};
    private static final Item[] FISH_BUCKETS = new Item[]{Items.COD_BUCKET, Items.TROPICAL_FISH_BUCKET, Items.PUFFERFISH_BUCKET, Items.SALMON_BUCKET};
    private static final Item[] EDIBLE_ITEMS = new Item[]{Items.APPLE, Items.MUSHROOM_STEW, Items.BREAD, Items.PORKCHOP, Items.COOKED_PORKCHOP, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH, Items.COOKED_COD, Items.COOKED_SALMON, Items.COOKIE, Items.MELON_SLICE, Items.BEEF, Items.COOKED_BEEF, Items.CHICKEN, Items.COOKED_CHICKEN, Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.CARROT, Items.POTATO, Items.BAKED_POTATO, Items.POISONOUS_POTATO, Items.GOLDEN_CARROT, Items.PUMPKIN_PIE, Items.RABBIT, Items.COOKED_RABBIT, Items.RABBIT_STEW, Items.MUTTON, Items.COOKED_MUTTON, Items.CHORUS_FRUIT, Items.BEETROOT, Items.BEETROOT_SOUP, Items.DRIED_KELP, Items.SUSPICIOUS_STEW, Items.SWEET_BERRIES, Items.HONEY_BOTTLE, Items.GLOW_BERRIES};
    private static final Item[] WAX_SCRAPING_TOOLS = new Item[]{Items.WOODEN_AXE, Items.GOLDEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE};

    @Override
    public void accept(Consumer<Advancement> consumer) {
        Advancement advancement = Advancement.SerializedAdvancement.advancement().display(Blocks.HAY_BLOCK, new ChatMessage("advancements.husbandry.root.title"), new ChatMessage("advancements.husbandry.root.description"), new MinecraftKey("textures/gui/advancements/backgrounds/husbandry.png"), AdvancementFrameType.TASK, false, false, false).addCriterion("consumed_item", CriterionTriggerConsumeItem.CriterionInstanceTrigger.usedItem()).save(consumer, "husbandry/root");
        Advancement advancement2 = Advancement.SerializedAdvancement.advancement().parent(advancement).display(Items.WHEAT, new ChatMessage("advancements.husbandry.plant_seed.title"), new ChatMessage("advancements.husbandry.plant_seed.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).requirements(AdvancementRequirements.OR).addCriterion("wheat", CriterionTriggerPlacedBlock.CriterionInstanceTrigger.placedBlock(Blocks.WHEAT)).addCriterion("pumpkin_stem", CriterionTriggerPlacedBlock.CriterionInstanceTrigger.placedBlock(Blocks.PUMPKIN_STEM)).addCriterion("melon_stem", CriterionTriggerPlacedBlock.CriterionInstanceTrigger.placedBlock(Blocks.MELON_STEM)).addCriterion("beetroots", CriterionTriggerPlacedBlock.CriterionInstanceTrigger.placedBlock(Blocks.BEETROOTS)).addCriterion("nether_wart", CriterionTriggerPlacedBlock.CriterionInstanceTrigger.placedBlock(Blocks.NETHER_WART)).save(consumer, "husbandry/plant_seed");
        Advancement advancement3 = Advancement.SerializedAdvancement.advancement().parent(advancement).display(Items.WHEAT, new ChatMessage("advancements.husbandry.breed_an_animal.title"), new ChatMessage("advancements.husbandry.breed_an_animal.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).requirements(AdvancementRequirements.OR).addCriterion("bred", CriterionTriggerBredAnimals.CriterionInstanceTrigger.bredAnimals()).save(consumer, "husbandry/breed_an_animal");
        this.addFood(Advancement.SerializedAdvancement.advancement()).parent(advancement2).display(Items.APPLE, new ChatMessage("advancements.husbandry.balanced_diet.title"), new ChatMessage("advancements.husbandry.balanced_diet.description"), (MinecraftKey)null, AdvancementFrameType.CHALLENGE, true, true, false).rewards(AdvancementRewards.Builder.experience(100)).save(consumer, "husbandry/balanced_diet");
        Advancement.SerializedAdvancement.advancement().parent(advancement2).display(Items.NETHERITE_HOE, new ChatMessage("advancements.husbandry.netherite_hoe.title"), new ChatMessage("advancements.husbandry.netherite_hoe.description"), (MinecraftKey)null, AdvancementFrameType.CHALLENGE, true, true, false).rewards(AdvancementRewards.Builder.experience(100)).addCriterion("netherite_hoe", CriterionTriggerInventoryChanged.CriterionInstanceTrigger.hasItems(Items.NETHERITE_HOE)).save(consumer, "husbandry/obtain_netherite_hoe");
        Advancement advancement4 = Advancement.SerializedAdvancement.advancement().parent(advancement).display(Items.LEAD, new ChatMessage("advancements.husbandry.tame_an_animal.title"), new ChatMessage("advancements.husbandry.tame_an_animal.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("tamed_animal", CriterionTriggerTamedAnimal.CriterionInstanceTrigger.tamedAnimal()).save(consumer, "husbandry/tame_an_animal");
        this.addBreedable(Advancement.SerializedAdvancement.advancement()).parent(advancement3).display(Items.GOLDEN_CARROT, new ChatMessage("advancements.husbandry.breed_all_animals.title"), new ChatMessage("advancements.husbandry.breed_all_animals.description"), (MinecraftKey)null, AdvancementFrameType.CHALLENGE, true, true, false).rewards(AdvancementRewards.Builder.experience(100)).save(consumer, "husbandry/bred_all_animals");
        Advancement advancement5 = this.addFish(Advancement.SerializedAdvancement.advancement()).parent(advancement).requirements(AdvancementRequirements.OR).display(Items.FISHING_ROD, new ChatMessage("advancements.husbandry.fishy_business.title"), new ChatMessage("advancements.husbandry.fishy_business.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).save(consumer, "husbandry/fishy_business");
        Advancement advancement6 = this.addFishBuckets(Advancement.SerializedAdvancement.advancement()).parent(advancement5).requirements(AdvancementRequirements.OR).display(Items.PUFFERFISH_BUCKET, new ChatMessage("advancements.husbandry.tactical_fishing.title"), new ChatMessage("advancements.husbandry.tactical_fishing.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).save(consumer, "husbandry/tactical_fishing");
        Advancement advancement7 = Advancement.SerializedAdvancement.advancement().parent(advancement6).requirements(AdvancementRequirements.OR).addCriterion(IRegistry.ITEM.getKey(Items.AXOLOTL_BUCKET).getKey(), CriterionTriggerFilledBucket.CriterionInstanceTrigger.filledBucket(CriterionConditionItem.Builder.item().of(Items.AXOLOTL_BUCKET).build())).display(Items.AXOLOTL_BUCKET, new ChatMessage("advancements.husbandry.axolotl_in_a_bucket.title"), new ChatMessage("advancements.husbandry.axolotl_in_a_bucket.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).save(consumer, "husbandry/axolotl_in_a_bucket");
        Advancement.SerializedAdvancement.advancement().parent(advancement7).addCriterion("kill_axolotl_target", CriterionTriggerEffectsChanged.CriterionInstanceTrigger.gotEffectsFrom(CriterionConditionEntity.Builder.entity().of(EntityTypes.AXOLOTL).build())).display(Items.TROPICAL_FISH_BUCKET, new ChatMessage("advancements.husbandry.kill_axolotl_target.title"), new ChatMessage("advancements.husbandry.kill_axolotl_target.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).save(consumer, "husbandry/kill_axolotl_target");
        this.addCatVariants(Advancement.SerializedAdvancement.advancement()).parent(advancement4).display(Items.COD, new ChatMessage("advancements.husbandry.complete_catalogue.title"), new ChatMessage("advancements.husbandry.complete_catalogue.description"), (MinecraftKey)null, AdvancementFrameType.CHALLENGE, true, true, false).rewards(AdvancementRewards.Builder.experience(50)).save(consumer, "husbandry/complete_catalogue");
        Advancement advancement8 = Advancement.SerializedAdvancement.advancement().parent(advancement).addCriterion("safely_harvest_honey", CriterionTriggerInteractBlock.CriterionInstanceTrigger.itemUsedOnBlock(CriterionConditionLocation.Builder.location().setBlock(CriterionConditionBlock.Builder.block().of(TagsBlock.BEEHIVES).build()).setSmokey(true), CriterionConditionItem.Builder.item().of(Items.GLASS_BOTTLE))).display(Items.HONEY_BOTTLE, new ChatMessage("advancements.husbandry.safely_harvest_honey.title"), new ChatMessage("advancements.husbandry.safely_harvest_honey.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).save(consumer, "husbandry/safely_harvest_honey");
        Advancement advancement9 = Advancement.SerializedAdvancement.advancement().parent(advancement8).display(Items.HONEYCOMB, new ChatMessage("advancements.husbandry.wax_on.title"), new ChatMessage("advancements.husbandry.wax_on.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("wax_on", CriterionTriggerInteractBlock.CriterionInstanceTrigger.itemUsedOnBlock(CriterionConditionLocation.Builder.location().setBlock(CriterionConditionBlock.Builder.block().of(HoneycombItem.WAXABLES.get().keySet()).build()), CriterionConditionItem.Builder.item().of(Items.HONEYCOMB))).save(consumer, "husbandry/wax_on");
        Advancement.SerializedAdvancement.advancement().parent(advancement9).display(Items.STONE_AXE, new ChatMessage("advancements.husbandry.wax_off.title"), new ChatMessage("advancements.husbandry.wax_off.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("wax_off", CriterionTriggerInteractBlock.CriterionInstanceTrigger.itemUsedOnBlock(CriterionConditionLocation.Builder.location().setBlock(CriterionConditionBlock.Builder.block().of(HoneycombItem.WAX_OFF_BY_BLOCK.get().keySet()).build()), CriterionConditionItem.Builder.item().of(WAX_SCRAPING_TOOLS))).save(consumer, "husbandry/wax_off");
        Advancement.SerializedAdvancement.advancement().parent(advancement).addCriterion("silk_touch_nest", CriterionTriggerBeeNestDestroyed.CriterionInstanceTrigger.destroyedBeeNest(Blocks.BEE_NEST, CriterionConditionItem.Builder.item().hasEnchantment(new CriterionConditionEnchantments(Enchantments.SILK_TOUCH, CriterionConditionValue.IntegerRange.atLeast(1))), CriterionConditionValue.IntegerRange.exactly(3))).display(Blocks.BEE_NEST, new ChatMessage("advancements.husbandry.silk_touch_nest.title"), new ChatMessage("advancements.husbandry.silk_touch_nest.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).save(consumer, "husbandry/silk_touch_nest");
        Advancement.SerializedAdvancement.advancement().parent(advancement).display(Items.OAK_BOAT, new ChatMessage("advancements.husbandry.ride_a_boat_with_a_goat.title"), new ChatMessage("advancements.husbandry.ride_a_boat_with_a_goat.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("ride_a_boat_with_a_goat", CriterionTriggerStartRiding.CriterionInstanceTrigger.playerStartsRiding(CriterionConditionEntity.Builder.entity().vehicle(CriterionConditionEntity.Builder.entity().of(EntityTypes.BOAT).passenger(CriterionConditionEntity.Builder.entity().of(EntityTypes.GOAT).build()).build()))).save(consumer, "husbandry/ride_a_boat_with_a_goat");
        Advancement.SerializedAdvancement.advancement().parent(advancement).display(Items.GLOW_INK_SAC, new ChatMessage("advancements.husbandry.make_a_sign_glow.title"), new ChatMessage("advancements.husbandry.make_a_sign_glow.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("make_a_sign_glow", CriterionTriggerInteractBlock.CriterionInstanceTrigger.itemUsedOnBlock(CriterionConditionLocation.Builder.location().setBlock(CriterionConditionBlock.Builder.block().of(TagsBlock.SIGNS).build()), CriterionConditionItem.Builder.item().of(Items.GLOW_INK_SAC))).save(consumer, "husbandry/make_a_sign_glow");
    }

    private Advancement.SerializedAdvancement addFood(Advancement.SerializedAdvancement task) {
        for(Item item : EDIBLE_ITEMS) {
            task.addCriterion(IRegistry.ITEM.getKey(item).getKey(), CriterionTriggerConsumeItem.CriterionInstanceTrigger.usedItem(item));
        }

        return task;
    }

    private Advancement.SerializedAdvancement addBreedable(Advancement.SerializedAdvancement task) {
        for(EntityTypes<?> entityType : BREEDABLE_ANIMALS) {
            task.addCriterion(EntityTypes.getName(entityType).toString(), CriterionTriggerBredAnimals.CriterionInstanceTrigger.bredAnimals(CriterionConditionEntity.Builder.entity().of(entityType)));
        }

        task.addCriterion(EntityTypes.getName(EntityTypes.TURTLE).toString(), CriterionTriggerBredAnimals.CriterionInstanceTrigger.bredAnimals(CriterionConditionEntity.Builder.entity().of(EntityTypes.TURTLE).build(), CriterionConditionEntity.Builder.entity().of(EntityTypes.TURTLE).build(), CriterionConditionEntity.ANY));
        return task;
    }

    private Advancement.SerializedAdvancement addFishBuckets(Advancement.SerializedAdvancement task) {
        for(Item item : FISH_BUCKETS) {
            task.addCriterion(IRegistry.ITEM.getKey(item).getKey(), CriterionTriggerFilledBucket.CriterionInstanceTrigger.filledBucket(CriterionConditionItem.Builder.item().of(item).build()));
        }

        return task;
    }

    private Advancement.SerializedAdvancement addFish(Advancement.SerializedAdvancement task) {
        for(Item item : FISH) {
            task.addCriterion(IRegistry.ITEM.getKey(item).getKey(), CriterionTriggerFishingRodHooked.CriterionInstanceTrigger.fishedItem(CriterionConditionItem.ANY, CriterionConditionEntity.ANY, CriterionConditionItem.Builder.item().of(item).build()));
        }

        return task;
    }

    private Advancement.SerializedAdvancement addCatVariants(Advancement.SerializedAdvancement task) {
        EntityCat.TEXTURE_BY_TYPE.forEach((integer, resourceLocation) -> {
            task.addCriterion(resourceLocation.getKey(), CriterionTriggerTamedAnimal.CriterionInstanceTrigger.tamedAnimal(CriterionConditionEntity.Builder.entity().of(resourceLocation).build()));
        });
        return task;
    }
}
