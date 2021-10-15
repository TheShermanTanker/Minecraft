package net.minecraft.data.advancements;

import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementFrameType;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.critereon.CriterionConditionDistance;
import net.minecraft.advancements.critereon.CriterionConditionEntity;
import net.minecraft.advancements.critereon.CriterionConditionLocation;
import net.minecraft.advancements.critereon.CriterionConditionValue;
import net.minecraft.advancements.critereon.CriterionTriggerChangedDimension;
import net.minecraft.advancements.critereon.CriterionTriggerEnterBlock;
import net.minecraft.advancements.critereon.CriterionTriggerInventoryChanged;
import net.minecraft.advancements.critereon.CriterionTriggerKilled;
import net.minecraft.advancements.critereon.CriterionTriggerLevitation;
import net.minecraft.advancements.critereon.CriterionTriggerLocation;
import net.minecraft.advancements.critereon.CriterionTriggerSummonedEntity;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;

public class TheEndAdvancements implements Consumer<Consumer<Advancement>> {
    @Override
    public void accept(Consumer<Advancement> consumer) {
        Advancement advancement = Advancement.SerializedAdvancement.advancement().display(Blocks.END_STONE, new ChatMessage("advancements.end.root.title"), new ChatMessage("advancements.end.root.description"), new MinecraftKey("textures/gui/advancements/backgrounds/end.png"), AdvancementFrameType.TASK, false, false, false).addCriterion("entered_end", CriterionTriggerChangedDimension.TriggerInstance.changedDimensionTo(World.END)).save(consumer, "end/root");
        Advancement advancement2 = Advancement.SerializedAdvancement.advancement().parent(advancement).display(Blocks.DRAGON_HEAD, new ChatMessage("advancements.end.kill_dragon.title"), new ChatMessage("advancements.end.kill_dragon.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("killed_dragon", CriterionTriggerKilled.TriggerInstance.playerKilledEntity(CriterionConditionEntity.Builder.entity().of(EntityTypes.ENDER_DRAGON))).save(consumer, "end/kill_dragon");
        Advancement advancement3 = Advancement.SerializedAdvancement.advancement().parent(advancement2).display(Items.ENDER_PEARL, new ChatMessage("advancements.end.enter_end_gateway.title"), new ChatMessage("advancements.end.enter_end_gateway.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("entered_end_gateway", CriterionTriggerEnterBlock.TriggerInstance.entersBlock(Blocks.END_GATEWAY)).save(consumer, "end/enter_end_gateway");
        Advancement.SerializedAdvancement.advancement().parent(advancement2).display(Items.END_CRYSTAL, new ChatMessage("advancements.end.respawn_dragon.title"), new ChatMessage("advancements.end.respawn_dragon.description"), (MinecraftKey)null, AdvancementFrameType.GOAL, true, true, false).addCriterion("summoned_dragon", CriterionTriggerSummonedEntity.TriggerInstance.summonedEntity(CriterionConditionEntity.Builder.entity().of(EntityTypes.ENDER_DRAGON))).save(consumer, "end/respawn_dragon");
        Advancement advancement4 = Advancement.SerializedAdvancement.advancement().parent(advancement3).display(Blocks.PURPUR_BLOCK, new ChatMessage("advancements.end.find_end_city.title"), new ChatMessage("advancements.end.find_end_city.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("in_city", CriterionTriggerLocation.TriggerInstance.located(CriterionConditionLocation.inFeature(StructureGenerator.END_CITY))).save(consumer, "end/find_end_city");
        Advancement.SerializedAdvancement.advancement().parent(advancement2).display(Items.DRAGON_BREATH, new ChatMessage("advancements.end.dragon_breath.title"), new ChatMessage("advancements.end.dragon_breath.description"), (MinecraftKey)null, AdvancementFrameType.GOAL, true, true, false).addCriterion("dragon_breath", CriterionTriggerInventoryChanged.TriggerInstance.hasItems(Items.DRAGON_BREATH)).save(consumer, "end/dragon_breath");
        Advancement.SerializedAdvancement.advancement().parent(advancement4).display(Items.SHULKER_SHELL, new ChatMessage("advancements.end.levitate.title"), new ChatMessage("advancements.end.levitate.description"), (MinecraftKey)null, AdvancementFrameType.CHALLENGE, true, true, false).rewards(AdvancementRewards.Builder.experience(50)).addCriterion("levitated", CriterionTriggerLevitation.TriggerInstance.levitated(CriterionConditionDistance.vertical(CriterionConditionValue.DoubleRange.atLeast(50.0D)))).save(consumer, "end/levitate");
        Advancement.SerializedAdvancement.advancement().parent(advancement4).display(Items.ELYTRA, new ChatMessage("advancements.end.elytra.title"), new ChatMessage("advancements.end.elytra.description"), (MinecraftKey)null, AdvancementFrameType.GOAL, true, true, false).addCriterion("elytra", CriterionTriggerInventoryChanged.TriggerInstance.hasItems(Items.ELYTRA)).save(consumer, "end/elytra");
        Advancement.SerializedAdvancement.advancement().parent(advancement2).display(Blocks.DRAGON_EGG, new ChatMessage("advancements.end.dragon_egg.title"), new ChatMessage("advancements.end.dragon_egg.description"), (MinecraftKey)null, AdvancementFrameType.GOAL, true, true, false).addCriterion("dragon_egg", CriterionTriggerInventoryChanged.TriggerInstance.hasItems(Blocks.DRAGON_EGG)).save(consumer, "end/dragon_egg");
    }
}
