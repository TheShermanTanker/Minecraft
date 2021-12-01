package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.boss.wither.EntityWither;
import net.minecraft.world.entity.monster.EntitySkeletonWither;
import net.minecraft.world.entity.monster.hoglin.EntityHoglin;
import net.minecraft.world.entity.monster.piglin.EntityPiglin;
import net.minecraft.world.entity.monster.piglin.EntityPiglinAbstract;
import net.minecraft.world.entity.monster.piglin.EntityPiglinBrute;
import net.minecraft.world.entity.monster.piglin.PiglinAI;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.block.BlockCampfire;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class SensorPiglinSpecific extends Sensor<EntityLiving> {
    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_NEMESIS, MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD, MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM, MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN, MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN, MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS, MemoryModuleType.NEARBY_ADULT_PIGLINS, MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, MemoryModuleType.NEAREST_REPELLENT);
    }

    @Override
    protected void doTick(WorldServer world, EntityLiving entity) {
        BehaviorController<?> brain = entity.getBehaviorController();
        brain.setMemory(MemoryModuleType.NEAREST_REPELLENT, findNearestRepellent(world, entity));
        Optional<EntityInsentient> optional = Optional.empty();
        Optional<EntityHoglin> optional2 = Optional.empty();
        Optional<EntityHoglin> optional3 = Optional.empty();
        Optional<EntityPiglin> optional4 = Optional.empty();
        Optional<EntityLiving> optional5 = Optional.empty();
        Optional<EntityHuman> optional6 = Optional.empty();
        Optional<EntityHuman> optional7 = Optional.empty();
        int i = 0;
        List<EntityPiglinAbstract> list = Lists.newArrayList();
        List<EntityPiglinAbstract> list2 = Lists.newArrayList();
        NearestVisibleLivingEntities nearestVisibleLivingEntities = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).orElse(NearestVisibleLivingEntities.empty());

        for(EntityLiving livingEntity : nearestVisibleLivingEntities.findAll((livingEntityx) -> {
            return true;
        })) {
            if (livingEntity instanceof EntityHoglin) {
                EntityHoglin hoglin = (EntityHoglin)livingEntity;
                if (hoglin.isBaby() && optional3.isEmpty()) {
                    optional3 = Optional.of(hoglin);
                } else if (hoglin.isAdult()) {
                    ++i;
                    if (optional2.isEmpty() && hoglin.canBeHunted()) {
                        optional2 = Optional.of(hoglin);
                    }
                }
            } else if (livingEntity instanceof EntityPiglinBrute) {
                EntityPiglinBrute piglinBrute = (EntityPiglinBrute)livingEntity;
                list.add(piglinBrute);
            } else if (livingEntity instanceof EntityPiglin) {
                EntityPiglin piglin = (EntityPiglin)livingEntity;
                if (piglin.isBaby() && optional4.isEmpty()) {
                    optional4 = Optional.of(piglin);
                } else if (piglin.isAdult()) {
                    list.add(piglin);
                }
            } else if (livingEntity instanceof EntityHuman) {
                EntityHuman player = (EntityHuman)livingEntity;
                if (optional6.isEmpty() && !PiglinAI.isWearingGold(player) && entity.canAttack(livingEntity)) {
                    optional6 = Optional.of(player);
                }

                if (optional7.isEmpty() && !player.isSpectator() && PiglinAI.isPlayerHoldingLovedItem(player)) {
                    optional7 = Optional.of(player);
                }
            } else if (!optional.isEmpty() || !(livingEntity instanceof EntitySkeletonWither) && !(livingEntity instanceof EntityWither)) {
                if (optional5.isEmpty() && PiglinAI.isZombified(livingEntity.getEntityType())) {
                    optional5 = Optional.of(livingEntity);
                }
            } else {
                optional = Optional.of((EntityInsentient)livingEntity);
            }
        }

        for(EntityLiving livingEntity2 : brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).orElse(ImmutableList.of())) {
            if (livingEntity2 instanceof EntityPiglinAbstract) {
                EntityPiglinAbstract abstractPiglin = (EntityPiglinAbstract)livingEntity2;
                if (abstractPiglin.isAdult()) {
                    list2.add(abstractPiglin);
                }
            }
        }

        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS, optional);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN, optional2);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN, optional3);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, optional5);
        brain.setMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD, optional6);
        brain.setMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM, optional7);
        brain.setMemory(MemoryModuleType.NEARBY_ADULT_PIGLINS, list2);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS, list);
        brain.setMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, list.size());
        brain.setMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, i);
    }

    private static Optional<BlockPosition> findNearestRepellent(WorldServer world, EntityLiving entity) {
        return BlockPosition.findClosestMatch(entity.getChunkCoordinates(), 8, 4, (pos) -> {
            return isValidRepellent(world, pos);
        });
    }

    private static boolean isValidRepellent(WorldServer world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos);
        boolean bl = blockState.is(TagsBlock.PIGLIN_REPELLENTS);
        return bl && blockState.is(Blocks.SOUL_CAMPFIRE) ? BlockCampfire.isLitCampfire(blockState) : bl;
    }
}
