package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.phys.Vec3D;

public class BehaviorNearestVillage extends Behavior<EntityVillager> {
    private final float speedModifier;
    private final int closeEnoughDistance;

    public BehaviorNearestVillage(float speed, int completionRange) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
        this.speedModifier = speed;
        this.closeEnoughDistance = completionRange;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityVillager entity) {
        return !world.isVillage(entity.getChunkCoordinates());
    }

    @Override
    protected void start(WorldServer world, EntityVillager entity, long time) {
        VillagePlace poiManager = world.getPoiManager();
        int i = poiManager.sectionsToVillage(SectionPosition.of(entity.getChunkCoordinates()));
        Vec3D vec3 = null;

        for(int j = 0; j < 5; ++j) {
            Vec3D vec32 = LandRandomPos.getPos(entity, 15, 7, (blockPos) -> {
                return (double)(-poiManager.sectionsToVillage(SectionPosition.of(blockPos)));
            });
            if (vec32 != null) {
                int k = poiManager.sectionsToVillage(SectionPosition.of(new BlockPosition(vec32)));
                if (k < i) {
                    vec3 = vec32;
                    break;
                }

                if (k == i) {
                    vec3 = vec32;
                }
            }
        }

        if (vec3 != null) {
            entity.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(vec3, this.speedModifier, this.closeEnoughDistance));
        }

    }
}
