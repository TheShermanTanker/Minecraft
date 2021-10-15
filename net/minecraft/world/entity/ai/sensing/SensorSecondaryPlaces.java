package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.level.World;

public class SensorSecondaryPlaces extends Sensor<EntityVillager> {
    private static final int SCAN_RATE = 40;

    public SensorSecondaryPlaces() {
        super(40);
    }

    @Override
    protected void doTick(WorldServer world, EntityVillager entity) {
        ResourceKey<World> resourceKey = world.getDimensionKey();
        BlockPosition blockPos = entity.getChunkCoordinates();
        List<GlobalPos> list = Lists.newArrayList();
        int i = 4;

        for(int j = -4; j <= 4; ++j) {
            for(int k = -2; k <= 2; ++k) {
                for(int l = -4; l <= 4; ++l) {
                    BlockPosition blockPos2 = blockPos.offset(j, k, l);
                    if (entity.getVillagerData().getProfession().getSecondaryPoi().contains(world.getType(blockPos2).getBlock())) {
                        list.add(GlobalPos.create(resourceKey, blockPos2));
                    }
                }
            }
        }

        BehaviorController<?> brain = entity.getBehaviorController();
        if (!list.isEmpty()) {
            brain.setMemory(MemoryModuleType.SECONDARY_JOB_SITE, list);
        } else {
            brain.removeMemory(MemoryModuleType.SECONDARY_JOB_SITE);
        }

    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.SECONDARY_JOB_SITE);
    }
}
