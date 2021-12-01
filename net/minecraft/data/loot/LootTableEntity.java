package net.minecraft.data.loot;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.advancements.critereon.CriterionConditionDamageSource;
import net.minecraft.advancements.critereon.CriterionConditionEntity;
import net.minecraft.advancements.critereon.CriterionConditionEntityFlags;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.TagsEntity;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumCreatureType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootSelector;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootSelectorEmpty;
import net.minecraft.world.level.storage.loot.entries.LootSelectorLootTable;
import net.minecraft.world.level.storage.loot.entries.LootSelectorTag;
import net.minecraft.world.level.storage.loot.functions.LootEnchantFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionSetCount;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionSmelt;
import net.minecraft.world.level.storage.loot.functions.SetPotionFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionDamageSourceProperties;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionEntityProperty;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionKilledByPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionRandomChance;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionRandomChanceWithLooting;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class LootTableEntity implements Consumer<BiConsumer<MinecraftKey, LootTable.Builder>> {
    private static final CriterionConditionEntity.Builder ENTITY_ON_FIRE = CriterionConditionEntity.Builder.entity().flags(CriterionConditionEntityFlags.Builder.flags().setOnFire(true).build());
    private static final Set<EntityTypes<?>> SPECIAL_LOOT_TABLE_TYPES = ImmutableSet.of(EntityTypes.PLAYER, EntityTypes.ARMOR_STAND, EntityTypes.IRON_GOLEM, EntityTypes.SNOW_GOLEM, EntityTypes.VILLAGER);
    private final Map<MinecraftKey, LootTable.Builder> map = Maps.newHashMap();

    private static LootTable.Builder createSheepTable(IMaterial item) {
        return LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(item))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootSelectorLootTable.lootTableReference(EntityTypes.SHEEP.getDefaultLootTable())));
    }

    @Override
    public void accept(BiConsumer<MinecraftKey, LootTable.Builder> biConsumer) {
        this.add(EntityTypes.ARMOR_STAND, LootTable.lootTable());
        this.add(EntityTypes.AXOLOTL, LootTable.lootTable());
        this.add(EntityTypes.BAT, LootTable.lootTable());
        this.add(EntityTypes.BEE, LootTable.lootTable());
        this.add(EntityTypes.BLAZE, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.BLAZE_ROD).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 1.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))).when(LootItemConditionKilledByPlayer.killedByPlayer())));
        this.add(EntityTypes.CAT, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.STRING).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))))));
        this.add(EntityTypes.CAVE_SPIDER, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.STRING).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.SPIDER_EYE).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(-1.0F, 1.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))).when(LootItemConditionKilledByPlayer.killedByPlayer())));
        this.add(EntityTypes.CHICKEN, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.FEATHER).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.CHICKEN).apply(LootItemFunctionSmelt.smelted().when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, ENTITY_ON_FIRE))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.COD, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.COD).apply(LootItemFunctionSmelt.smelted().when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, ENTITY_ON_FIRE))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.BONE_MEAL)).when(LootItemConditionRandomChance.randomChance(0.05F))));
        this.add(EntityTypes.COW, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.LEATHER).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.BEEF).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(1.0F, 3.0F))).apply(LootItemFunctionSmelt.smelted().when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, ENTITY_ON_FIRE))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.CREEPER, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.GUNPOWDER).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().add(LootSelectorTag.expandTag(TagsItem.CREEPER_DROP_MUSIC_DISCS)).when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.KILLER, CriterionConditionEntity.Builder.entity().of(TagsEntity.SKELETONS)))));
        this.add(EntityTypes.DOLPHIN, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.COD).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 1.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))).apply(LootItemFunctionSmelt.smelted().when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, ENTITY_ON_FIRE))))));
        this.add(EntityTypes.DONKEY, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.LEATHER).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.DROWNED, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.ROTTEN_FLESH).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.COPPER_INGOT)).when(LootItemConditionKilledByPlayer.killedByPlayer()).when(LootItemConditionRandomChanceWithLooting.randomChanceAndLootingBoost(0.11F, 0.02F))));
        this.add(EntityTypes.ELDER_GUARDIAN, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.PRISMARINE_SHARD).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.COD).setWeight(3).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))).apply(LootItemFunctionSmelt.smelted().when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, ENTITY_ON_FIRE)))).add(LootItem.lootTableItem(Items.PRISMARINE_CRYSTALS).setWeight(2).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))).add(LootSelectorEmpty.emptyItem())).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Blocks.WET_SPONGE)).when(LootItemConditionKilledByPlayer.killedByPlayer())).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootSelectorLootTable.lootTableReference(LootTables.FISHING_FISH).apply(LootItemFunctionSmelt.smelted().when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, ENTITY_ON_FIRE)))).when(LootItemConditionKilledByPlayer.killedByPlayer()).when(LootItemConditionRandomChanceWithLooting.randomChanceAndLootingBoost(0.025F, 0.01F))));
        this.add(EntityTypes.ENDER_DRAGON, LootTable.lootTable());
        this.add(EntityTypes.ENDERMAN, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.ENDER_PEARL).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 1.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.ENDERMITE, LootTable.lootTable());
        this.add(EntityTypes.EVOKER, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.TOTEM_OF_UNDYING))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.EMERALD).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 1.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))).when(LootItemConditionKilledByPlayer.killedByPlayer())));
        this.add(EntityTypes.FOX, LootTable.lootTable());
        this.add(EntityTypes.GHAST, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.GHAST_TEAR).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 1.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.GUNPOWDER).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.GIANT, LootTable.lootTable());
        this.add(EntityTypes.GLOW_SQUID, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.GLOW_INK_SAC).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(1.0F, 3.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.GOAT, LootTable.lootTable());
        this.add(EntityTypes.GUARDIAN, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.PRISMARINE_SHARD).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.COD).setWeight(2).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))).apply(LootItemFunctionSmelt.smelted().when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, ENTITY_ON_FIRE)))).add(LootItem.lootTableItem(Items.PRISMARINE_CRYSTALS).setWeight(2).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))).add(LootSelectorEmpty.emptyItem())).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootSelectorLootTable.lootTableReference(LootTables.FISHING_FISH).apply(LootItemFunctionSmelt.smelted().when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, ENTITY_ON_FIRE)))).when(LootItemConditionKilledByPlayer.killedByPlayer()).when(LootItemConditionRandomChanceWithLooting.randomChanceAndLootingBoost(0.025F, 0.01F))));
        this.add(EntityTypes.HORSE, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.LEATHER).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.HUSK, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.ROTTEN_FLESH).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.IRON_INGOT)).add(LootItem.lootTableItem(Items.CARROT)).add(LootItem.lootTableItem(Items.POTATO).apply(LootItemFunctionSmelt.smelted().when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, ENTITY_ON_FIRE)))).when(LootItemConditionKilledByPlayer.killedByPlayer()).when(LootItemConditionRandomChanceWithLooting.randomChanceAndLootingBoost(0.025F, 0.01F))));
        this.add(EntityTypes.RAVAGER, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.SADDLE).apply(LootItemFunctionSetCount.setCount(ConstantValue.exactly(1.0F))))));
        this.add(EntityTypes.ILLUSIONER, LootTable.lootTable());
        this.add(EntityTypes.IRON_GOLEM, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Blocks.POPPY).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.IRON_INGOT).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(3.0F, 5.0F))))));
        this.add(EntityTypes.LLAMA, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.LEATHER).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.MAGMA_CUBE, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.MAGMA_CREAM).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(-2.0F, 1.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.MULE, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.LEATHER).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.MOOSHROOM, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.LEATHER).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.BEEF).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(1.0F, 3.0F))).apply(LootItemFunctionSmelt.smelted().when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, ENTITY_ON_FIRE))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.OCELOT, LootTable.lootTable());
        this.add(EntityTypes.PANDA, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Blocks.BAMBOO).apply(LootItemFunctionSetCount.setCount(ConstantValue.exactly(1.0F))))));
        this.add(EntityTypes.PARROT, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.FEATHER).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(1.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.PHANTOM, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.PHANTOM_MEMBRANE).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 1.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))).when(LootItemConditionKilledByPlayer.killedByPlayer())));
        this.add(EntityTypes.PIG, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.PORKCHOP).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(1.0F, 3.0F))).apply(LootItemFunctionSmelt.smelted().when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, ENTITY_ON_FIRE))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.PILLAGER, LootTable.lootTable());
        this.add(EntityTypes.PLAYER, LootTable.lootTable());
        this.add(EntityTypes.POLAR_BEAR, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.COD).apply(LootItemFunctionSmelt.smelted().when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, ENTITY_ON_FIRE))).setWeight(3).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))).add(LootItem.lootTableItem(Items.SALMON).apply(LootItemFunctionSmelt.smelted().when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, ENTITY_ON_FIRE))).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.PUFFERFISH, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.PUFFERFISH).apply(LootItemFunctionSetCount.setCount(ConstantValue.exactly(1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.BONE_MEAL)).when(LootItemConditionRandomChance.randomChance(0.05F))));
        this.add(EntityTypes.RABBIT, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.RABBIT_HIDE).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 1.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.RABBIT).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 1.0F))).apply(LootItemFunctionSmelt.smelted().when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, ENTITY_ON_FIRE))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.RABBIT_FOOT)).when(LootItemConditionKilledByPlayer.killedByPlayer()).when(LootItemConditionRandomChanceWithLooting.randomChanceAndLootingBoost(0.1F, 0.03F))));
        this.add(EntityTypes.SALMON, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.SALMON).apply(LootItemFunctionSmelt.smelted().when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, ENTITY_ON_FIRE))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.BONE_MEAL)).when(LootItemConditionRandomChance.randomChance(0.05F))));
        this.add(EntityTypes.SHEEP, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.MUTTON).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(1.0F, 2.0F))).apply(LootItemFunctionSmelt.smelted().when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, ENTITY_ON_FIRE))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(LootTables.SHEEP_BLACK, createSheepTable(Blocks.BLACK_WOOL));
        this.add(LootTables.SHEEP_BLUE, createSheepTable(Blocks.BLUE_WOOL));
        this.add(LootTables.SHEEP_BROWN, createSheepTable(Blocks.BROWN_WOOL));
        this.add(LootTables.SHEEP_CYAN, createSheepTable(Blocks.CYAN_WOOL));
        this.add(LootTables.SHEEP_GRAY, createSheepTable(Blocks.GRAY_WOOL));
        this.add(LootTables.SHEEP_GREEN, createSheepTable(Blocks.GREEN_WOOL));
        this.add(LootTables.SHEEP_LIGHT_BLUE, createSheepTable(Blocks.LIGHT_BLUE_WOOL));
        this.add(LootTables.SHEEP_LIGHT_GRAY, createSheepTable(Blocks.LIGHT_GRAY_WOOL));
        this.add(LootTables.SHEEP_LIME, createSheepTable(Blocks.LIME_WOOL));
        this.add(LootTables.SHEEP_MAGENTA, createSheepTable(Blocks.MAGENTA_WOOL));
        this.add(LootTables.SHEEP_ORANGE, createSheepTable(Blocks.ORANGE_WOOL));
        this.add(LootTables.SHEEP_PINK, createSheepTable(Blocks.PINK_WOOL));
        this.add(LootTables.SHEEP_PURPLE, createSheepTable(Blocks.PURPLE_WOOL));
        this.add(LootTables.SHEEP_RED, createSheepTable(Blocks.RED_WOOL));
        this.add(LootTables.SHEEP_WHITE, createSheepTable(Blocks.WHITE_WOOL));
        this.add(LootTables.SHEEP_YELLOW, createSheepTable(Blocks.YELLOW_WOOL));
        this.add(EntityTypes.SHULKER, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.SHULKER_SHELL)).when(LootItemConditionRandomChanceWithLooting.randomChanceAndLootingBoost(0.5F, 0.0625F))));
        this.add(EntityTypes.SILVERFISH, LootTable.lootTable());
        this.add(EntityTypes.SKELETON, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.ARROW).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.BONE).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.SKELETON_HORSE, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.BONE).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.SLIME, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.SLIME_BALL).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.SNOW_GOLEM, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.SNOWBALL).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 15.0F))))));
        this.add(EntityTypes.SPIDER, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.STRING).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.SPIDER_EYE).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(-1.0F, 1.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))).when(LootItemConditionKilledByPlayer.killedByPlayer())));
        this.add(EntityTypes.SQUID, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.INK_SAC).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(1.0F, 3.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.STRAY, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.ARROW).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.BONE).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.TIPPED_ARROW).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 1.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)).setLimit(1)).apply(SetPotionFunction.setPotion(Potions.SLOWNESS))).when(LootItemConditionKilledByPlayer.killedByPlayer())));
        this.add(EntityTypes.STRIDER, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.STRING).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(2.0F, 5.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.TRADER_LLAMA, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.LEATHER).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.TROPICAL_FISH, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.TROPICAL_FISH).apply(LootItemFunctionSetCount.setCount(ConstantValue.exactly(1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.BONE_MEAL)).when(LootItemConditionRandomChance.randomChance(0.05F))));
        this.add(EntityTypes.TURTLE, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Blocks.SEAGRASS).setWeight(3).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.BOWL)).when(LootItemConditionDamageSourceProperties.hasDamageSource(CriterionConditionDamageSource.Builder.damageType().isLightning(true)))));
        this.add(EntityTypes.VEX, LootTable.lootTable());
        this.add(EntityTypes.VILLAGER, LootTable.lootTable());
        this.add(EntityTypes.WANDERING_TRADER, LootTable.lootTable());
        this.add(EntityTypes.VINDICATOR, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.EMERALD).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 1.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))).when(LootItemConditionKilledByPlayer.killedByPlayer())));
        this.add(EntityTypes.WITCH, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(UniformGenerator.between(1.0F, 3.0F)).add(LootItem.lootTableItem(Items.GLOWSTONE_DUST).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))).add(LootItem.lootTableItem(Items.SUGAR).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))).add(LootItem.lootTableItem(Items.REDSTONE).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))).add(LootItem.lootTableItem(Items.SPIDER_EYE).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))).add(LootItem.lootTableItem(Items.GLASS_BOTTLE).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))).add(LootItem.lootTableItem(Items.GUNPOWDER).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))).add(LootItem.lootTableItem(Items.STICK).setWeight(2).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.WITHER, LootTable.lootTable());
        this.add(EntityTypes.WITHER_SKELETON, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.COAL).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(-1.0F, 1.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.BONE).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Blocks.WITHER_SKELETON_SKULL)).when(LootItemConditionKilledByPlayer.killedByPlayer()).when(LootItemConditionRandomChanceWithLooting.randomChanceAndLootingBoost(0.025F, 0.01F))));
        this.add(EntityTypes.WOLF, LootTable.lootTable());
        this.add(EntityTypes.ZOGLIN, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.ROTTEN_FLESH).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(1.0F, 3.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.ZOMBIE, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.ROTTEN_FLESH).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.IRON_INGOT)).add(LootItem.lootTableItem(Items.CARROT)).add(LootItem.lootTableItem(Items.POTATO).apply(LootItemFunctionSmelt.smelted().when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, ENTITY_ON_FIRE)))).when(LootItemConditionKilledByPlayer.killedByPlayer()).when(LootItemConditionRandomChanceWithLooting.randomChanceAndLootingBoost(0.025F, 0.01F))));
        this.add(EntityTypes.ZOMBIE_HORSE, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.ROTTEN_FLESH).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.ZOMBIFIED_PIGLIN, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.ROTTEN_FLESH).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 1.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.GOLD_NUGGET).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 1.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.GOLD_INGOT)).when(LootItemConditionKilledByPlayer.killedByPlayer()).when(LootItemConditionRandomChanceWithLooting.randomChanceAndLootingBoost(0.025F, 0.01F))));
        this.add(EntityTypes.HOGLIN, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.PORKCHOP).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(2.0F, 4.0F))).apply(LootItemFunctionSmelt.smelted().when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, ENTITY_ON_FIRE))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.LEATHER).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 1.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))));
        this.add(EntityTypes.PIGLIN, LootTable.lootTable());
        this.add(EntityTypes.PIGLIN_BRUTE, LootTable.lootTable());
        this.add(EntityTypes.ZOMBIE_VILLAGER, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.ROTTEN_FLESH).apply(LootItemFunctionSetCount.setCount(UniformGenerator.between(0.0F, 2.0F))).apply(LootEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))).withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.IRON_INGOT)).add(LootItem.lootTableItem(Items.CARROT)).add(LootItem.lootTableItem(Items.POTATO).apply(LootItemFunctionSmelt.smelted().when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, ENTITY_ON_FIRE)))).when(LootItemConditionKilledByPlayer.killedByPlayer()).when(LootItemConditionRandomChanceWithLooting.randomChanceAndLootingBoost(0.025F, 0.01F))));
        Set<MinecraftKey> set = Sets.newHashSet();

        for(EntityTypes<?> entityType : IRegistry.ENTITY_TYPE) {
            MinecraftKey resourceLocation = entityType.getDefaultLootTable();
            if (!SPECIAL_LOOT_TABLE_TYPES.contains(entityType) && entityType.getCategory() == EnumCreatureType.MISC) {
                if (resourceLocation != LootTables.EMPTY && this.map.remove(resourceLocation) != null) {
                    throw new IllegalStateException(String.format("Weird loottable '%s' for '%s', not a LivingEntity so should not have loot", resourceLocation, IRegistry.ENTITY_TYPE.getKey(entityType)));
                }
            } else if (resourceLocation != LootTables.EMPTY && set.add(resourceLocation)) {
                LootTable.Builder builder = this.map.remove(resourceLocation);
                if (builder == null) {
                    throw new IllegalStateException(String.format("Missing loottable '%s' for '%s'", resourceLocation, IRegistry.ENTITY_TYPE.getKey(entityType)));
                }

                biConsumer.accept(resourceLocation, builder);
            }
        }

        this.map.forEach(biConsumer);
    }

    private void add(EntityTypes<?> entityType, LootTable.Builder lootTable) {
        this.add(entityType.getDefaultLootTable(), lootTable);
    }

    private void add(MinecraftKey entityId, LootTable.Builder lootTable) {
        this.map.put(entityId, lootTable);
    }
}
