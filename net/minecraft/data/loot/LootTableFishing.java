package net.minecraft.data.loot;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.advancements.critereon.CriterionConditionEntity;
import net.minecraft.advancements.critereon.CriterionConditionInOpenWater;
import net.minecraft.advancements.critereon.CriterionConditionLocation;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootSelector;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootSelectorLootTable;
import net.minecraft.world.level.storage.loot.functions.LootEnchantLevel;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionSetCount;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionSetDamage;
import net.minecraft.world.level.storage.loot.functions.SetPotionFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionEntityProperty;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionLocationCheck;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class LootTableFishing implements Consumer<BiConsumer<MinecraftKey, LootTable.Builder>> {
    public static final LootItemCondition.Builder IN_JUNGLE = LootItemConditionLocationCheck.checkLocation(CriterionConditionLocation.Builder.location().setBiome(Biomes.JUNGLE));
    public static final LootItemCondition.Builder IN_SPARSE_JUNGLE = LootItemConditionLocationCheck.checkLocation(CriterionConditionLocation.Builder.location().setBiome(Biomes.SPARSE_JUNGLE));
    public static final LootItemCondition.Builder IN_BAMBOO_JUNGLE = LootItemConditionLocationCheck.checkLocation(CriterionConditionLocation.Builder.location().setBiome(Biomes.BAMBOO_JUNGLE));

    @Override
    public void accept(BiConsumer<MinecraftKey, LootTable.Builder> biConsumer) {
        biConsumer.accept(LootTables.FISHING, LootTable.lootTable().withPool(LootSelector.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootSelectorLootTable.lootTableReference(LootTables.FISHING_JUNK).setWeight(10).setQuality(-2)).add(LootSelectorLootTable.lootTableReference(LootTables.FISHING_TREASURE).setWeight(5).setQuality(2).when(LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, CriterionConditionEntity.Builder.entity().fishingHook(CriterionConditionInOpenWater.inOpenWater(true))))).add(LootSelectorLootTable.lootTableReference(LootTables.FISHING_FISH).setWeight(85).setQuality(-1))));
        biConsumer.accept(LootTables.FISHING_FISH, LootTable.lootTable().withPool(LootSelector.lootPool().add(LootItem.lootTableItem(Items.COD).setWeight(60)).add(LootItem.lootTableItem(Items.SALMON).setWeight(25)).add(LootItem.lootTableItem(Items.TROPICAL_FISH).setWeight(2)).add(LootItem.lootTableItem(Items.PUFFERFISH).setWeight(13))));
        biConsumer.accept(LootTables.FISHING_JUNK, LootTable.lootTable().withPool(LootSelector.lootPool().add(LootItem.lootTableItem(Blocks.LILY_PAD).setWeight(17)).add(LootItem.lootTableItem(Items.LEATHER_BOOTS).setWeight(10).apply(LootItemFunctionSetDamage.setDamage(UniformGenerator.between(0.0F, 0.9F)))).add(LootItem.lootTableItem(Items.LEATHER).setWeight(10)).add(LootItem.lootTableItem(Items.BONE).setWeight(10)).add(LootItem.lootTableItem(Items.POTION).setWeight(10).apply(SetPotionFunction.setPotion(Potions.WATER))).add(LootItem.lootTableItem(Items.STRING).setWeight(5)).add(LootItem.lootTableItem(Items.FISHING_ROD).setWeight(2).apply(LootItemFunctionSetDamage.setDamage(UniformGenerator.between(0.0F, 0.9F)))).add(LootItem.lootTableItem(Items.BOWL).setWeight(10)).add(LootItem.lootTableItem(Items.STICK).setWeight(5)).add(LootItem.lootTableItem(Items.INK_SAC).setWeight(1).apply(LootItemFunctionSetCount.setCount(ConstantValue.exactly(10.0F)))).add(LootItem.lootTableItem(Blocks.TRIPWIRE_HOOK).setWeight(10)).add(LootItem.lootTableItem(Items.ROTTEN_FLESH).setWeight(10)).add(LootItem.lootTableItem(Blocks.BAMBOO).when(IN_JUNGLE.or(IN_SPARSE_JUNGLE).or(IN_BAMBOO_JUNGLE)).setWeight(10))));
        biConsumer.accept(LootTables.FISHING_TREASURE, LootTable.lootTable().withPool(LootSelector.lootPool().add(LootItem.lootTableItem(Items.NAME_TAG)).add(LootItem.lootTableItem(Items.SADDLE)).add(LootItem.lootTableItem(Items.BOW).apply(LootItemFunctionSetDamage.setDamage(UniformGenerator.between(0.0F, 0.25F))).apply(LootEnchantLevel.enchantWithLevels(ConstantValue.exactly(30.0F)).allowTreasure())).add(LootItem.lootTableItem(Items.FISHING_ROD).apply(LootItemFunctionSetDamage.setDamage(UniformGenerator.between(0.0F, 0.25F))).apply(LootEnchantLevel.enchantWithLevels(ConstantValue.exactly(30.0F)).allowTreasure())).add(LootItem.lootTableItem(Items.BOOK).apply(LootEnchantLevel.enchantWithLevels(ConstantValue.exactly(30.0F)).allowTreasure())).add(LootItem.lootTableItem(Items.NAUTILUS_SHELL))));
    }
}
