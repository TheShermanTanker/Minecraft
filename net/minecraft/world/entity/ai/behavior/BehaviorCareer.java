package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.IRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.npc.VillagerProfession;

public class BehaviorCareer extends Behavior<EntityVillager> {
    public BehaviorCareer() {
        super(ImmutableMap.of(MemoryModuleType.POTENTIAL_JOB_SITE, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityVillager entity) {
        BlockPosition blockPos = entity.getBehaviorController().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get().getBlockPosition();
        return blockPos.closerThan(entity.getPositionVector(), 2.0D) || entity.assignProfessionWhenSpawned();
    }

    @Override
    protected void start(WorldServer world, EntityVillager entity, long time) {
        GlobalPos globalPos = entity.getBehaviorController().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get();
        entity.getBehaviorController().removeMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
        entity.getBehaviorController().setMemory(MemoryModuleType.JOB_SITE, globalPos);
        world.broadcastEntityEffect(entity, (byte)14);
        if (entity.getVillagerData().getProfession() == VillagerProfession.NONE) {
            MinecraftServer minecraftServer = world.getMinecraftServer();
            Optional.ofNullable(minecraftServer.getWorldServer(globalPos.getDimensionManager())).flatMap((worldx) -> {
                return worldx.getPoiManager().getType(globalPos.getBlockPosition());
            }).flatMap((poiType) -> {
                return IRegistry.VILLAGER_PROFESSION.stream().filter((profession) -> {
                    return profession.getJobPoiType() == poiType;
                }).findFirst();
            }).ifPresent((profession) -> {
                entity.setVillagerData(entity.getVillagerData().withProfession(profession));
                entity.refreshBrain(world);
            });
        }
    }
}
