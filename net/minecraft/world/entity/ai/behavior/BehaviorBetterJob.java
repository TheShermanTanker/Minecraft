package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.npc.VillagerProfession;

public class BehaviorBetterJob extends Behavior<EntityVillager> {
    final VillagerProfession profession;

    public BehaviorBetterJob(VillagerProfession profession) {
        super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
        this.profession = profession;
    }

    @Override
    protected void start(WorldServer world, EntityVillager entity, long time) {
        GlobalPos globalPos = entity.getBehaviorController().getMemory(MemoryModuleType.JOB_SITE).get();
        world.getPoiManager().getType(globalPos.getBlockPosition()).ifPresent((poiType) -> {
            BehaviorUtil.getNearbyVillagersWithCondition(entity, (villager) -> {
                return this.competesForSameJobsite(globalPos, poiType, villager);
            }).reduce(entity, BehaviorBetterJob::selectWinner);
        });
    }

    private static EntityVillager selectWinner(EntityVillager first, EntityVillager second) {
        EntityVillager villager;
        EntityVillager villager2;
        if (first.getExperience() > second.getExperience()) {
            villager = first;
            villager2 = second;
        } else {
            villager = second;
            villager2 = first;
        }

        villager2.getBehaviorController().removeMemory(MemoryModuleType.JOB_SITE);
        return villager;
    }

    private boolean competesForSameJobsite(GlobalPos pos, VillagePlaceType poiType, EntityVillager villager) {
        return this.hasJobSite(villager) && pos.equals(villager.getBehaviorController().getMemory(MemoryModuleType.JOB_SITE).get()) && this.hasMatchingProfession(poiType, villager.getVillagerData().getProfession());
    }

    private boolean hasMatchingProfession(VillagePlaceType poiType, VillagerProfession profession) {
        return profession.getJobPoiType().getPredicate().test(poiType);
    }

    private boolean hasJobSite(EntityVillager villager) {
        return villager.getBehaviorController().getMemory(MemoryModuleType.JOB_SITE).isPresent();
    }
}
