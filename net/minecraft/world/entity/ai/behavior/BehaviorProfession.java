package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;

public class BehaviorProfession extends Behavior<EntityVillager> {
    public BehaviorProfession() {
        super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_ABSENT));
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityVillager entity) {
        VillagerData villagerData = entity.getVillagerData();
        return villagerData.getProfession() != VillagerProfession.NONE && villagerData.getProfession() != VillagerProfession.NITWIT && entity.getExperience() == 0 && villagerData.getLevel() <= 1;
    }

    @Override
    protected void start(WorldServer world, EntityVillager entity, long time) {
        entity.setVillagerData(entity.getVillagerData().withProfession(VillagerProfession.NONE));
        entity.refreshBrain(world);
    }
}
